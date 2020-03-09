import React from "react";
import { NavLink, MenuContext } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";

type Props = WithTranslation & {
  url: string;
  activeWhenMatch: (route: any) => boolean;
};

class PullRequestsNavLink extends React.Component<Props> {
  render() {
    const { url, activeWhenMatch, t } = this.props;

    return (
      <MenuContext.Consumer>
        {({ menuCollapsed }) => (
          <NavLink
            to={`${url}/pull-requests`}
            icon="fas fa-code-branch fa-rotate-180"
            label={t("scm-review-plugin.navLink")}
            activeWhenMatch={activeWhenMatch}
            title={t("scm-review-plugin.navLink")}
            collapsed={menuCollapsed}
          />
        )}
      </MenuContext.Consumer>
    );
  }
}

export default withTranslation("plugins")(PullRequestsNavLink);
