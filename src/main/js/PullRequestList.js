// @flow
import React from "react";
import {
  Title,
  Loading,
  ErrorPage,
  Subtitle,
  DateFromNow
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { translate } from "react-i18next";
import { withRouter } from "react-router-dom";

type Props = {
};

type State = {
};

class PullRequestList extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
    };
  }

  componentDidMount(): void {
  }

  render() {

    return (
      "Hallo"
    );
  }
}

export default withRouter(translate("plugins")(PullRequestList));
