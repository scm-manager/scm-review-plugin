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
import { RouteComponentProps, withRouter } from "react-router-dom";
import { WithTranslation, withTranslation } from "react-i18next";
import { Changeset, PagedCollection, Repository } from "@scm-manager/ui-types";
import { createChangesetUrl, getChangesets } from "./pullRequest";
import {
  ChangesetList,
  ErrorNotification,
  LinkPaginator,
  Loading,
  Notification,
  NotFoundError
} from "@scm-manager/ui-components";

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    source: string;
    target: string;
    shouldFetchChangesets?: boolean;
  };

type ChangesetCollection = PagedCollection & {
  _embedded: {
    changesets: Changeset[];
  };
};

type State = {
  changesets: ChangesetCollection;
  error?: Error;
  loading: boolean;
  page: number;
};

class Changesets extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = {
      loading: true,
      page: 1
    };
  }
  static defaultProps = {
    shouldFetchChangesets: true
  };

  getCurrentPage(): number {
    const { match } = this.props;
    if (match.params.page) {
      return parseInt(match.params.page, 10);
    }
    return 1;
  }

  createChangesetLink = () => {
    const { repository, source, target } = this.props;
    if (source && target) {
      return createChangesetUrl(repository, source, target);
    }
    return "";
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
    const { source, target, shouldFetchChangesets } = this.props;
    const url = this.createChangesetLink();
    if (!url && source && target) {
      this.setState({
        loading: false,
        error: new Error("incoming changesets are not supported")
      });
    } else {
      url && shouldFetchChangesets && this.fetchChangesets(url);
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
      if (error instanceof NotFoundError) {
        return <Notification type="info">{t("scm-review-plugin.pullRequest.noChangesets")}</Notification>;
      }
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
