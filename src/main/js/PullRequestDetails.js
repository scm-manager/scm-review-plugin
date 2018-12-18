// @flow
import React from "react";
import {DateFromNow, ErrorPage, Loading, Notification, Title} from "@scm-manager/ui-components";
import type {Repository} from "@scm-manager/ui-types";
import type {PullRequest} from "./types/PullRequest";
import {translate} from "react-i18next";
import {Link, withRouter} from "react-router-dom";
import {merge} from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import MergeButton from "./MergeButton";
import type { History } from "history";
import injectSheet from "react-jss";
import classNames from "classnames";

const styles = {
  bottomSpace: {
    marginBottom: "1.5em"
  }
};

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  classes: any,
  t: string => string,
  match: any,
  history: History
};

type State = {
  pullRequest: PullRequest,
  error?: Error,
  loading: boolean,
  mergePossible?: boolean,
  mergeButtonLoading: boolean,
  showNotification: boolean
};

class SinglePullRequest extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      pullRequest: this.props.pullRequest,
      mergeButtonLoading: true,
      showNotification: false
    };
  }

  componentDidMount(): void {
    const { pullRequest} = this.props;
    this.getMergeDryRun(pullRequest);
  }

  mergePerformed = () => {
    const { history, match } = this.props;
      this.setState({
        showNotification: true,
        loading: true,
      });
    history.push({
      pathname: `${match.url}/comments`,
      state: {
        from: this.props.match.url+"/updated",
      }
    });
  };

  getMergeDryRun(pullRequest: PullRequest) {
    const {repository} = this.props;
    if (repository._links.mergeDryRun && repository._links.mergeDryRun.href) {
      merge(repository._links.mergeDryRun.href, pullRequest).then(response => {
        if (response.conflict) {
          this.setState({
            mergeButtonLoading: false,
            loading: false,
            mergePossible: false});
        } else if (response.error) {
          this.setState({
            error: true,
            loading: false,
            mergeButtonLoading: false
          });
        } else {
          this.setState({
            mergePossible: true,
            loading: false,
            mergeButtonLoading: false});
        }
      });
    }else{
      // TODO: what to do if the link does not exists?
    }
  }

  performMerge = () => {
    const { repository } = this.props;
    const { pullRequest } = this.state;
    this.setMergeButtonLoadingState();
    merge(repository._links.merge.href, pullRequest).then(response => {
      if (response.error) {
        this.setState({ error: response.error, mergeButtonLoading: false });
      } else if (response.conflict) {
        this.setState({
          mergePossible: true,
          mergeButtonLoading: false
        });
      } else {
        this.setState({ loading: true, mergeButtonLoading: false });
        this.mergePerformed();
      }
    });
  };

  setMergeButtonLoadingState = () => {
    this.setState({
      mergeButtonLoading: true
    });
  };

  onClose = () => {
    this.setState({
      ...this.state,
      showNotification: false,
      mergeConflict: false
    });
  };

  render() {
    const { repository, match, t, classes } = this.props;
    const {
      pullRequest,
      error,
      loading,
      mergeButtonLoading,
      mergePossible,
      showNotification
    } = this.state;
    let description = null;
    if (error) {
      return (
        <ErrorPage
          title={t("scm-review-plugin.show-pull-request.error-title")}
          subtitle={t("scm-review-plugin.show-pull-request.error-subtitle")}
          error={error}
        />
      );
    }

    if (!pullRequest || loading) {
      return <Loading />;
    }

    if (pullRequest.description) {
      description = (
        <div className="media">
          <div className="media-content">
            {pullRequest.description.split("\n").map(line => {
              return (
                <span>
                  {line}
                  <br />
                </span>
              );
            })}
          </div>
        </div>
      );
    }

    let mergeNotification = null;
    if (showNotification) {
      mergeNotification = (
        <Notification
          type={"info"}
          children={t("scm-review-plugin.show-pull-request.notification")}
          onClose={() => this.onClose()}
        />
      );
    }

    let mergeButton = null;
    if (repository._links.merge && repository._links.merge.href) {
      mergeButton = (
        <MergeButton
          merge={() => this.performMerge()}
          mergePossible={mergePossible}
          loading={mergeButtonLoading}
          repository={repository}
          pullRequest={pullRequest}
        />
      );
    }
    let editButton = null;
    if (pullRequest._links.update && pullRequest._links.update.href){
      const toEdit = "/repo/"+repository.namespace+"/"+repository.name+"/pull-request/"+pullRequest.id+"/edit";
      editButton = (
        <div className="level-right">
        <div className="level-item">
          <a className="level-item" >
              <span className="icon is-small">
              <Link  to={toEdit}>
                <i className="fas fa-edit"></i>
              </Link>
              </span>
          </a>

        </div>
      </div>)
    }

    return (
      <div className="columns">
        <div className="column">

          {editButton}

          <div className="level-left">
            <div className="level-item">
              <Title title={" #" + pullRequest.id + " " + pullRequest.title}/>
            </div>
          </div>

          {mergeNotification}

          <div className="media">
            <div className="media-content">
              <div>
                <span className="tag is-light is-medium">
                  {pullRequest.source}
                </span>{" "}
                <i className="fas fa-long-arrow-alt-right"/>{" "}
                <span className="tag is-light is-medium">
                  {pullRequest.target}
                </span>
              </div>
            </div>
            <div className="media-right">{pullRequest.status}</div>
          </div>

          {description}

          <div className={classNames("media", classes.bottomSpace)}>
            <div className="media-content">{pullRequest.author}</div>
            <div className="media-right">
              <DateFromNow date={pullRequest.creationDate}/>
            </div>
          </div>

          {mergeButton}

          <PullRequestInformation pullRequest={pullRequest} baseURL={match.url} repository={repository}
                                  source={pullRequest.source} target={pullRequest.target}/>
        </div>
      </div>
    );
  }
}

export default withRouter(
  injectSheet(styles)(translate("plugins")(SinglePullRequest))
);
