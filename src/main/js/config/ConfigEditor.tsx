import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Title, Checkbox, Subtitle } from "@scm-manager/ui-components";
import BranchList from "./BranchList";
import { Config } from "../types/Config";
import styled from "styled-components";

type Props = WithTranslation & {
  onConfigurationChange: (config: State, valid: boolean) => void;
  initialConfiguration: Config;
  global: boolean;
};

type State = Config;

const BottomMarginText = styled.p`
  margin-bottom: 1rem;
`;

class ConfigEditor extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = props.initialConfiguration;
  }

  onChangeRestrictBranchWriteAccess = (isRestrictBranchWriteAccess: boolean) => {
    this.setState(
      {
        restrictBranchWriteAccess: isRestrictBranchWriteAccess
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
    const { restrictBranchWriteAccess, protectedBranchPatterns, disableRepositoryConfiguration } = this.state;
    return (
      <>
        {global && (
          <>
            <Title title={t("scm-review-plugin.config.title")} />
            <Checkbox
              checked={!!disableRepositoryConfiguration}
              onChange={this.onChangeDisableRepositoryConfiguration}
              label={t("scm-review-plugin.config.disableRepositoryConfiguration.label")}
              helpText={t("scm-review-plugin.config.disableRepositoryConfiguration.helpText")}
            />
          </>
        )}
        <Checkbox
          checked={restrictBranchWriteAccess}
          onChange={this.onChangeRestrictBranchWriteAccess}
          label={t("scm-review-plugin.config.restrictBranchWriteAccess.label")}
          helpText={t("scm-review-plugin.config.restrictBranchWriteAccess.helpText")}
        />
        {restrictBranchWriteAccess && (
          <>
            <hr />
            <Subtitle subtitle={t("scm-review-plugin.config.subtitle")} />
            <BottomMarginText>{t("scm-review-plugin.config.note")}</BottomMarginText>
            <BranchList branches={protectedBranchPatterns} onChange={this.onChangeBranches} />
          </>
        )}
      </>
    );
  }
}

export default withTranslation("plugins")(ConfigEditor);
