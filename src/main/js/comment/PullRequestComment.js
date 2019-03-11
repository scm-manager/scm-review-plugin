// @flow
import React from "react";
import {
  Button,
  confirmAlert,
  DateFromNow,
  Loading,
  SubmitButton,
  Textarea
} from "@scm-manager/ui-components";
import type { Comment } from "../types/PullRequest";
import { translate, type TFunction } from "react-i18next";
import {
  deletePullRequestComment,
  updatePullRequestComment
} from "../pullRequest";

type Props = {
  comment: Comment,
  refresh: () => void,
  onReply?: Comment => void,
  handleError: (error: Error) => void,

  // context props
  t: TFunction
};

type State = {
  edit: boolean,
  updatedComment: string,
  loading: boolean
};

class PullRequestComment extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      updatedComment: props.comment.comment,
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
      title: t("scm-review-plugin.comment.confirm-alert.title"),
      message: t("scm-review-plugin.comment.confirm-alert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.comment.confirm-alert.submit"),
          onClick: () => this.delete()
        },
        {
          label: t("scm-review-plugin.comment.confirm-alert.cancel"),
          onClick: () => null
        }
      ]
    });
  };

  confirmCancelUpdate = () => {
    const { t } = this.props;
    confirmAlert({
      title: t("scm-review-plugin.comment.confirm-cancel-update-alert.title"),
      message: t(
        "scm-review-plugin.comment.confirm-cancel-update-alert.message"
      ),
      buttons: [
        {
          label: t(
            "scm-review-plugin.comment.confirm-cancel-update-alert.submit"
          ),
          onClick: () => this.cancelUpdate()
        },
        {
          label: t(
            "scm-review-plugin.comment.confirm-cancel-update-alert.cancel"
          ),
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
    const { comment, onReply } = this.props;
    const deleteIcon = comment._links.delete ? (
      <a className="level-item" onClick={this.confirmDelete}>
        <span className="icon is-small">
          <i className="fas fa-trash" />
        </span>
      </a>
    ) : (
      ""
    );

    const editIcon = comment._links.update ? (
      <a className="level-item" onClick={this.startUpdate}>
        <span className="icon is-small">
          <i className="fas fa-edit" />
        </span>
      </a>
    ) : (
      ""
    );

    const replyIcon = onReply ? (
      <a className="level-item" onClick={() => onReply(comment)}>
        <span className="icon is-small">
          <i className="fas fa-reply" />
        </span>
      </a>
    ) : (
      ""
    );

    return (
      <div className="media-right">
        <div className="level-right">
          {deleteIcon}
          {editIcon}
          {replyIcon}
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
    const { comment } = this.props;
    return comment.comment.split("\n").map(line => {
      return (
        <span>
          {line}
          <br />
        </span>
      );
    });
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
    const { comment, t } = this.props;
    const { loading, edit } = this.state;

    if (loading) {
      return <Loading />;
    }

    let icons = null;
    let editButtons = null;
    let message = null;
    let inlineTag = comment.location?
      <span className="tag is-rounded is-info ">
        <span className="fas fa-code " >&nbsp;</span>
        {t("scm-review-plugin.comment.inlineTag")}
      </span>
      : "" ;
    if (edit) {
      message = this.createMessageEditor();
      editButtons = this.createEditButtons();
    } else {
      message = this.createDisplayMessage();
      icons = this.createEditIcons();
    }

    return (
      <>
        <article className="media">
          <div className="media-content">
            <div className="content">
              <p>
                <strong>{comment.author} </strong>
                <DateFromNow date={comment.date} />
                &nbsp; {inlineTag}
                <br />
                {message}
              </p>
              {editButtons}
            </div>
          </div>
          {icons}
        </article>
      </>
    );
  }
}

export default translate("plugins")(PullRequestComment);
