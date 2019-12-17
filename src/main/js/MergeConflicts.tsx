import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Conflict, Conflicts, PullRequest } from "./types/PullRequest";
import { Loading, Diff, DiffFile } from "@scm-manager/ui-components";
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

  getTypeLabel = (type: string) => {
    return this.props.t("scm-review-plugin.conflicts.types." + type);
  };

  createDiffComponent = (conflict: Conflict) => {
    if (conflict.diff) {
      const parsedDiff = parser.parse(conflict.diff);
      return parsedDiff
        .map(file => ({ ...file, type: this.getTypeLabel(conflict.type) }))
        .map(file => <DiffFile file={file} sideBySide={false} />);
    } else {
      return (
        <DiffFile
          file={{ hunks: [], newPath: conflict.path, type: this.getTypeLabel(conflict.type) }}
          sideBySide={false}
        />
      );
    }
  };

  render() {
    if (this.state.loading) {
      return <Loading />;
    }

    return (
      <div className={"content"}>
        {this.state.conflicts.conflicts.map(conflict => this.createDiffComponent(conflict))}
      </div>
    );
  }
}

export default withTranslation("plugins")(MergeConflicts);
