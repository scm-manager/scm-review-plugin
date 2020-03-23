/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import React from "react";
import { apiClient, Button, ErrorNotification, Level, Loading, Radio, SubmitButton } from "@scm-manager/ui-components";
import { BasicComment, Comment, Location } from "../types/PullRequest";
import { WithTranslation, withTranslation } from "react-i18next";
import { createPullRequestComment } from "../pullRequest";
import { createChangeIdFromLocation } from "../diff/locations";
import MentionTextarea from "./MentionTextarea";

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
        type: "COMMENT",
        mentions: []
      }
    };
  }

  handleChanges = (event: any) => {
    this.setState({
      newComment: {
        ...this.state.newComment,
        comment: event.target.value
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
        .then(r => r.json())
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

  createCommentEditorName = () => {
    const { location } = this.props;
    let name = "editor";
    if (location) {
      name += location.file;
      if (location.oldLineNumber || location.newLineNumber) {
        name += "_" + createChangeIdFromLocation(location);
      }
    }
    return name;
  };

  isValid() {
    const { newComment } = this.state;
    return newComment && newComment.comment && newComment.comment.trim() !== "";
  }

  render() {
    const { onCancel, reply, t, url } = this.props;
    const { loading, errorResult, newComment } = this.state;

    if (loading) {
      return <Loading />;
    }

    let cancelButton = null;
    if (onCancel) {
      cancelButton = <Button label={t("scm-review-plugin.comment.cancel")} color="warning" action={onCancel} />;
    }

    let toggleType = null;
    if (!reply) {
      const editorName = this.createCommentEditorName();
      toggleType = (
        <div className="field is-grouped">
          <div className="control">
            <Radio
              name={`comment_type_${editorName}`}
              value="COMMENT"
              checked={newComment?.type === "COMMENT"}
              label={t("scm-review-plugin.comment.type.comment")}
              onChange={this.switchToCommentType}
            />
            <Radio
              name={`comment_type_${editorName}`}
              value="TASK_TODO"
              checked={newComment?.type === "TASK_TODO"}
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
                  <MentionTextarea
                    value={newComment?.comment}
                    comment={newComment}
                    placeholder={t(
                      newComment?.type === "TASK_TODO"
                        ? "scm-review-plugin.comment.addTask"
                        : "scm-review-plugin.comment.addComment"
                    )}
                    onAddMention={(id, displayName) => {
                      this.setState(prevState => ({
                        ...prevState,
                        newComment: {
                          ...prevState.newComment,
                          mentions: [...prevState.newComment.mentions, { id, displayName }]
                        }
                      }));
                    }}
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
                            newComment?.type === "TASK_TODO"
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
