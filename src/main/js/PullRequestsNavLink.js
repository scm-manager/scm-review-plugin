// @flow
import React from "react";
import { NavLink } from "@scm-manager/ui-components";
import { translate } from "react-i18next";

type Props = {
  url: string,
  activeWhenMatch: (route: any) => boolean,
  t: string => string
};

class PullRequestsNavLink extends React.Component<Props> {
  render() {
    const { url, activeWhenMatch, t } = this.props;

    return (
      <NavLink
        to={`${url}/pull-requests`}
        label={t("scm-review-plugin.navLink")}
        activeWhenMatch={activeWhenMatch}
      />
    );
  }
}

export default translate("plugins")(PullRequestsNavLink);
