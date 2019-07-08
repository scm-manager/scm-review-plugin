// @flow
import React from "react";
import classNames from "classnames";
import { translate, type TFunction } from "react-i18next";
import {
  Button,
  confirmAlert,
  DateFromNow,
  Loading,
  MarkdownView,
  SubmitButton,
  Textarea
} from "@scm-manager/ui-components";
import type {Comment, Reply} from "../types/PullRequest";
import {
  deletePullRequestComment,
  updatePullRequestComment
} from "../pullRequest";
import CreateCommentInlineWrapper from "../diff/CreateCommentInlineWrapper";
import CreateComment from "./CreateComment";
import RecursivePullRequestComment from "./RecursivePullRequestComment";

type Props = {
  comment: Comment,
  refresh: () => void,
  handleError: (error: Error) => void,
  child: boolean,

  // context props
  t: TFunction
};

type State = {
  collapsed: boolean,
  edit: boolean,
  updatedComment: string,
  loading: boolean,
  replyEditor: Reply
};

class PullRequestComment extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      updatedComment: props.comment.comment,
      collapsed: props.comment.done,
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
      updatedComment: this.props.comment.comment
    });
  };

  update = () => {
    const { comment, handleError } = this.props;
    comment.comment = this.state.updatedComment;
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
          this.setState({
            loading: false,
            edit: false
          });
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

  done = () => {
    const { comment, handleError } = this.props;
    comment.done = true;
    this.setState({
      loading: true
    });
    updatePullRequestComment(comment._links.update.href, comment).then(
      response => {
        if (response.error) {
          this.setState({
            loading: false
          });
          handleError(response.error);
        } else {
          this.setState({
            loading: false,
            collapsed: true
          });
        }
      }
    );
  };

  confirmDone = () => {
    const { t } = this.props;
    confirmAlert({
      title: t("scm-review-plugin.comment.confirmDoneAlert.title"),
      message: t("scm-review-plugin.comment.confirmDoneAlert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.comment.confirmDoneAlert.submit"),
          onClick: () => this.done()
        },
        {
          label: t("scm-review-plugin.comment.confirmDoneAlert.cancel"),
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

  handleUpdateChange = (updatedComment: string) => {
    this.setState({
      updatedComment: updatedComment
    });
  };

  createEditIcons = () => {
    const { comment, t } = this.props;
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

    const collapseTitle = collapsed
      ? t("scm-review-plugin.comment.expand")
      : t("scm-review-plugin.comment.collapse");
    const collapseIcon = collapsed ? "fa-angle-left" : "fa-angle-down";
    const doneIcon = !comment.done ? (
      comment._links.update ? (
        <a
          className="level-item"
          onClick={this.confirmDone}
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
      <a
        className="level-item"
        onClick={this.toggleCollapse}
        title={collapseTitle}
      >
        <span className="icon is-small">
          <i className={classNames("fa", collapseIcon)} />
        </span>
      </a>
    );

    return (
      <div className="media-right">
        <div className="level-right">
          {deleteIcon}
          {editIcon}
          {replyIcon}
          {doneIcon}
        </div>
      </div>
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
            disabled={updatedComment.trim() === ""}
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
      <Textarea
        name="comment"
        value={updatedComment}
        onChange={this.handleUpdateChange}
      />
    );
  };

  render() {
    const { comment, refresh, handleError, t } = this.props;
    const { loading, collapsed, edit } = this.state;

    if (loading) {
      return <Loading />;
    }

    let icons = null;
    let editButtons = null;
    let message = null;
    let tag = comment.location ? (
      <span className="tag is-rounded is-info" title={comment.location.file}>
        <span className="fas fa-code">&nbsp;</span>
        {comment.location.file.replace(/^.+\//, "")}
      </span>
    ) : (
      ""
    );
    tag = comment.systemComment ? (
      <span className="tag is-rounded is-info ">
        <span className="fas fa-bolt">&nbsp;</span>
        {t("scm-review-plugin.comment.tag.system")}
      </span>
    ) : (
      tag
    );
    let done = comment.done ? (
      <span className="tag is-rounded is-info ">
        <span className="fas fa-check-circle">&nbsp;</span>
        {t("scm-review-plugin.comment.tag.done")}
      </span>
    ) : (
      ""
    );
    if (edit) {
      message = this.createMessageEditor();
      editButtons = this.createEditButtons();
    } else {
      message = !collapsed ? this.createDisplayMessage() : "";
      icons = this.createEditIcons();
    }

    return (
      <>
        <CreateCommentInlineWrapper isChildComment={this.props.child}>
          <article className="media">
            <div className="media-content is-clipped content">
              <p>
                <strong>{comment.author.displayName} </strong>
                <DateFromNow date={comment.date} />
                &nbsp; {tag} {done}
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
          handleError={this.onError}
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

export default translate("plugins")(PullRequestComment);
