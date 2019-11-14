import React from "react";
import { RouteComponentProps, withRouter } from "react-router-dom";
import { WithTranslation, withTranslation } from "react-i18next";
import { Changeset, PagedCollection, Repository } from "@scm-manager/ui-types";
import { createChangesetUrl, getChangesets } from "./pullRequest";
import { ChangesetList, ErrorNotification, LinkPaginator, Loading, Notification } from "@scm-manager/ui-components";

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    source: string;
    target: string;
  };

type ChangesetCollection = PagedCollection & {
  _embedded: {
    changesets: Changeset[];
  };
};

type State = {
  changesets: ChangesetCollection;
  error?: Error;
  loading: false;
  page: 1;
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
      return parseInt(match.params.page, 10);
    }
    return 1;
  }

  createChangesetLink = () => {
    const { repository, source, target } = this.props;
    return createChangesetUrl(repository, source, target);
  };

  fetchChangesets = (url: string) => {
    const page = this.getCurrentPage();
    getChangesets(`${url}?page=${page - 1}`)
      .then(changesets => {
        this.setState({
          changesets,
          error: undefined,
          loading: false
        });
      })
      .catch(error => {
        this.setState({
          error,
          loading: false
        });
      });
  };

  loadChangesets = () => {
    const url = this.createChangesetLink();
    if (!url) {
      this.setState({
        loading: false,
        error: new Error("incoming changesets are not supported")
      });
    } else {
      this.fetchChangesets(url);
    }
  };

  componentDidMount() {
    this.loadChangesets();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.source !== this.props.source ||
      prevProps.target !== this.props.target ||
      prevProps.match.params.page !== this.props.match.params.page
    ) {
      this.loadChangesets();
    }
  }

  renderPaginator = () => {
    const { changesets } = this.state;
    const page = this.getCurrentPage();

    if (changesets && changesets.pageTotal > 1) {
      return (
        <div className="panel-footer">
          <LinkPaginator collection={changesets} page={page} />
        </div>
      );
    }
    return null;
  };

  render() {
    const { repository, t } = this.props;
    const { changesets, error, loading } = this.state;

    if (error) {
      return <ErrorNotification error={error} />;
    } else if (loading) {
      return <Loading />;
    } else if (changesets && changesets._embedded && changesets._embedded.changesets) {
      if (changesets._embedded.changesets.length !== 0) {
        return (
          <div className="panel">
            <div className="panel-block">
              <ChangesetList repository={repository} changesets={changesets._embedded.changesets} />
            </div>
            {this.renderPaginator()}
          </div>
        );
      } else {
        return <Notification type="info">{t("scm-review-plugin.pullRequest.noChangesets")}</Notification>;
      }
    }
    return null;
  }
}

export default withRouter(withTranslation("plugins")(Changesets));
