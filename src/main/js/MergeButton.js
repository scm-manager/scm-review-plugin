// @flow
import React from "react";
import { translate } from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";
import ManualMergeInformation from "./ManualMergeInformation";
import type { PullRequest } from "./types/PullRequest";
import type { Repository } from "@scm-manager/ui-types";
import injectSheet from "react-jss";

type Props = {
  merge: () => void,
  repository: Repository,
  mergePossible: boolean,
  loading: boolean,
  pullRequest: PullRequest,
  t: string => string
};

type State = {
  mergeInformation: boolean
};

const styles = {
  buttonSpace: {
    marginBottom: "1em"
  }
};

class MergeButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      mergeInformation: false
    };
  }

  showInformation = () => {
    this.setState({
      mergeInformation: true
    });
  };

  closeInformation = () => {
    this.setState({
      mergeInformation: false
    });
  };

  confirmMerge = () => {
    const { t, merge } = this.props;
    confirmAlert({
      title: t(
        "scm-review-plugin.show-pull-request.mergeButton.confirm-alert.title"
      ),
      message: t(
        "scm-review-plugin.show-pull-request.mergeButton.confirm-alert.message"
      ),
      buttons: [
        {
          label: t(
            "scm-review-plugin.show-pull-request.mergeButton.confirm-alert.submit"
          ),
          onClick: () => merge()
        },
        {
          label: t(
            "scm-review-plugin.show-pull-request.mergeButton.confirm-alert.cancel"
          ),
          onClick: () => null
        }
      ]
    });
  };

  render() {
    const {
      t,
      loading,
      classes,
      mergePossible,
      repository,
      pullRequest
    } = this.props;
    const { mergeInformation } = this.state;
    const action = mergePossible ? this.confirmMerge : this.showInformation;
    const color = mergePossible ? "primary" : "warning";
    return (
      <>
        <Button
          label={t(
            "scm-review-plugin.show-pull-request.mergeButton.button-title"
          )}
          loading={loading}
          action={action}
          color={color}
          className={classes.buttonSpace}
        />
        <ManualMergeInformation
          showMergeInformation={mergeInformation}
          repository={repository}
          pullRequest={pullRequest}
          onClose={() => this.closeInformation()}
        />
      </>
    );
  }
}

export default injectSheet(styles)(translate("plugins")(MergeButton));
