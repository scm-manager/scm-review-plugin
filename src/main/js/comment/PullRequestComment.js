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
  t: string => string
};

type State = {
  error?: Error,
  edit: boolean,
  updatedComment : string,
  loading: boolean
};

class PullRequestComment extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      updatedComment: props.comment.comment,
      edit: false
    };
  }

  updateMode = () => {
    this.setState({
      loading: false,
      edit: true
    });
  };

  update = () => {
    const {comment} = this.props;
    comment.comment = this.state.updatedComment;
    updatePullRequestComment(comment._links.update.href, comment).then(response => {
      if (response.error) {
        this.setState({
          error: response.error,
          loading: false,
          edit: false
        });
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
    console.log("deleteComment: ");
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

  deletePullRequestComment = (url: string) => {
    const {refresh} = this.props;
    deletePullRequestComment(url).then(response => {
      if (response.error) {
        this.setState({
          error: response.error,
          loading: false
        });
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

    if (loading && !comment) {
      return <Loading/>;
    }
    const deleteIcon = comment._links.delete ? (
      <a className="level-item" onClick={this.confirmDelete}>
        <span className="icon is-small"><i className="fas fa-trash"></i></span>
      </a>
    ) : "";
    const editIcon = comment._links.update ? (
      <a className="level-item" onClick={this.updateMode}>
        <span className="icon is-small"><i className="fas fa-edit"></i></span>
      </a>
    ) : "";

    const icons = (
      <div className="level-left">
        {deleteIcon}
        {editIcon}
      </div>
    );

    const save = (
      <div className="level-left">
        <SubmitButton
          label={t("scm-review-plugin.comment.save")}
          action={this.update}
          disabled={!comment || (comment && comment.comment === "")}
        />
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
    const controlPanel = edit ? save : icons;

    return (
      <>
        <article className="media">
          <div className="media-content">
            <div className="content">
              <p>
                <strong>{comment.author}</strong>
                <span>   </span>
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
