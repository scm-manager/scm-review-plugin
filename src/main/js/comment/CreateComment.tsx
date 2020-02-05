import React from "react";
import { Mention, SuggestionDataItem } from "react-mentions";
import { Button, Loading, Level, SubmitButton, Radio, ErrorNotification, apiClient } from "@scm-manager/ui-components";
import { BasicComment, Comment, Location } from "../types/PullRequest";
import { WithTranslation, withTranslation } from "react-i18next";
import { createPullRequestComment } from "../pullRequest";
import { createChangeIdFromLocation } from "../diff/locations";
import MentionTextarea from "./MentionTextarea";
import styled from "styled-components";

const StyledSuggestion = styled.div`
  color: ${props => props.focused && `#33b2e8`};
  :hover,
  & option:hover {
    color: #33b2e8;
  }
`;

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
    const { autofocus, onCancel, reply, t, url } = this.props;
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

    const users: SuggestionDataItem[] = [
      {
        id: "walter",
        display: "Walter White"
      },
      {
        id: "jesse",
        display: "Jesse Pinkman"
      },
      {
        id: "gus",
        display: 'Gustavo "Gus" Fring'
      },
      {
        id: "saul",
        display: "Saul Goodman"
      },
      {
        id: "hank",
        display: "Hank Schrader"
      },
      {
        id: "skyler",
        display: "Skyler White"
      },
      {
        id: "mike",
        display: "Mike Ehrmantraut"
      },
      {
        id: "lydia",
        display: "Lydìã Rôdarté-Qüayle"
      }
    ];

    return (
      <>
        {url ? (
          <article className="media">
            <div className="media-content">
              <div className="field">
                <div className="control">
                  <MentionTextarea
                    value={newComment?.comment}
                    onChange={this.handleChanges}
                    onSubmit={this.submit}
                    placeholder={t(
                      newComment?.type === "TASK_TODO"
                        ? "scm-review-plugin.comment.addTask"
                        : "scm-review-plugin.comment.addComment"
                    )}
                    autofocus={autofocus}
                  >
                    <Mention
                      markup="@[__id__]"
                      displayTransform={(id: string) => {
                        return `@${users.filter(entry => entry.id === id)[0]?.display}`;
                      }}
                      trigger="@"
                      data={users}
                      renderSuggestion={(
                        suggestion: SuggestionDataItem,
                        search: string,
                        highlightedDisplay: React.ReactNode,
                        index: number,
                        focused: boolean
                      ) => (
                        <StyledSuggestion className="user" focused={focused}>
                          {highlightedDisplay}
                        </StyledSuggestion>
                      )}
                    />
                  </MentionTextarea>
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
