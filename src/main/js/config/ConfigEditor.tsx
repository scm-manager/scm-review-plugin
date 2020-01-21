import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Checkbox } from "@scm-manager/ui-components";
import BranchList from "./BranchList";
import { Config } from "../types/Config";

type Props = WithTranslation & {
  onConfigurationChange: (config: State, valid: boolean) => void;
  initialConfiguration: Config;
  global: boolean;
};

type State = Config;

class ConfigEditor extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = props.initialConfiguration;
  }

  onChangeEnabled = (isEnabled: boolean) => {
    this.setState(
      {
        enabled: isEnabled
      },
      () => this.props.onConfigurationChange(this.state, true)
    );
  };

  onChangeDisableRepositoryConfiguration = (isChangeDisableRepositoryConfiguration: boolean) => {
    this.setState(
      {
        disableRepositoryConfiguration: isChangeDisableRepositoryConfiguration
      },
      () => this.props.onConfigurationChange(this.state, true)
    );
  };

  onChangeBranches = (newBranches: string[]) => {
    this.setState(
      {
        protectedBranchPatterns: newBranches
      },
      () => this.props.onConfigurationChange(this.state, true)
    );
  };

  render() {
    const { global, t } = this.props;
    const { enabled, protectedBranchPatterns, disableRepositoryConfiguration } = this.state;
    return (
      <>
        {global && (
          <Checkbox
            checked={!!disableRepositoryConfiguration}
            onChange={this.onChangeDisableRepositoryConfiguration}
            label={t("scm-review-plugin.config.disableRepositoryConfiguration.label")}
            helpText={t("scm-review-plugin.config.disableRepositoryConfiguration.helpText")}
          />
        )}
        <Checkbox
          checked={enabled}
          onChange={this.onChangeEnabled}
          label={t("scm-review-plugin.config.enabled.label")}
          helpText={t("scm-review-plugin.config.enabled.helpText")}
        />
        {enabled && <BranchList branches={protectedBranchPatterns} onChange={this.onChangeBranches} />}
      </>
    );
  }
}

export default withTranslation("plugins")(ConfigEditor);
