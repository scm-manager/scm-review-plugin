// @flow
import React from "react";
import classNames from "classnames";
import {type TFunction, translate} from "react-i18next";
import injectSheet from "react-jss";
import {
  Button,
  confirmAlert,
  DateFromNow,
  Loading,
  MarkdownView,
  Modal,
  SubmitButton,
  Textarea
} from "@scm-manager/ui-components";
import type {BasicComment, Comment, Reply} from "../types/PullRequest";
import {deletePullRequestComment, transformPullRequestComment, updatePullRequestComment} from "../pullRequest";
import CreateCommentInlineWrapper from "../diff/CreateCommentInlineWrapper";
import CreateComment from "./CreateComment";
import RecursivePullRequestComment from "./RecursivePullRequestComment";
import {FileTag, OutdatedTag, SystemTag, TaskDoneTag, TaskTodoTag} from "./tags";
import TagGroup from "./TagGroup";
import DiffFile from "@scm-manager/ui-components/src/repos/DiffFile";
import {mapCommentToFile} from "./commentToFileMapper";
import type {AnnotationFactoryContext} from "@scm-manager/ui-components";
import InlineComments from "../diff/InlineComments";
import {createChangeIdFromLocation} from "../diff/locations";

type Props = {
  comment: Comment,
  refresh: () => void,
  handleError: (error: Error) => void,
  child?: boolean,
  createLink: string,

  // context props
  classes: any,
  t: TFunction
};

type State = {
  collapsed: boolean,
  edit: boolean,
  updatedComment: BasicComment,
  loading: boolean,
  contextModalOpen: boolean,
  replyEditor?: Reply
};

const styles = {
  linkColor: {
    color: "inherit"
  },
  authorName: {
    marginLeft: "5px"
  },
  commentMeta: {
    padding: "0 0.4rem"
  }
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
      edit: false
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
    const { comment, handleError, refresh } = this.props;
    comment.comment = this.state.updatedComment.comment;
    comment.type = this.state.updatedComment.type;
    this.setState({
      loading: true
    });
    updatePullRequestComment(comment._links.update.href, comment).then(
      response => {
        if (response.error) {
          this.setState({
            loading: false,
            edit: false
          });
          handleError(response.error);
        } else {
          refresh();
        }
      }
    );
  };

  delete = () => {
    const { comment } = this.props;
    this.deletePullRequestComment(comment._links.delete.href);
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
    const { comment, handleError } = this.props;
    let transformation = comment._embedded.possibleTransitions.find(
      t => t.name === transition
    );
    transformPullRequestComment(transformation).then(response => {
      if (response.error) {
        this.setState({
          loading: false
        });
        handleError(response.error);
      } else {
        this.props.refresh();
      }
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

  deletePullRequestComment = (url: string) => {
    const { refresh, handleError } = this.props;
    this.setState({
      loading: true
    });
    deletePullRequestComment(url).then(response => {
      if (response.error) {
        this.setState({
          loading: false
        });
        handleError(response.error);
      } else {
        this.setState({
          loading: false
        });
        refresh();
      }
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
        <a
          className="level-item"
          onClick={this.confirmDelete}
          title={t("scm-review-plugin.comment.delete")}
        >
          <span className="icon is-small">
            <i className="fas fa-trash" />
          </span>
        </a>
      ) : (
        ""
      );

    const editIcon =
      comment._links.update && !collapsed ? (
        <a
          className="level-item"
          onClick={this.startUpdate}
          title={t("scm-review-plugin.comment.update")}
        >
          <span className="icon is-small">
            <i className="fas fa-edit" />
          </span>
        </a>
      ) : (
        ""
      );

    const replyIcon =
      !!comment._links.reply && !collapsed ? (
        <a
          className="level-item"
          onClick={() => this.reply(comment)}
          title={t("scm-review-plugin.comment.reply")}
        >
          <span className="icon is-small">
            <i className="fas fa-reply" />
          </span>
        </a>
      ) : (
        ""
      );

    let doneTransformation = this.containsPossibleTransition("SET_DONE");
    const doneIcon =
      !!doneTransformation && !collapsed ? (
        createLink ? (
          <a
            className="level-item"
            onClick={this.confirmTransition(
              "SET_DONE",
              "scm-review-plugin.comment.confirmDoneAlert"
            )}
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

    let makeTaskTransformation = this.containsPossibleTransition("MAKE_TASK");
    const makeTaskIcon =
      !!makeTaskTransformation && !collapsed ? (
        createLink ? (
          <a
            className="level-item"
            onClick={this.confirmTransition(
              "MAKE_TASK",
              "scm-review-plugin.comment.confirmMakeTaskAlert"
            )}
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

    let reopenTransformation = this.containsPossibleTransition("REOPEN");
    const reopenIcon =
      !!reopenTransformation && !collapsed ? (
        createLink ? (
          <a
            className="level-item"
            onClick={this.confirmTransition(
              "REOPEN",
              "scm-review-plugin.comment.reopenAlert"
            )}
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

    let normalCommentTransformation = this.containsPossibleTransition(
      "MAKE_COMMENT"
    );
    const normalCommentIcon =
      !!normalCommentTransformation && !collapsed ? (
        createLink ? (
          <a
            className="level-item"
            onClick={this.confirmTransition(
              "MAKE_COMMENT",
              "scm-review-plugin.comment.makeCommentAlert"
            )}
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
      comment._embedded.possibleTransitions &&
      comment._embedded.possibleTransitions.find(t => t.name === name)
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
          <Button
            label={t("scm-review-plugin.comment.cancel")}
            color="warning"
            action={this.confirmCancelUpdate}
          />
        </div>
      </div>
    );
  };

  createDisplayMessage = () => {
    const { comment, t } = this.props;

    let message = comment.systemComment
      ? t("scm-review-plugin.comment.systemMessage." + comment.comment)
      : comment.comment;
    return <MarkdownView content={message} />;
  };

  createMessageEditor = () => {
    const { updatedComment } = this.state;
    return (
      <>
        <Textarea
          name="comment"
          value={updatedComment.comment}
          onChange={this.handleUpdateChange}
        />
      </>
    );
  };

  onClose = () => {
    this.setState({contextModalOpen: false});
  };

  collectTags = (comment: Comment) => {
    const tags = [];

    const openContextModal = this.props.comment.context ? () => {
      this.setState({contextModalOpen: true});
    } : null;

    if (comment.outdated) {
      tags.push(<OutdatedTag onClick={openContextModal} />);
    }

    if (comment.location && comment.location.file) {
      tags.push(<FileTag path={comment.location.file} onClick={openContextModal} />);
    }

    if (comment.systemComment) {
      tags.push(<SystemTag/>);
    }

    if (comment.type === "TASK_TODO") {
      tags.push(<TaskTodoTag />);
    } else if (comment.type === "TASK_DONE") {
      tags.push(<TaskDoneTag title={this.getSetDoneByLabel()}/>);
    }

    if (tags.length > 0) {
      return <TagGroup>{tags}</TagGroup>;
    }

    return null;
  };

  render() {
    const { comment, refresh, handleError, classes, t } = this.props;
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

    const collapseTitle = collapsed
      ? t("scm-review-plugin.comment.expand")
      : t("scm-review-plugin.comment.collapse");
    const collapseIcon = collapsed ? "fa-angle-right" : "fa-angle-down";

    const lastEdited = this.getLastEdited();

    return (
      <>
        {contextModalOpen &&
        <Modal
          title={t("scm-review-plugin.comment.contextModal.title")}
          closeFunction={() => this.onClose()}
          body={<>
            <strong>{t("scm-review-plugin.comment.contextModal.message")}</strong>
            <br/>
            <br/>
            <DiffFile
              file={mapCommentToFile(comment)}
              collapsible={false}
              annotationFactory={this.inlineComment(comment)}
            />
          </>
          }
          active={true}/>
        }
        <CreateCommentInlineWrapper isChildComment={this.props.child}>
          <article className="media">
            <div className="media-content is-clipped content">
              <p>
                <a
                  className={classes.linkColor}
                  onClick={this.toggleCollapse}
                  title={collapseTitle}
                >
                  <span className="icon is-small">
                    <i className={classNames("fa", collapseIcon)} />
                  </span>
                  <span className={classes.authorName}>
                    <strong>{comment.author.displayName}</strong>{" "}
                  </span>
                </a>
                <span className={classes.commentMeta}>
                  <DateFromNow date={comment.date} /> {lastEdited}
                </span>
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
    const {classes} = this.props;
    return (context: AnnotationFactoryContext) => {
      const annotations = {};
      annotations[createChangeIdFromLocation(comment.location)] = (
        <InlineComments>
          <CreateCommentInlineWrapper isChildComment={false}>
            <article className="media">
              <div className="media-content is-clipped content"><p>
                <strong>{comment.author.displayName}</strong>{" "}
                <span className={classes.commentMeta}>
                  <DateFromNow date={comment.date}/> {this.getLastEdited()}
                </span>
                <br/>
                {comment.comment}</p></div>
            </article>
          </CreateCommentInlineWrapper>
        </InlineComments>
      );
      return annotations;
    }
  };

  getLastEdited = () => {
    const { comment, t } = this.props;
    if (comment._embedded && comment._embedded.transitions) {
      const latestTransition = this.getLatestTransition("CHANGE_TEXT");
      if (latestTransition && latestTransition.user.id !== comment.author.id) {
        const latestEditor = latestTransition.user.displayName;
        return (
          <>
            ({t("scm-review-plugin.comment.lastEdited")}{" "}
            <strong>{latestEditor}</strong>)
          </>
        );
      }
    }
    return null;
  };

  getLatestTransition = (type: string) => {
    const { comment } = this.props;
    if (comment._embedded && comment._embedded.transitions) {
      const latestTransitions = comment._embedded.transitions.filter(
        t => t.transition === type
      );
      if (latestTransitions.length > 0) {
        return latestTransitions[latestTransitions.length - 1];
      }
    }
    return null;
  };

  getSetDoneByLabel = () => {
    const { t } = this.props;
    const transition = this.getLatestTransition("SET_DONE");
    return !transition
      ? undefined
      : t("scm-review-plugin.comment.markedDoneBy") +
          " " +
          transition.user.displayName;
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
          handleError={this.props.handleError} // TODO check??
        />
      </CreateCommentInlineWrapper>
    );
  }

  openReplyEditor(comment: Comment) {
    this.setState({ replyEditor: comment });
  }

  closeReplyEditor() {
    this.setState({ replyEditor: null }, this.props.refresh);
  }
}

export default translate("plugins")(injectSheet(styles)(PullRequestComment));
