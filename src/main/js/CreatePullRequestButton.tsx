import React from "react";
import { Repository, Branch, Link } from "@scm-manager/ui-types";
import { AddButton, Loading } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { getPullRequests } from "./pullRequest";
import { PullRequest, PullRequestCollection } from "./types/PullRequest";
import PullRequestTable from "./table/PullRequestTable";
import styled from "styled-components";

const PullRequestInfo = styled.div`
  padding-top: 1em;
`;

type Props = WithTranslation & {
  repository: Repository;
  branch: Branch;
};

type State = {
  pullRequests: PullRequestCollection;
  error?: Error;
  loading: boolean;
};

class CreatePullRequestButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequests: { _embedded: { pullRequests: [] }, _links: {} },
      loading: true
    };
  }

  componentDidMount(): void {
    if (this.props.repository._links.pullRequest) {
      const url = (this.props.repository._links.pullRequest as Link).href;
      this.updatePullRequests(url);
    }
  }

  updatePullRequests = (url: string) => {
    getPullRequests(url)
      .then(response => {
        this.setState({
          pullRequests: response,
          loading: false
        });
      })
      .catch(error => {
        this.setState({
          loading: false,
          error
        });
      });
  };

  render() {
    const { repository, branch, t } = this.props;
    const { loading, error, pullRequests } = this.state;

    if (!this.props.repository._links.pullRequest) {
      return null;
    }

    if (loading) {
      return <Loading />;
    }

    if (error) {
      return;
    }

    const matchingPullRequests = pullRequests._embedded.pullRequests.filter(
      (pr: PullRequest) => pr.source === branch.name
    );

    let existing = null;
    if (matchingPullRequests.length > 0) {
      existing = (
        <div>
          <PullRequestTable repository={repository} pullRequests={matchingPullRequests} />
        </div>
      );
    }
    return (
      <PullRequestInfo>
        <h4>{t("scm-review-plugin.branch.header")}</h4>
        {existing}
        <AddButton
          label={t("scm-review-plugin.branch.createPullRequest")}
          link={`/repo/${repository.namespace}/${repository.name}/pull-requests/add?source=${branch.name}`}
        />
      </PullRequestInfo>
    );
  }
}

export default withTranslation("plugins")(CreatePullRequestButton);
