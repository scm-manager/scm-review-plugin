import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Conflict, Conflicts, PullRequest } from "./types/PullRequest";
import { Loading, Diff } from "@scm-manager/ui-components";
import { Repository, Link } from "@scm-manager/ui-types";
import { fetchConflicts } from "./pullRequest";
// @ts-ignore
import parser from "gitdiff-parser";

type Props = WithTranslation & {
  repository: Repository;
  pullRequest: PullRequest;
  source: string;
  target: string;
};

type State = {
  loading: boolean;
  error?: Error;
  conflicts?: Conflicts;
};

class MergeConflicts extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true
    };
  }

  componentDidMount() {
    this.fetchConflicts();
  }

  fetchConflicts = () => {
    const { pullRequest } = this.props;
    if (pullRequest && pullRequest._links && pullRequest._links.mergeConflicts) {
      fetchConflicts(pullRequest._links.mergeConflicts.href, pullRequest.source, pullRequest.target)
        .then(conflicts => {
          this.setState({
            loading: false,
            error: undefined,
            conflicts
          });
        })
        .catch(error =>
          this.setState({
            loading: false,
            error
          })
        );
    } else {
      this.setState({
        loading: false
      });
    }
  };

  createDiffComponent = (conflict: Conflict) => {
    const parsedDiff = parser.parse(conflict.diff);
    return <Diff diff={parsedDiff} {...this.props} />;
  };

  render() {
    if (this.state.loading) {
      return <Loading />;
    }

    this.state.conflicts.conflicts.forEach;

    return (
      <div className={"content"}>
        <dl>
          {this.state.conflicts.conflicts.map(conflict => (
            <>
              <dt>{conflict.path}</dt>
              <dd>
                {conflict.type}
                {conflict.diff && this.createDiffComponent(conflict)}
              </dd>
            </>
          ))}
        </dl>
      </div>
    );
  }
}

export default withTranslation("plugins")(MergeConflicts);
