import React from "react";
import classNames from "classnames";
import { WithTranslation, withTranslation } from "react-i18next";
import styled from "styled-components";
import {
  Button,
  confirmAlert,
  DateFromNow,
  ErrorNotification,
  Loading,
  SubmitButton,
  Textarea,
  apiClient
} from "@scm-manager/ui-components";
import { Link } from "@scm-manager/ui-types";
import { BasicComment, Comment, PossibleTransition, Reply } from "../types/PullRequest";
import { deletePullRequestComment, transformPullRequestComment, updatePullRequestComment } from "../pullRequest";
import CommentSpacingWrapper from "./CommentSpacingWrapper";
import CreateComment from "./CreateComment";
import RecursivePullRequestComment from "./RecursivePullRequestComment";
import CommentActionToolbar from "./CommentActionToolbar";
import CommentTags from "./CommentTags";
import CommentContent from "./CommentContent";
import CommentMetadata from "./CommentMetadata";
import ContextModal from "./ContextModal";
import LastEdited from "./LastEdited";
import EditButtons from "./EditButtons";

const LinkWithInheritColor = styled.a`
  color: inherit;
`;

const AuthorName = styled.span`
  margin-left: 5px;
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
            onDelete(comment);
          } else if (refresh) {
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

  confirmTransition = (transition: string, translationKey: string) => {
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

  handleUpdateChange = (comment: string) => {
    this.setState({
      updatedComment: {
        ...this.state.updatedComment,
        comment: comment
      }
    });
  };

  createEditIcons = () => {
    const { comment, createLink } = this.props;
    const { collapsed } = this.state;
    return (
      <CommentActionToolbar
        comment={comment}
        collapsed={collapsed}
        createLink={createLink}
        onUpdate={this.startUpdate}
        onDelete={this.confirmDelete}
        onReply={() => this.reply(comment)}
        onTransitionChange={this.confirmTransition}
      />
    );
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
    let onOpenContext = () => {};
    if (comment.context) {
      onOpenContext = () => {
        this.setState({
          contextModalOpen: true
        });
      };
    }

    return <CommentTags comment={comment} onOpenContext={onOpenContext} />;
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
      editButtons = <EditButtons comment={comment} onSubmit={this.update} onCancel={this.cancelUpdate} />;
    } else {
      message = !collapsed ? <CommentContent comment={comment} /> : "";
      icons = this.createEditIcons();
    }

    const collapseTitle = collapsed ? t("scm-review-plugin.comment.expand") : t("scm-review-plugin.comment.collapse");
    const collapseIcon = collapsed ? "fa-angle-right" : "fa-angle-down";

    return (
      <>
        {contextModalOpen && <ContextModal comment={comment} onClose={this.onClose} />}
        <CommentSpacingWrapper isChildComment={this.props.child}>
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
                  <DateFromNow date={comment.date} /> <LastEdited comment={comment} />
                </CommentMetadata>
                {this.collectTags(comment)}
                <br />
                {message}
              </p>
              {editButtons}
            </div>
            {icons}
          </article>
        </CommentSpacingWrapper>
        {!collapsed &&
          !!comment._embedded &&
          !!comment._embedded.replies &&
          comment._embedded.replies.map((childComment: Comment) => (
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
    if (!replyComment._links.reply) {
      throw new Error("reply links is missing");
    }

    const replyLink = replyComment._links.reply as Link;
    return (
      <CommentSpacingWrapper>
        <CreateComment
          url={replyLink.href}
          refresh={() => this.closeReplyEditor()}
          onCancel={() => this.closeReplyEditor()}
          autofocus={true}
          reply={true}
        />
      </CommentSpacingWrapper>
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
        replyEditor: undefined
      },
      this.props.refresh
    );
  }
}

export default withTranslation("plugins")(PullRequestComment);
