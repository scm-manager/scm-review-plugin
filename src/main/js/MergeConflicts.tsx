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
import { Conflict, Conflicts, PullRequest } from "./types/PullRequest";
import { Loading, Notification, DiffFile } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import { fetchConflicts } from "./pullRequest";
// @ts-ignore
import parser from "gitdiff-parser";
import ManualMergeInformation from "./ManualMergeInformation";

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
  mergeInformation: boolean;
};

class MergeConflicts extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      mergeInformation: false
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
        .map(file => <DiffFile markConflicts={true} file={file} sideBySide={false} />);
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
    const { loading, conflicts, mergeInformation } = this.state;
    const { repository, pullRequest } = this.props;

    if (loading) {
      return <Loading />;
    }

    return (
      <>
        <Notification type={"warning"}>
          <div className="content">
            <b>{this.props.t("scm-review-plugin.conflicts.hint.header")}</b>
            <p>
              <a onClick={() => this.setState({ mergeInformation: true })}>
                {this.props.t("scm-review-plugin.conflicts.hint.text")}
              </a>
            </p>
          </div>
        </Notification>
        {conflicts.conflicts.map(conflict => this.createDiffComponent(conflict))}
        <ManualMergeInformation
          showMergeInformation={mergeInformation}
          repository={repository}
          pullRequest={pullRequest}
          onClose={() => this.setState({ mergeInformation: false })}
        />
      </>
    );
  }
}

export default withTranslation("plugins")(MergeConflicts);
