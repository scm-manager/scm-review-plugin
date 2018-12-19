//@flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import {
  Select,
  ErrorNotification
} from "@scm-manager/ui-components";
import type { BasicPullRequest } from "./types/PullRequest";
import { getBranches } from "./pullRequest";
import { translate } from "react-i18next";
import EditForm from "./EditForm";

type Props = {
  repository: Repository,
  onChange: (pr: BasicPullRequest) => void,

  // Context props
  t: string => string
};

type State = {
  pullRequest?: BasicPullRequest,
  branches: string[],
  loading: boolean,
  error?: boolean
};

class CreateForm extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      branches: [],
      loading: false
    };
  }

  componentDidMount() {
    const { repository } = this.props;

    this.setState({ ...this.state, loading: true });
    getBranches(repository._links.branches.href).then(result => {
      if (result.error) {
        this.setState({
          loading: false,
          error: result.error
        });
      } else {
        this.setState({
          branches: result,
          loading: false,
          pullRequest: {
            source: result[0], //set first entry, otherwise nothing is select in state even if one branch is shown in Select component at beginning
            target: result[0]
          }
        });
      }
    });
  }

  handleFormChange = (value: string, name: string) => {
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
    const pullRequest = { ...this.state.pullRequest };
    this.props.onChange(pullRequest);
  };

  render() {
    const { t } = this.props;
    const { loading, error } = this.state;
    const options = this.state.branches.map(branch => ({
      label: branch,
      value: branch
    }));

    if (error) {
      return <ErrorNotification error={error} />;
    }

    return (
      <form>
        <div className="columns">
          <div className="column is-clipped">
            <Select
              name="source"
              label={t("scm-review-plugin.pull-request.sourceBranch")}
              options={options}
              onChange={this.handleFormChange}
              loading={loading}
            />
          </div>
          <div className="column is-clipped">
            <Select
              name="target"
              label={t("scm-review-plugin.pull-request.targetBranch")}
              options={options}
              onChange={this.handleFormChange}
              loading={loading}
            />
          </div>
        </div>

        <EditForm description={""} title={""} handleFormChange={this.handleFormChange} />

      </form>
    );
  }
}

export default translate("plugins")(CreateForm);
