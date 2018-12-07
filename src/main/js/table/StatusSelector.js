// @flow
import React from "react";
import { translate } from "react-i18next";
import { Select } from "@scm-manager/ui-components";

type Props = {
  t: string => string,
  handleTypeChange: string => void,
  status: string,
  label?: string,
  helpText?: string,
  loading?: boolean
};

class StatusSelector extends React.Component<Props> {
  render() {
    const { status, handleTypeChange, loading, label, helpText } = this.props;
    const types = ["OPEN", "ALL", "REJECTED", "MERGED"];

    return (
      <Select
        onChange={handleTypeChange}
        value={status ? status : "ALL"}
        options={this.createSelectOptions(types)}
        loading={loading}
        label={label}
        helpText={helpText}
      />
    );
  }

  createSelectOptions(status: string[]) {
    return status.map(singleStatus => {
      return {
        label: singleStatus,
        value: singleStatus
      };
    });
  }
}

export default translate("plugins")(StatusSelector);
