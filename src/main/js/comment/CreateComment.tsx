import React from "react";
import {
  Button,
  Loading,
  Level,
  SubmitButton,
  Radio,
  Textarea,
  ErrorNotification,
  apiClient
} from "@scm-manager/ui-components";
import { BasicComment, Comment, Location } from "../types/PullRequest";
import { WithTranslation, withTranslation } from "react-i18next";
import { createPullRequestComment } from "../pullRequest";

type Props = WithTranslation & {
  url: string;
  location?: Location;
  onCancel?: () => void;
  onCreation: (comment: Comment) => void;
  autofocus?: boolean;
  reply?: boolean;
};

type State = {
  newComment?: BasicComment;
  loading: boolean;
  errorResult?: Error;
};

class CreateComment extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      newComment: {
        type: "COMMENT"
      }
    };
  }

  handleChanges = (value: string, name: string) => {
    this.setState({
      newComment: {
        ...this.state.newComment,
        [name]: value
      }
    });
  };

  switchToCommentType = (value: boolean) => {
    if (value) {
      this.switchCommentType("COMMENT");
    }
  };

  switchToTaskType = (value: boolean) => {
    if (value) {
      this.switchCommentType("TASK_TODO");
    }
  };

  switchCommentType = (type: string) => {
    this.setState({
      newComment: {
        ...this.state.newComment,
        type
      }
    });
  };

  submit = () => {
    const { newComment } = this.state;
    if (!newComment || !this.isValid()) {
      return;
    }

    const { url, location } = this.props;
    this.setState({
      loading: true
    });

    createPullRequestComment(url, {
      ...newComment,
      location
    })
      .then(this.fetchCreatedComment)
      .catch((errorResult: Error) => {
        this.setState({
          errorResult,
          loading: false
        });
      });
  };

  fetchCreatedComment = (response: Response) => {
    const commentHref = response.headers.get("Location");
    if (commentHref) {
      return apiClient
        .get(commentHref)
        .then(response => response.json())
        .then(comment => {
          if (this.props.onCreation) {
            this.props.onCreation(comment);
          }
          this.finishedLoading();
        });
    } else {
      throw new Error("missing location header");
    }
  };

  finishedLoading = () => {
    this.setState(state => {
      let newComment;
      if (state.newComment) {
        newComment = {
          ...state.newComment,
          comment: ""
        };
      }
      return {
        loading: false,
        newComment
      };
    });
  };

  isValid() {
    const { newComment } = this.state;
    return newComment && newComment.comment && newComment.comment.trim() !== "";
  }

  render() {
    const { autofocus, onCancel, reply, t, url } = this.props;
    const { loading, errorResult } = this.state;

    if (loading) {
      return <Loading />;
    }

    let cancelButton = null;
    if (onCancel) {
      cancelButton = <Button label={t("scm-review-plugin.comment.cancel")} color="warning" action={onCancel} />;
    }

    let toggleType = null;
    if (!reply) {
      toggleType = (
        <div className="field is-grouped">
          <div className="control">
            <Radio
              name="comment_type"
              value="COMMENT"
              checked={this.state.newComment.type === "COMMENT"}
              label={t("scm-review-plugin.comment.type.comment")}
              onChange={this.switchToCommentType}
            />
            <Radio
              name="comment_type"
              value="TASK_TODO"
              checked={this.state.newComment.type === "TASK_TODO"}
              label={t("scm-review-plugin.comment.type.task")}
              onChange={this.switchToTaskType}
            />
          </div>
        </div>
      );
    }

    return (
      <>
        {url ? (
          <article className="media">
            <div className="media-content">
              <div className="field">
                <div className="control">
                  <Textarea
                    name="comment"
                    value={this.state.newComment.comment}
                    autofocus={autofocus}
                    placeholder={t(
                      this.state.newComment.type === "TASK_TODO"
                        ? "scm-review-plugin.comment.addTask"
                        : "scm-review-plugin.comment.addComment"
                    )}
                    onChange={this.handleChanges}
                    onSubmit={this.submit}
                  />
                </div>
              </div>
              {errorResult && <ErrorNotification error={errorResult} />}
              {toggleType}
              <div className="field">
                <Level
                  right={
                    <>
                      <div className="level-item">
                        <SubmitButton
                          label={t(
                            this.state.newComment.type === "TASK_TODO"
                              ? "scm-review-plugin.comment.addTask"
                              : "scm-review-plugin.comment.addComment"
                          )}
                          action={this.submit}
                          disabled={!this.isValid()}
                          loading={loading}
                          scrollToTop={false}
                        />
                      </div>
                      <div className="level-item">{cancelButton}</div>
                    </>
                  }
                />
              </div>
            </div>
          </article>
        ) : (
          ""
        )}
      </>
    );
  }
}

export default withTranslation("plugins")(CreateComment);
