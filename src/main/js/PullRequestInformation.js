// @flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import { translate } from "react-i18next";
import Changesets from "./Changesets";

type Props = {
  repository: Repository,
  source: string,
  target: string,
  t: string => string
};

type State = {
  activeTab: string
};

class PullRequestInformation extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      activeTab: "commits"
    };
  }

  changeTab = (activeTab: string, newTab: string) => {
    if ( activeTab !== newTab ) {
      this.setState({
        activeTab: newTab
      });
    }
  };

  render() {
    const { activeTab } = this.state;
    return (
      <>
        <div className="tabs">
          <ul>
            <li className={ activeTab === "commits" ? "is-active": "" }>
              <a onClick={() => this.changeTab(activeTab, "commits")}>Commits</a>
            </li>
            <li className={ activeTab === "diff" ? "is-active": "" }>
              <a onClick={() => this.changeTab(activeTab, "diff")}>Diff</a>
            </li>
          </ul>
        </div>

        { this.renderActiveTab(activeTab) }
      </>
    );
  }

  renderActiveTab(activeTab) {
    const { repository, source, target } = this.props;
    if (activeTab === "commits") {
      return <Changesets repository={repository} source={source} target={target} />
    } else {
      return null;
    }
  }
}

export default (translate("plugins")(PullRequestInformation));
