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
import { Repository, Branch, Link } from "@scm-manager/ui-types";
import { ErrorNotification, Select } from "@scm-manager/ui-components";
import {BasicPullRequest, CheckResult} from "./types/PullRequest";
import { getBranches } from "./pullRequest";
import { WithTranslation, withTranslation } from "react-i18next";
import EditForm from "./EditForm";
import styled from "styled-components";

const ValidationError = styled.p`
  font-size: 0.75rem;
  color: #ff3860;
  margin-top: -3em;
  margin-bottom: 3em;
`;

type Props = WithTranslation & {
  repository: Repository;
  onChange: (pr: BasicPullRequest) => void;
  userAutocompleteLink: string;
  source?: string;
  target?: string;
  checkResult?: CheckResult
};

type State = {
  pullRequest: BasicPullRequest;
  branches: string[];
  loading: boolean;
  error?: Error;
};

class CreateForm extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequest: {
        source: "",
        target: "",
        title: ""
      },
      branches: [],
      loading: false
    };
  }

  componentDidMount() {
    const { repository, source, target } = this.props;

    this.setState({
      ...this.state,
      loading: true
    });
    getBranches((repository._links.branches as Link).href)
      .then((result: Branch | any) => {
        const initialSource = source ? source : result.branchNames[0];
        const initialTarget = target ? target : result.defaultBranch ? result.defaultBranch.name : result[0];
        this.setState(
          {
            branches: result.branchNames,
            loading: false,
            pullRequest: {
              title: "",
              source: initialSource,
              target: initialTarget
            }
          },
          this.notifyAboutChangedForm
        );
      })
      .catch(error => {
        this.setState({ error, loading: false });
      });
  }

  handleFormChange = (value: any, name?: any) => {
    this.setState(
      {
        pullRequest: {
          ...this.state.pullRequest,
          [name]: value
        }
      },
      this.notifyAboutChangedForm
    );
  };

  notifyAboutChangedForm = () => {
    const { pullRequest } = this.state;
    this.props.onChange(pullRequest);
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
  };

  renderValidationError = () => {
    const { t, checkResult } = this.props;

    if (checkResult && checkResult.status !== "PR_VALID") {
      return <ValidationError>{t(`scm-review-plugin.pullRequest.validation.${checkResult.status}`)}</ValidationError>;
    }
  };

  render() {
    const { t } = this.props;
    const { loading, error, pullRequest } = this.state;
    const options = this.state.branches.map(branch => ({
      label: branch,
      value: branch
    }));

    if (error) {
      return <ErrorNotification error={error} />;
    }

    return (
      <form onSubmit={this.handleSubmit}>
        <div className="columns">
          <div className="column is-clipped">
            <Select
              name="source"
              label={t("scm-review-plugin.pullRequest.sourceBranch")}
              options={options}
              onChange={this.handleFormChange}
              loading={loading}
              value={pullRequest ? pullRequest.source : undefined}
            />
          </div>
          <div className="column is-clipped">
            <Select
              name="target"
              label={t("scm-review-plugin.pullRequest.targetBranch")}
              options={options}
              onChange={this.handleFormChange}
              loading={loading}
              value={pullRequest ? pullRequest.target : undefined}
            />
          </div>
        </div>
        {this.renderValidationError()}
        <EditForm
          description=""
          title={undefined}
          reviewer={[]}
          userAutocompleteLink={this.props.userAutocompleteLink}
          handleFormChange={this.handleFormChange}
        />
      </form>
    );
  }
}

export default withTranslation("plugins")(CreateForm);
