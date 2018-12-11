// @flow
import React from "react";
import {ErrorPage, Loading, SubmitButton, Textarea} from "@scm-manager/ui-components";
import type {Comment, Comments, PullRequest} from "../types/PullRequest";
import {translate} from "react-i18next";
import {createPullRequestComment, getPullRequestComments} from "../pullRequest";
import injectSheet from "react-jss";
import PullRequestComment from "./PullRequestComment";

const styles = {
  bottomSpace: {
    marginBottom: "1.5em"
  }
};

type Props = {
  pullRequest: PullRequest,
  t: string => string,
  classes: any
};

type State = {
  pullRequestComments: Comments,
  actualComment: Comment,
  error?: Error,
  loading: boolean
};

class PullRequestComments extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequestComments: null,
      actualComment: null,
      loading: true
    };
  }

  componentDidMount(): void {
    const {pullRequest} = this.props;
    if (pullRequest) {
      this.updatePullRequestComments();
    }
  }

  updatePullRequestComments = () => {
    const url = this.props.pullRequest._links.comments.href;
    getPullRequestComments(url).then(response => {
      if (response.error) {
        this.setState({
          error: response.error,
          loading: false
        });
      } else {
        this.setState({
          pullRequestComments: response,
          loading: false
        });
      }
    });
  };

  handleFormChange = (value: string, name: string) => {
    this.setState(
      {
        actualComment: {
          ...this.state.actualComment,
          [name]: value
        }
      },
    );
  };

  submit = () => {
    const {pullRequestComments, actualComment} = this.state;
    this.setState({loading: true});

    createPullRequestComment(pullRequestComments._links.create.href, actualComment).then(
      result => {
        if (result.error) {
          this.setState({loading: false, error: result.error});
        } else {
          this.setState({loading: false});
          actualComment.comment = "";
          this.componentDidMount();
        }
      }
    );
  };

  render() {
    const {t, classes} = this.props;
    const {loading, error, pullRequestComments, actualComment} = this.state;

    if (error) {
      return (
        <ErrorPage
          title={t("scm-review-plugin.pull-requests.error-title")}
          subtitle={t("scm-review-plugin.pull-requests.error-subtitle")}
          error={error}
        />
      );
    }

    if (!pullRequestComments) {
      return <div/>;
    }
    if (loading) {
      return <Loading/>;
    }

    if (pullRequestComments && pullRequestComments._embedded && pullRequestComments._embedded.pullRequestComments) {
      const comments = pullRequestComments._embedded.pullRequestComments;
      const createAllowed = pullRequestComments._links.create;
      return (
        <>
          {comments.map((comment) => {
            return <PullRequestComment comment={comment} refresh={() => this.updatePullRequestComments()}  />
          })}

          {createAllowed ? (
          <article className="media">
            <div className="media-content">
              <div className="field">
                <p className="control">
                    <Textarea
                      name="comment"
                      placeholder={t("scm-review-plugin.comment.add")}
                      onChange={this.handleFormChange}
                    />
                </p>
              </div>
              <div className="field">
                <p className="control">
                  <SubmitButton
                    label={t("scm-review-plugin.comment.add")}
                    action={this.submit}
                    disabled={!actualComment || (actualComment && actualComment.comment === "")}
                    loading={loading}
                  />

                </p>
              </div>
            </div>
          </article>
          ): ""}
        </>
      );
    }
  }

}

export default injectSheet(styles)(translate("plugins")(PullRequestComments));
