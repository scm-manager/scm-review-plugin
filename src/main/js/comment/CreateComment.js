// @flow
import React from "react";
import {
  Button,
  Loading,
  SubmitButton,
  Textarea
} from "@scm-manager/ui-components";
import type { BasicComment, Location } from "../types/PullRequest";
import { translate, type TFunction } from "react-i18next";
import { createPullRequestComment } from "../pullRequest";

type Props = {
  url: string,
  location?: Location,
  onCancel?: () => void,
  refresh: () => void,
  autofocus?: boolean,
  handleError: (error: Error) => void,

  // context props
  t: TFunction
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
    this.setState({
      newComment: {
        ...this.state.newComment,
        [name]: value
      }
    });
  };

  submit = () => {
    const { newComment } = this.state;
    if (!newComment) {
      return;
    }

    const { url, location, refresh, handleError } = this.props;
    this.setState({ loading: true });

    createPullRequestComment(url, { ...newComment, location }).then(result => {
      if (result.error) {
        this.setState({ loading: false });
        handleError(result.error);
      } else {
        newComment.comment = "";
        this.setState({ loading: false });
        refresh();
      }
    });
  };

  isValid() {
    const { newComment } = this.state;
    return newComment && newComment.comment && newComment.comment.trim() !== "";
  }

  render() {
    const { autofocus, onCancel, t, url } = this.props;
    const { loading } = this.state;

    if (loading) {
      return <Loading />;
    }

    let cancelButton = null;
    if (onCancel) {
      cancelButton = (
        <Button
          label={t("scm-review-plugin.comment.cancel")}
          color="warning"
          action={onCancel}
        />
      );
    }

    return (
      <>
        {url ? (
          <article className="media">
            <div className="media-content">
              <div className="field">
                <p className="control">
                  <Textarea
                    name="comment"
                    autofocus={autofocus}
                    placeholder={t("scm-review-plugin.comment.add")}
                    onChange={this.handleChanges}
                  />
                </p>
              </div>
              <div className="field">
                <div className="level-left">
                  <div className="level-item">
                    <SubmitButton
                      label={t("scm-review-plugin.comment.add")}
                      action={this.submit}
                      disabled={!this.isValid()}
                      loading={loading}
                      scrollToTop={false}
                    />
                  </div>
                  <div className="level-item">{cancelButton}</div>
                </div>
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

export default translate("plugins")(CreateComment);
