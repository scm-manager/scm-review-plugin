//@flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import {apiClient, InputField, Textarea, Select} from "@scm-manager/ui-components";
import type {PullRequest} from "./PullRequest";

type Props = {
  repository: Repository,
  onChange: (pr: PullRequest) => void
};

type State = {
  pullRequest?: PullRequest,
  branches: string[],
  loading: boolean
};

class CreateForm extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      branches: [],
      loading: true
    }
  }

  componentDidMount() {
    const { repository } = this.props;
    // TODO error handling
    apiClient.get(repository._links.branches.href)
      .then(response => response.json())
      .then(collection => collection._embedded.branches)
      .then(branches => branches.map(b => b.name))
      .then(names => this.setState(
        {
          branches: names,
          loading: false,
          pullRequest: {
            source: names[0], //set first entry, otherwise nothing is select in state even if one branch is shown in Select component at beginning
            target: names[0]
          }
        }
        ));
  }

  handleFormChange = (value: string, name: string) => {
    this.setState({
      pullRequest: {
        ...this.state.pullRequest,
        [name]: value
      }
    }, this.notifyAboutChangedForm);
  };

  notifyAboutChangedForm = () => {
    const pullRequest = { ...this.state.pullRequest};
    this.props.onChange(pullRequest);
  };

  render() {
    const {loading} = this.state;
    const options = this.state.branches.map((branch) => ({
      label: branch,
      value: branch
    }));

    return (
      <form>

        <div className="columns">
          <div className="column">
            <Select name="source" label="Source Branch" options={options} onChange={this.handleFormChange} loading={loading}/>
          </div>
          <div className="column">
            <Select name="target" label="Target Branch" options={options} onChange={this.handleFormChange} loading={loading}/>
          </div>
        </div>

        <InputField name="title" label="Title" onChange={this.handleFormChange} />
        <Textarea name="description" label="Description" onChange={this.handleFormChange} />

      </form>
    );
  }

}

export default CreateForm;
