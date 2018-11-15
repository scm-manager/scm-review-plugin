//@flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import {InputField, Textarea, Select, ErrorNotification} from "@scm-manager/ui-components";
import type {PullRequest} from "./PullRequest";
import {getBranches} from "./pullRequest";

type Props = {
  repository: Repository,
  onChange: (pr: PullRequest) => void
};

type State = {
  pullRequest?: PullRequest,
  branches: string[],
  loading: boolean,
  error?: boolean
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
      getBranches(repository._links.branches.href)
      .then(result => {
        if(result.error){
          this.setState({
            loading: false,
            error: result.error
          });
        }
        else {
          this.setState(
            {
              branches: result,
              loading: false,
              pullRequest: {
                source: result[0], //set first entry, otherwise nothing is select in state even if one branch is shown in Select component at beginning
                target: result[0]
              }
            }
          )
        }
      });
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
    const {loading, error} = this.state;
    const options = this.state.branches.map((branch) => ({
      label: branch,
      value: branch
    }));

    if(error){
      return <ErrorNotification error={error} />
    }

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
