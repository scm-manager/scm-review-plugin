//@flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import {apiClient, InputField, Textarea, Select} from "@scm-manager/ui-components";

type Props = {
  repository: Repository,
  onChange: (pr: Object) => void // todo add type
};

type State = {
  branches: string[],
  source?: string,
  target?: string,
  title?: string,
  description?: string
};

class CreateForm extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      branches: []
    }
  }

  componentDidMount() {
    const { repository } = this.props;
    // TODO set loading state for the selects
    // TODO error handling
    apiClient.get(repository._links.branches.href)
      .then(response => response.json())
      .then(collection => collection._embedded.branches)
      .then(branches => branches.map(b => b.name))
      .then(names => this.setState({branches: names}));
  }

  handleFormChange = (value: string, name: string) => {
    this.setState({
      [name]: value
    }, this.notifyAboutChangedForm);
  };

  notifyAboutChangedForm = () => {
    const pullRequest = { ...this.state, branches: undefined }; // we don't need the branches
    this.props.onChange(pullRequest);
  };

  render() {
    const options = this.state.branches.map((branch) => ({
      label: branch,
      value: branch
    }));

    // TODO fix form submit
    return (
      <form>

        <div className="columns">
          <div className="column">
            <Select name="source" label="Source Branch" options={options} onChange={this.handleFormChange}/>
          </div>
          <div className="column">
            <Select name="target" label="Target Branch" options={options} onChange={this.handleFormChange}/>
          </div>
        </div>

        <InputField name="title" label="Title" onChange={this.handleFormChange} />
        <Textarea name="description" label="Description" onChange={this.handleFormChange} />

      </form>
    );
  }

}

export default CreateForm;
