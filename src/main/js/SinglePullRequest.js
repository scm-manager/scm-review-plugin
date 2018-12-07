// @flow
import React from "react";
import {
  Title,
  Loading,
  ErrorPage,
  DateFromNow
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { translate } from "react-i18next";
import { withRouter } from "react-router-dom";
import {getPullRequest, merge} from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import MergeButton from "./MergeButton";
import injectSheet from "react-jss";
import classNames from "classnames";

const styles = {
  bottomSpace: {
    marginBottom: "1.5em"
  }
};

type Props = {
  repository: Repository,
  classes: any,
  t: string => string,
  match: any
};

type State = {
  pullRequest: PullRequest,
  error?: Error,
  loading: boolean
};

class SinglePullRequest extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      pullRequest: null
    };
  }

  componentDidMount(): void {
    const { repository } = this.props;
    const pullRequestNumber = this.props.match.params.pullRequestNumber;
    const url = repository._links.pullRequest.href + "/" + pullRequestNumber;
    getPullRequest(url).then(response => {
      if (response.error) {
        this.setState({
          error: response.error,
          loading: false
        });
      } else {
        this.setState({
          pullRequest: response,
          loading: false
        });
      }
    });
  }

  merge = () => {
    const {repository} = this.props;
    const {pullRequest} = this.state;
    merge(repository._links.merge.href, {
        sourceRevision: pullRequest.source,
        targetRevision: pullRequest.target
      })
      .then(response => {
        console.log(response.error);
        if (response.error) {
          this.setState({ error: response.error, loading: false });
        } else {
          console.log("merged");
        }
      });
  }

  render() {
    const { repository, t, classes } = this.props;
    const { pullRequest, error, loading } = this.state;
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

    return (
      <div className="columns">
        <div className="column">
          <Title title={" #" + pullRequest.id + " " + pullRequest.title} />

          <div className="media">
            <div className="media-content">
              <div>
                <span className="tag is-light is-medium">
                  {pullRequest.source}
                </span>{" "}
                <i className="fas fa-long-arrow-alt-right" />{" "}
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
              <DateFromNow date={pullRequest.creationDate} />
            </div>
          </div>
          <PullRequestInformation repository={repository} />
          <MergeButton merge={() => this.merge()}/>
        </div>
      </div>
    );
  }
}

export default withRouter(
  injectSheet(styles)(translate("plugins")(SinglePullRequest))
);
