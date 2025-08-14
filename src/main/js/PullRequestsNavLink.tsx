/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import React from "react";
import { SecondaryNavigationItem } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";

type Props = WithTranslation & {
  url: string;
  activeWhenMatch: (route: any) => boolean;
};

class PullRequestsNavLink extends React.Component<Props> {
  render() {
    const { url, activeWhenMatch, t } = this.props;

    return (
      <SecondaryNavigationItem
        to={`${url}/pull-requests/`}
        icon="fas fa-code-branch fa-rotate-180"
        label={t("scm-review-plugin.navLink.pullRequest")}
        activeWhenMatch={activeWhenMatch}
        title={t("scm-review-plugin.navLink.pullRequest")}
      />
    );
  }
}

export default withTranslation("plugins")(PullRequestsNavLink);
