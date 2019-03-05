// @flow
import React from "react";
import {Loading, SubmitButton, Textarea} from "@scm-manager/ui-components";
import type {BasicComment, Location} from "../types/PullRequest";
import {translate} from "react-i18next";
import {createPullRequestComment} from "../pullRequest";

type Props = {
  url: string,
  location?: Location,
  refresh: () => void,
  handleError: (error: Error) => void,

  // context props
  t: string => string,
};

type State = {
  newComment?: BasicComment,
  loading: boolean
};

class CreateComment extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
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
    if (!newComment) {
      return;
    }

    const {url, location, refresh, handleError} = this.props;
    this.setState({loading: true});

    createPullRequestComment(url, {...newComment, location}).then(
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

  isValid() {
    const {newComment} = this.state;
    return !newComment || (newComment && newComment.comment &&  newComment.comment.trim() === "");
  }

  render() {
    const {t, url} = this.props;
    const { loading } = this.state;

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
                    disabled={this.isValid()}
                    loading={loading}
                    scrollToTop={false}
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
