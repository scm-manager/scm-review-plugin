//@flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import { Select, ErrorNotification } from "@scm-manager/ui-components";
import type { BasicPullRequest } from "./types/PullRequest";
import { getBranches } from "./pullRequest";
import { translate } from "react-i18next";
import EditForm from "./EditForm";

type Props = {
  repository: Repository,
  onChange: (pr: BasicPullRequest) => void,
  userAutocompleteLink: string,
  source?: string,
  target?: string,

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
    const { repository, source, target } = this.props;

    this.setState({ ...this.state, loading: true });
    getBranches(repository._links.branches.href).then(result => {
      if (result.error) {
        this.setState({
          loading: false,
          error: result.error
        });
      } else {
        const initialSource = source ? source : result[0];
        const initialTarget = target
          ? target
          : result.defaultBranch
          ? result.defaultBranch.name
          : result[0];
        this.setState({
          branches: result.branchNames,
          loading: false,
          pullRequest: {
            source: initialSource,
            target: initialTarget
          }
        });
      }
    });
  }

  handleFormChange = (value, name: string) => {
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

  handleSubmit = (event: Event) => {
    event.preventDefault();
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
              label={t("scm-review-plugin.pull-request.sourceBranch")}
              options={options}
              onChange={this.handleFormChange}
              loading={loading}
              value={pullRequest ? pullRequest.source : undefined}
            />
          </div>
          <div className="column is-clipped">
            <Select
              name="target"
              label={t("scm-review-plugin.pull-request.targetBranch")}
              options={options}
              onChange={this.handleFormChange}
              loading={loading}
              value={pullRequest ? pullRequest.target : undefined}
            />
          </div>
        </div>

        <EditForm
          description={""}
          title={""}
          reviewer={[]}
          userAutocompleteLink={this.props.userAutocompleteLink}
          handleFormChange={this.handleFormChange}
        />
      </form>
    );
  }
}

export default translate("plugins")(CreateForm);
