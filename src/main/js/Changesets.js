//@flow
import React from "react";
import type {Repository} from "@scm-manager/ui-types";
import {createChangesetUrl, getChangesets} from "./pullRequest";
import {ErrorNotification, ChangesetList, Loading} from "@scm-manager/ui-components";

type Props = {
  repository: Repository,
  source: string,
  target: string
};

type State = {
  changesets?: Changeset,
  error?: Error,
  loading: false
};

class Changesets extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);

    this.state = {
      loading: true
    };
  }

  createChangesetLink = () => {
    const { repository, source, target } = this.props;
    return createChangesetUrl(repository, source, target);
  };

  fetchChangesets = (url: string) => {
    getChangesets(url).then(changesets => {
      this.setState({
        changesets: changesets._embedded.changesets,
        error: undefined,
        loading: false
      })
    }).catch(error => {
      this.setState({
        error,
        loading: false
      })
    });
  };

  loadChangesets = () => {
    const url = this.createChangesetLink();
    if (! url) {
      this.setState({
        loading: false,
        error: new Error("incoming changesets are not supported")
      })
    } else {
      this.fetchChangesets(url);
    }
  };

  componentDidMount() {
    this.loadChangesets();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.source !== this.props.source || prevProps.target !== this.props.target) {
      this.loadChangesets();
    }
  }

  render() {
    const { repository } = this.props;
    const { changesets, error, loading } = this.state;

    if (error) {
      return <ErrorNotification error={ error } />;
    } else if (loading) {
      return <Loading />;
    } else {
      return <ChangesetList repository={repository} changesets={changesets} />;
    }
  }

}

export default Changesets;
