// @flow
import React from "react";
import {ErrorNotification, ErrorPage, Loading, SubmitButton, Textarea} from "@scm-manager/ui-components";
import type {BasicComment, Comments, PullRequest} from "../types/PullRequest";
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
  pullRequestComments?: Comments,
  newComment?: BasicComment,
  error?: Error,
  loading: boolean
};

class PullRequestComments extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequestComments: null,
      newComment: null,
      loading: true
    };
  }

  componentDidMount(): void {
    const {pullRequest} = this.props;
    if (pullRequest && pullRequest._links && pullRequest._links.comments  ) {
      this.updatePullRequestComments();
    }else{
      this.setState({
        loading: false
      });
    }
  }

  handleError = (error : Error) => {
    this.setState({
      loading: false,
      error : error
    });
  };

  updatePullRequestComments = () => {
    this.setState({
      loading: true
    });
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

  handleChanges = (value: string, name: string) => {
    this.setState(
      {
        newComment: {
          ...this.state.newComment,
          [name]: value
        }
      },
    );
  };

  submit = () => {
    const {pullRequestComments, newComment} = this.state;
    this.setState({loading: true});

    createPullRequestComment(pullRequestComments._links.create.href, newComment).then(
      result => {
        if (result.error) {
          this.setState({loading: false, error: result.error});
        } else {
          newComment.comment = "";
          this.updatePullRequestComments();
        }
      }
    );
  };

  render() {
    const {t} = this.props;
    const {loading, error, pullRequestComments, newComment} = this.state;

    if (error) {
      return <ErrorNotification error={error} />;
    }


    if (loading) {
      return <Loading/>;
    }
    if (!pullRequestComments) {
      return <div/>;
    }

    if (pullRequestComments && pullRequestComments._embedded && pullRequestComments._embedded.pullRequestComments) {
      const comments = pullRequestComments._embedded.pullRequestComments;
      const createAllowed = pullRequestComments._links.create;
      return (
        <>
          {comments.map((comment) => {
            return <PullRequestComment comment={comment} refresh={this.updatePullRequestComments} handleError={this.handleError} />
          })}

          {createAllowed ? (
          <article className="media">
            <div className="media-content">
              <div className="field">
                <p className="control">
                    <Textarea
                      name="comment"
                      placeholder={t("scm-review-plugin.comment.add")}
                      onChange={this.handleChanges}
                    />
                </p>
              </div>
              <div className="field">
                <p className="control">
                  <SubmitButton
                    label={t("scm-review-plugin.comment.add")}
                    action={this.submit}
                    disabled={!newComment || (newComment && newComment.comment === "")}
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
