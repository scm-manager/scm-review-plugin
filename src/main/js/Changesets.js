//@flow
import React from "react";
import type {PagedCollection, Repository} from "@scm-manager/ui-types";
import {createChangesetUrl, getChangesets} from "./pullRequest";
import {ErrorNotification, ChangesetList, LinkPaginator, Loading} from "@scm-manager/ui-components";
import { withRouter } from "react-router-dom";

type Props = {
  repository: Repository,
  source: string,
  target: string,
  match: any
};

type ChangesetCollection = PagedCollection & {
  _embedded: {
    changesets: Changeset[]
  }
}

type State = {
  changesets: ChangesetCollection,
  error?: Error,
  loading: false,
  page: 1
};

class Changesets extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);

    this.state = {
      loading: true
    };
  }

  getCurrentPage(): number {
    const { match } = this.props;
    if (match.params.page) {
      return parseInt(match.params.page);
    }
    return 1;
  }

  createChangesetLink = () => {
    const { repository, source, target } = this.props;
    return createChangesetUrl(repository, source, target);
  };

  fetchChangesets = (url: string) => {
    const page = this.getCurrentPage();
    getChangesets(`${url}?page=${page - 1}`).then(changesets => {
      this.setState({
        changesets,
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
    if (prevProps.source !== this.props.source || prevProps.target !== this.props.target || prevProps.match.params.page !== this.props.match.params.page) {
      this.loadChangesets();
    }
  }

  render() {
    const { repository } = this.props;
    const { changesets, error, loading } = this.state;
    const page = this.getCurrentPage();

    if (error) {
      return <ErrorNotification error={ error } />;
    } else if (loading) {
      return <Loading />;
    } else {
      return (
        <>
          <ChangesetList repository={repository} changesets={changesets._embedded.changesets} />
          <LinkPaginator collection={changesets} page={page} />
        </>
      );
    }
  }

}

export default withRouter(Changesets);
