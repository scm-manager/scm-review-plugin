import React from "react";
import classNames from "classnames";
import { WithTranslation, withTranslation } from "react-i18next";
import styled from "styled-components";
import {
  Button,
  confirmAlert,
  DateFromNow,
  DiffFile,
  ErrorNotification,
  Loading,
  MarkdownView,
  Modal,
  SubmitButton,
  Textarea,
  apiClient
} from "@scm-manager/ui-components";
import { Link } from "@scm-manager/ui-types";
import { BasicComment, Comment, PossibleTransition, Reply } from "../types/PullRequest";
import { deletePullRequestComment, transformPullRequestComment, updatePullRequestComment } from "../pullRequest";
import CreateCommentInlineWrapper from "../diff/CreateCommentInlineWrapper";
import CreateComment from "./CreateComment";
import RecursivePullRequestComment from "./RecursivePullRequestComment";
import { FileTag, OutdatedTag, SystemTag, TaskDoneTag, TaskTodoTag } from "./tags";
import TagGroup from "./TagGroup";
import { mapCommentToFile } from "./commentToFileMapper";
import { AnnotationFactoryContext } from "@scm-manager/ui-components";
import InlineComments from "../diff/InlineComments";
import { createChangeIdFromLocation } from "../diff/locations";

const StyledModal = styled(Modal)`
  & table.diff .diff-gutter:empty:hover::after {
    display: none;
  }

  & .modal-card {
    @media screen and (min-width: 1280px), print {
      width: 1200px;
    }
  }
`;

const LinkWithInheritColor = styled.a`
  color: inherit;
`;

const AuthorName = styled.span`
  margin-left: 5px;
`;

const CommentMetadata = styled.span`
  padding: 0 0.4rem;
`;

const LastEdited = styled.small`
  color: #9a9a9a; // dark-50
`;

const LatestEditor = styled.span`
  color: #9a9a9a; // dark-50
`;

type Props = WithTranslation & {
  comment: Comment;
  onDelete?: (comment: Comment) => void;
  onUpdate?: (comment: Comment) => void;
  refresh?: () => void;
  handleError: (error: Error) => void;
  child?: boolean;
  createLink: string;
};

type State = {
  collapsed: boolean;
  edit: boolean;
  updatedComment: BasicComment;
  loading: boolean;
  contextModalOpen: boolean;
  replyEditor?: Reply;
  errorResult?: Error;
};

class PullRequestComment extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      updatedComment: {
        ...props.comment
      },
      collapsed: props.comment.type === "TASK_DONE",
      edit: false,
      contextModalOpen: false
    };
  }

  startUpdate = () => {
    this.setState({
      loading: false,
      edit: true
    });
  };

  cancelUpdate = () => {
    this.setState({
      loading: false,
      edit: false,
      updatedComment: {
        ...this.props.comment
      }
    });
  };

  update = () => {
    const { refresh, onUpdate } = this.props;
    const { updatedComment } = this.state;
    const comment = {
      ...this.props.comment,
      comment: updatedComment.comment,
      type: updatedComment.type
    };

    this.setState({
      loading: true
    });

    if (!comment._links.update) {
      throw new Error("comment has no update link");
    }

    const link = comment._links.update as Link;

    updatePullRequestComment(link.href, comment)
      .then(() => {
        if (onUpdate) {
          return this.fetchRefreshed(comment, onUpdate);
        } else if (refresh) {
          this.setState({
            loading: false
          });
        }
      })
      .catch(error => {
        this.setState({
          loading: false,
          errorResult: error
        });
      });
  };

  fetchRefreshed = (comment: Comment, callback: (c: Comment) => void) => {
    const link = comment._links.self as Link;
    return apiClient
      .get(link.href)
      .then(response => response.json())
      .then((c: Comment) => {
        callback(c);
        this.setState({
          loading: false,
          edit: false,
          updatedComment: {
            ...c
          }
        });
      })
      .catch(error => {
        this.setState({
          loading: false,
          errorResult: error
        });
      });
  };

  delete = () => {
    const { comment } = this.props;
    if (comment._links.delete) {
      const href = (comment._links.delete as Link).href;

      const { refresh, onDelete, handleError } = this.props;
      this.setState({
        loading: true
      });

      deletePullRequestComment(href)
        .then(response => {
          if (onDelete) {
            console.log("delete with OnDelete");
            onDelete(comment);
          } else if (refresh) {
            console.log("delete with refresh");
            refresh();
          }
          this.setState({
            loading: false
          });
        })
        .catch(error => {
          this.setState({
            loading: false
          });
          handleError(error);
        });
    } else {
      throw new Error("could not find required delete link");
    }
  };

  confirmDelete = () => {
    const { t } = this.props;
    confirmAlert({
      title: t("scm-review-plugin.comment.confirmDeleteAlert.title"),
      message: t("scm-review-plugin.comment.confirmDeleteAlert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.comment.confirmDeleteAlert.submit"),
          onClick: () => this.delete()
        },
        {
          label: t("scm-review-plugin.comment.confirmDeleteAlert.cancel"),
          onClick: () => null
        }
      ]
    });
  };

  executeTransition = (transition: string) => {
    const { comment, handleError, refresh, onUpdate } = this.props;

    if (!(comment && comment._embedded && comment._embedded.possibleTransitions)) {
      throw new Error("comment has no possible transitions");
    }

    const transformation = comment._embedded.possibleTransitions.find((t: PossibleTransition) => t.name === transition);
    if (!transformation) {
      throw new Error("comment does not have transition " + transition);
    }

    transformPullRequestComment(transformation)
      .then(response => {
        if (onUpdate) {
          return this.fetchRefreshed(comment, onUpdate);
        } else if (refresh) {
          refresh();
        }
      })
      .catch(error => {
        this.setState({
          loading: false
        });
        handleError(error);
      });
  };

  confirmTransition = (transition: string, translationKey: string) => () => {
    const { t } = this.props;
    confirmAlert({
      title: t(translationKey + ".title"),
      message: t(translationKey + ".message"),
      buttons: [
        {
          label: t(translationKey + ".submit"),
          onClick: () => this.executeTransition(transition)
        },
        {
          label: t(translationKey + ".cancel"),
          onClick: () => null
        }
      ]
    });
  };

  toggleCollapse = () => {
    this.setState(prevState => ({
      collapsed: !prevState.collapsed
    }));
  };

  confirmCancelUpdate = () => {
    const { t } = this.props;
    confirmAlert({
      title: t("scm-review-plugin.comment.confirmCancelUpdateAlert.title"),
      message: t("scm-review-plugin.comment.confirmCancelUpdateAlert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.comment.confirmCancelUpdateAlert.submit"),
          onClick: () => this.cancelUpdate()
        },
        {
          label: t("scm-review-plugin.comment.confirmCancelUpdateAlert.cancel"),
          onClick: () => null
        }
      ]
    });
  };

  handleUpdateChange = (comment: string) => {
    this.setState({
      updatedComment: {
        ...this.state.updatedComment,
        comment: comment
      }
    });
  };

  createEditIcons = () => {
    const { comment, createLink, t } = this.props;
    const { collapsed } = this.state;

    const deleteIcon =
      comment._links.delete && !collapsed ? (
        <a className="level-item" onClick={this.confirmDelete} title={t("scm-review-plugin.comment.delete")}>
          <span className="icon is-small">
            <i className="fas fa-trash" />
          </span>
        </a>
      ) : (
        ""
      );

    const editIcon =
      comment._links.update && !collapsed ? (
        <a className="level-item" onClick={this.startUpdate} title={t("scm-review-plugin.comment.update")}>
          <span className="icon is-small">
            <i className="fas fa-edit" />
          </span>
        </a>
      ) : (
        ""
      );

    const replyIcon =
      !!comment._links.reply && !collapsed ? (
        <a className="level-item" onClick={() => this.reply(comment)} title={t("scm-review-plugin.comment.reply")}>
          <span className="icon is-small">
            <i className="fas fa-reply" />
          </span>
        </a>
      ) : (
        ""
      );

    const doneTransformation = this.containsPossibleTransition("SET_DONE");
    const doneIcon =
      !!doneTransformation && !collapsed ? (
        createLink ? (
          <a
            className="level-item"
            onClick={this.confirmTransition("SET_DONE", "scm-review-plugin.comment.confirmDoneAlert")}
            title={t("scm-review-plugin.comment.done")}
          >
            <span className="icon is-small">
              <i className="fas fa-check-circle" />
            </span>
          </a>
        ) : (
          ""
        )
      ) : (
        ""
      );

    const makeTaskTransformation = this.containsPossibleTransition("MAKE_TASK");
    const makeTaskIcon =
      !!makeTaskTransformation && !collapsed ? (
        createLink ? (
          <a
            className="level-item"
            onClick={this.confirmTransition("MAKE_TASK", "scm-review-plugin.comment.confirmMakeTaskAlert")}
            title={t("scm-review-plugin.comment.makeTask")}
          >
            <span className="icon is-small">
              <i className="fas fa-tasks" />
            </span>
          </a>
        ) : (
          ""
        )
      ) : (
        ""
      );

    const reopenTransformation = this.containsPossibleTransition("REOPEN");
    const reopenIcon =
      !!reopenTransformation && !collapsed ? (
        createLink ? (
          <a
            className="level-item"
            onClick={this.confirmTransition("REOPEN", "scm-review-plugin.comment.reopenAlert")}
            title={t("scm-review-plugin.comment.reopen")}
          >
            <span className="icon is-small">
              <i className="fas fa-undo" />
            </span>
          </a>
        ) : (
          ""
        )
      ) : (
        ""
      );

    const normalCommentTransformation = this.containsPossibleTransition("MAKE_COMMENT");
    const normalCommentIcon =
      !!normalCommentTransformation && !collapsed ? (
        createLink ? (
          <a
            className="level-item"
            onClick={this.confirmTransition("MAKE_COMMENT", "scm-review-plugin.comment.makeCommentAlert")}
            title={t("scm-review-plugin.comment.makeComment")}
          >
            <span className="icon is-small">
              <i className="fas fa-comment-dots" />
            </span>
          </a>
        ) : (
          ""
        )
      ) : (
        ""
      );

    return (
      <div className="media-right">
        <div className="level-right">
          {deleteIcon}
          {editIcon}
          {makeTaskIcon}
          {normalCommentIcon}
          {replyIcon}
          {reopenIcon}
          {doneIcon}
        </div>
      </div>
    );
  };

  containsPossibleTransition = (name: string) => {
    const { comment } = this.props;
    return (
      comment._embedded &&
      comment._embedded.possibleTransitions &&
      comment._embedded.possibleTransitions.find((t: PossibleTransition) => t.name === name)
    );
  };

  createEditButtons = () => {
    const { t } = this.props;
    const { updatedComment } = this.state;

    return (
      <div className="level-left">
        <div className="level-item">
          <SubmitButton
            label={t("scm-review-plugin.comment.save")}
            action={this.update}
            disabled={updatedComment.comment.trim() === ""}
            scrollToTop={false}
          />
        </div>
        <div className="level-item">
          <Button label={t("scm-review-plugin.comment.cancel")} color="warning" action={this.confirmCancelUpdate} />
        </div>
      </div>
    );
  };

  createDisplayMessage = () => {
    const { comment, t } = this.props;

    const message = comment.systemComment
      ? t("scm-review-plugin.comment.systemMessage." + comment.comment)
      : comment.comment;
    return <MarkdownView content={message} />;
  };

  createMessageEditor = () => {
    const { updatedComment, errorResult } = this.state;
    return (
      <>
        <Textarea name="comment" value={updatedComment.comment} onChange={this.handleUpdateChange} />
        {errorResult && <ErrorNotification error={errorResult} />}
      </>
    );
  };

  onClose = () => {
    this.setState({
      contextModalOpen: false
    });
  };

  collectTags = (comment: Comment) => {
    const tags = [];

    const openContextModal = this.props.comment.context
      ? () => {
          this.setState({
            contextModalOpen: true
          });
        }
      : null;

    if (comment.outdated) {
      tags.push(<OutdatedTag onClick={openContextModal} />);
    }

    if (comment.location && comment.location.file) {
      tags.push(<FileTag path={comment.location.file} onClick={openContextModal} />);
    }

    if (comment.systemComment) {
      tags.push(<SystemTag />);
    }

    if (comment.type === "TASK_TODO") {
      tags.push(<TaskTodoTag />);
    } else if (comment.type === "TASK_DONE") {
      tags.push(<TaskDoneTag title={this.getSetDoneByLabel()} />);
    }

    if (tags.length > 0) {
      return <TagGroup>{tags}</TagGroup>;
    }

    return null;
  };

  render() {
    const { comment, refresh, handleError, t } = this.props;
    const { loading, collapsed, edit, contextModalOpen } = this.state;

    if (loading) {
      return <Loading />;
    }

    let icons = null;
    let editButtons = null;
    let message = null;

    if (edit) {
      message = this.createMessageEditor();
      editButtons = this.createEditButtons();
    } else {
      message = !collapsed ? this.createDisplayMessage() : "";
      icons = this.createEditIcons();
    }

    const collapseTitle = collapsed ? t("scm-review-plugin.comment.expand") : t("scm-review-plugin.comment.collapse");
    const collapseIcon = collapsed ? "fa-angle-right" : "fa-angle-down";

    const lastEdited = this.getLastEdited();

    return (
      <>
        {contextModalOpen && (
          <StyledModal
            title={t("scm-review-plugin.comment.contextModal.title")}
            closeFunction={() => this.onClose()}
            body={
              <>
                <strong>{t("scm-review-plugin.comment.contextModal.message")}</strong>
                <br />
                <br />
                <DiffFile
                  file={mapCommentToFile(comment)}
                  collapsible={false}
                  annotationFactory={this.inlineComment(comment)}
                />
              </>
            }
            active={true}
          />
        )}
        <CreateCommentInlineWrapper isChildComment={this.props.child}>
          <article className="media">
            <div className="media-content is-clipped content">
              <p>
                <LinkWithInheritColor onClick={this.toggleCollapse} title={collapseTitle}>
                  <span className="icon is-small">
                    <i className={classNames("fa", collapseIcon)} />
                  </span>
                  <AuthorName>
                    <strong>{comment.author.displayName}</strong>{" "}
                  </AuthorName>
                </LinkWithInheritColor>
                <CommentMetadata>
                  <DateFromNow date={comment.date} /> {lastEdited}
                </CommentMetadata>
                {this.collectTags(comment)}
                <br />
                {message}
              </p>
              {editButtons}
            </div>
            {icons}
          </article>
        </CreateCommentInlineWrapper>
        {!collapsed &&
          !!comment._embedded &&
          !!comment._embedded.replies &&
          comment._embedded.replies.map(childComment => (
            <RecursivePullRequestComment
              child={true}
              comment={childComment}
              refresh={refresh}
              handleError={handleError}
            />
          ))}
        {this.createReplyEditorIfNeeded(comment.id)}
      </>
    );
  }

  inlineComment = (comment: Comment) => {
    return (context: AnnotationFactoryContext) => {
      const annotations = {};
      annotations[createChangeIdFromLocation(comment.location)] = (
        <InlineComments>
          <CreateCommentInlineWrapper isChildComment={false}>
            <article className="media">
              <div className="media-content is-clipped content">
                <p>
                  <strong>{comment.author.displayName}</strong>{" "}
                  <CommentMetadata>
                    <DateFromNow date={comment.date} /> {this.getLastEdited()}
                  </CommentMetadata>
                  <br />
                  <MarkdownView content={comment.comment} />
                </p>
              </div>
            </article>
          </CreateCommentInlineWrapper>
        </InlineComments>
      );
      return annotations;
    };
  };

  getLastEdited = () => {
    const { comment, t } = this.props;
    if (comment._embedded && comment._embedded.transitions) {
      const latestTransition = this.getLatestTransition("CHANGE_TEXT");
      if (latestTransition && latestTransition.user.id !== comment.author.id) {
        const latestEditor = latestTransition.user.displayName;
        return (
          <LastEdited>
            {t("scm-review-plugin.comment.lastEdited")} <LatestEditor>{latestEditor}</LatestEditor>
          </LastEdited>
        );
      }
    }
    return null;
  };

  getLatestTransition = (type: string) => {
    const { comment } = this.props;
    if (comment._embedded && comment._embedded.transitions) {
      const latestTransitions = comment._embedded.transitions.filter(t => t.transition === type);
      if (latestTransitions.length > 0) {
        return latestTransitions[latestTransitions.length - 1];
      }
    }
    return null;
  };

  getSetDoneByLabel = () => {
    const { t } = this.props;
    const transition = this.getLatestTransition("SET_DONE");
    return !transition ? undefined : t("scm-review-plugin.comment.markedDoneBy") + " " + transition.user.displayName;
  };

  reply = (comment: Comment) => {
    this.openReplyEditor(comment);
  };

  createReplyEditorIfNeeded = (id: string) => {
    const replyComment = this.state.replyEditor;
    if (replyComment && replyComment.id === id) {
      return this.createNewReplyEditor(replyComment);
    }
  };

  createNewReplyEditor(replyComment: Comment) {
    return (
      <CreateCommentInlineWrapper>
        <CreateComment
          url={replyComment._links.reply.href}
          refresh={() => this.closeReplyEditor()}
          onCancel={() => this.closeReplyEditor()}
          autofocus={true}
          reply={true}
        />
      </CreateCommentInlineWrapper>
    );
  }

  openReplyEditor(comment: Comment) {
    this.setState({
      replyEditor: comment
    });
  }

  closeReplyEditor() {
    this.setState(
      {
        replyEditor: null
      },
      this.props.refresh
    );
  }
}

export default withTranslation("plugins")(PullRequestComment);
