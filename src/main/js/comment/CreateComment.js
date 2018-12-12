// @flow
import React from "react";
import {Loading, SubmitButton, Textarea} from "@scm-manager/ui-components";
import type {BasicComment, PullRequest} from "../types/PullRequest";
import {translate} from "react-i18next";
import {createPullRequestComment} from "../pullRequest";

type Props = {
  url: string,
  t: string => string,
  refresh: () => void,
  handleError: (error: Error) => void
};

type State = {
  newComment?: BasicComment,
  loading: boolean
};

class CreateComment extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      newComment: null,
      loading: false
    };
  }

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
    const {newComment} = this.state;
    const {url, refresh, handleError} = this.props;
    this.setState({loading: true});

    createPullRequestComment(url, newComment).then(
      result => {
        if (result.error) {
          this.setState({loading: false});
          handleError(result.error);
        } else {
          newComment.comment = "";
          this.setState({loading: false});
          refresh();
        }
      }
    );
  };

  render() {
    const {t, url} = this.props;
    const {loading, newComment} = this.state;

    if (loading) {
      return <Loading/>;
    }
    return (
      <>
        {url? (
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
                    disabled={!newComment || (newComment && newComment.comment &&  newComment.comment.trim() == "")}
                    loading={loading}
                  />
                </p>
              </div>
            </div>
          </article>
        ) : ""}
      </>
    );
  }
}

export default translate("plugins")(CreateComment);
