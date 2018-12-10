// @flow
import React from "react";
import { translate } from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";

type Props = {
  merge: () => void,
  mergePossible: boolean,
  loading: boolean,
  t: string => string
};

type State = {
  confirmDialog?: boolean
};

class MergeButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      confirmDialog: true
    };
  }

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
    const { t, merge, loading, mergePossible } = this.props;
    const { confirmDialog } = this.state;
    const action = confirmDialog ? this.confirmMerge : merge;
    const color = mergePossible ? "primary": "warning";
    return (
      <Button
        label={t(
          "scm-review-plugin.show-pull-request.mergeButton.button-title"
        )}
        loading={loading}
        action={action}
        color={color}
      />
    );
  }
}

export default translate("plugins")(MergeButton);
