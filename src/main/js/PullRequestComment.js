// @flow
import React from "react";
import {DateFromNow, ErrorPage, Loading, SubmitButton, Textarea} from "@scm-manager/ui-components";
import type {Repository} from "@scm-manager/ui-types";
import type {Comments, PullRequest, Comment} from "./types/PullRequest";
import {translate} from "react-i18next";
import {createPullRequestComment, getPullRequestComments, deletePullRequestComment} from "./pullRequest";
import classNames from "classnames";
import injectSheet from "react-jss";

const styles = {
  bottomSpace: {
    marginBottom: "1.5em"
  }
};

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  t: string => string,
  classes: any
};

type State = {
  pullRequestComments : Comments,
  actualComment: Comment,
  error?: Error,
  loading: boolean
};

class PullRequestComment extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequestComments: null,
      loading: true
    };
  }

  componentDidMount(): void {
    const { pullRequest} = this.props;
    if (pullRequest){
      const url = this.props.pullRequest._links.comments.href;
      this.updatePullRequestComments(url);
    }
  }

  updatePullRequestComments = (url: string) => {
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
    const {pullRequestComments, actualComment } = this.state;
    this.setState({ loading: true });

    createPullRequestComment(pullRequestComments._links.create.href, actualComment).then(
      result => {
        if (result.error) {
          this.setState({ loading: false, error: result.error });
        } else {
          this.setState({ loading: false });
          this.componentDidMount();
        }
      }
    );
  };

 delete (url: string) {

   console.log("deleteComment: "+url )
    // this.setState({ loading: true });
    // deletePullRequestComment(url).then(
    //   result => {
    //     if (result.error) {
    //       this.setState({ loading: false, error: result.error });
    //     } else {
    //       this.setState({ loading: false });
    //       this.componentDidMount();
    //     }
    //   }
    // );
  };

  render() {
    const {t, classes } = this.props;
    const { loading, error, pullRequestComments} = this.state;

    if (error) {
      return (
        <ErrorPage
          title={t("scm-review-plugin.pull-requests.error-title")}
          subtitle={t("scm-review-plugin.pull-requests.error-subtitle")}
          error={error}
        />
      );
    }

    if (!pullRequestComments ) {
      return <div />;
    }
    if (loading) {
      return <Loading />;
    }

    if (pullRequestComments && pullRequestComments._embedded && pullRequestComments._embedded.pullRequestComments) {
      const comments = pullRequestComments._embedded.pullRequestComments;
      const disabled = !pullRequestComments._links.create;
      return (
        <>
          {comments.map((comment) => {
            return <>
                    <article className="media">
                      <div className="media-content">
                        <div className={classNames("media", classes.bottomSpace)}>
                          <div className="media-content">
                            <h1> {comment.author}</h1>
                          </div>
                          <div className="media-right"><DateFromNow date={comment.date} /></div>
                          <div>


                            {comment._links.delete? (
                            <SubmitButton
                              label={t("scm-review-plugin.comment.delete")}
                              action={this.delete(comment._links.delete.href)}
                              loading={loading}
                              disabled={disabled}
                            />) : ""  }
                          </div>
                        </div>
                          <div className="media">
                            <div className="media-content">
                              {comment.comment.split("\n").map((line) => {
                                return <span>{line}<br /></span>;
                              })}
                            </div>
                          </div>
                      </div>
                    </article>
                  </>
            ;
          })}
          <article className="media">
            <div className="media-content">

               <Textarea
                 name="comment"
                 label={t("scm-review-plugin.comment.add")}
                 onChange={this.handleFormChange}
               />
            </div>
          </article>

          <div >
            <SubmitButton
              label={t("scm-review-plugin.comment.add")}
              action={this.submit}
              loading={loading}
              disabled={disabled}
            />
          </div>

        </>
      );

    }
  }
}

export default injectSheet(styles)(translate("plugins")(PullRequestComment));
