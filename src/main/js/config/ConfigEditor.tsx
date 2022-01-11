/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Title, Checkbox, Subtitle } from "@scm-manager/ui-components";
import BranchList from "./BranchList";
import { Config, ProtectionBypass } from "../types/Config";
import BypassList from "./BypassList";

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

  onChangePreventMergeFromAuthor = (isChangePreventMergeFromAuthor: boolean) => {
    this.setState(
      {
        preventMergeFromAuthor: isChangePreventMergeFromAuthor
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

  onChangeBypasses = (newBypasses: ProtectionBypass[]) => {
    this.setState(
      {
        branchProtectionBypasses: newBypasses
      },
      () => this.props.onConfigurationChange(this.state, true)
    );
  };

  render() {
    const { global, t } = this.props;
    const {
      restrictBranchWriteAccess,
      protectedBranchPatterns,
      branchProtectionBypasses,
      preventMergeFromAuthor,
      disableRepositoryConfiguration
    } = this.state;
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
        {!global && <Subtitle>{t("scm-review-plugin.config.title")}</Subtitle>}
        <Checkbox
          checked={restrictBranchWriteAccess}
          onChange={this.onChangeRestrictBranchWriteAccess}
          label={t("scm-review-plugin.config.restrictBranchWriteAccess.label")}
          helpText={t("scm-review-plugin.config.restrictBranchWriteAccess.helpText")}
        />
        <Checkbox
          checked={preventMergeFromAuthor}
          onChange={this.onChangePreventMergeFromAuthor}
          label={t("scm-review-plugin.config.preventMergeFromAuthor.label")}
          helpText={t("scm-review-plugin.config.preventMergeFromAuthor.helpText")}
        />
        {restrictBranchWriteAccess && (
          <>
            <hr />
            <Subtitle subtitle={t("scm-review-plugin.config.branchProtection.branches.subtitle")} />
            <p className="mb-4">{t("scm-review-plugin.config.branchProtection.branches.note")}</p>
            <BranchList branches={protectedBranchPatterns} onChange={this.onChangeBranches} />
            <Subtitle subtitle={t("scm-review-plugin.config.branchProtection.bypasses.subtitle")} />
            <p className="mb-4">{t("scm-review-plugin.config.branchProtection.bypasses.note")}</p>
            <BypassList bypasses={branchProtectionBypasses} onChange={this.onChangeBypasses} />
          </>
        )}
      </>
    );
  }
}

export default withTranslation("plugins")(ConfigEditor);
