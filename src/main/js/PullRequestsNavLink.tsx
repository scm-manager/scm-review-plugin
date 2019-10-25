import React from "react";
import { NavLink } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";

type Props = WithTranslation & {
  url: string;
  activeWhenMatch: (route: any) => boolean;
};

class PullRequestsNavLink extends React.Component<Props> {
  render() {
    const { url, activeWhenMatch, t } = this.props;

    return (
      <NavLink
        to={`${url}/pull-requests`}
        icon="fas fa-code-branch fa-rotate-180"
        label={t("scm-review-plugin.navLink")}
        activeWhenMatch={activeWhenMatch}
      />
    );
  }
}

export default withTranslation("plugins")(PullRequestsNavLink);
