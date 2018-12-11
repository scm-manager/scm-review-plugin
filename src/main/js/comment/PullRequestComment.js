// @flow
import React from "react";
import {confirmAlert, DateFromNow, Loading, SubmitButton, Textarea} from "@scm-manager/ui-components";
import type {Comment} from "../types/PullRequest";
import {translate} from "react-i18next";
import injectSheet from "react-jss";
import {deletePullRequestComment, updatePullRequestComment} from "../pullRequest";

const styles = {
  bottomSpace: {
    marginBottom: "1.5em"
  }
};

type Props = {
  comment: Comment,
  refresh: () => void,
  handleError : (error: Error) => void;
  t: string => string
};

type State = {
  edit: boolean,
  updatedComment : string,
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
      updatedComment : this.props.comment.comment
    });
  };

  update = () => {
    const {comment, handleError} = this.props;
    comment.comment = this.state.updatedComment;
    this.setState({
      loading: true
    });
    updatePullRequestComment(comment._links.update.href, comment).then(response => {
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
    });
  };

  delete = () => {
    const {comment} = this.props;
    this.deletePullRequestComment(comment._links.delete.href);
  };

  confirmDelete = () => {
    const {t} = this.props;
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
    const {t} = this.props;
    confirmAlert({
      title: t("scm-review-plugin.comment.confirm-cancel-update-alert.title"),
      message: t("scm-review-plugin.comment.confirm-cancel-update-alert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.comment.confirm-cancel-update-alert.submit"),
          onClick: () => this.cancelUpdate()
        },
        {
          label: t("scm-review-plugin.comment.confirm-cancel-update-alert.cancel"),
          onClick: () => null
        }
      ]
    });
  };

  deletePullRequestComment = (url: string) => {
    const {refresh, handleError} = this.props;
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

  render() {
    const {t, comment} = this.props;
    const {loading, edit, updatedComment} = this.state;

    if (loading ) {
      return <Loading/>;
    }
    const deleteIcon = comment._links.delete ? (
      <a className="level-item"
         onClick={this.confirmDelete}
      >
        <span className="icon is-small">
          <i className="fas fa-trash">
          </i>
        </span>
      </a>
    ) : "";

    const editIcon = comment._links.update ? (
      <a className="level-item" onClick={this.startUpdate}>
        <span className="icon is-small">
          <i className="fas fa-edit">
          </i>
        </span>
      </a>
    ) : "";

    const icons = (
      <div className="level-left">
        {deleteIcon}
        {editIcon}
      </div>
    );

    const saveCancel = (
      <div className="level-left">
        <div className="level-item">
          <SubmitButton
            label={t("scm-review-plugin.comment.save")}
            action={this.update}
            disabled={!comment || (comment && comment.comment === "")}
          />
        </div>
        <div className="level-item">
          <SubmitButton
            label={t("scm-review-plugin.comment.cancel")}
            action={this.confirmCancelUpdate}
          />
        </div>
      </div>
    );

    const displayMessage = comment.comment.split("\n").map((line) => {
      return <span>{line}<br/></span>;
    });

    const editMessage = (
      <Textarea
        name="comment"
        value={updatedComment}
        onChange={this.handleUpdateChange}
      />
    );

    const message = edit ? editMessage : displayMessage;
    const controlPanel = edit ? saveCancel : icons;

    return (
      <>
        <article className="media">
          <div className="media-content">
            <div className="content">
              <p>
                <strong>{comment.author} </strong>
                <DateFromNow date={comment.date}/>
                <br/>
                {message}
              </p>
            </div>
            <nav className="level is-mobile">
              {controlPanel}
            </nav>
          </div>
        </article>
      </>
    );

  }
}

export default injectSheet(styles)(translate("plugins")(PullRequestComment));
