import React from "react";
import { ErrorNotification } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";
import { getSubscription, handleSubscription } from "./pullRequest";
import SubscribeButton from "./SubscribeButton";
import UnsubscribeButton from "./UnsubscribeButton";

type Props = {
  pullRequest: PullRequest;
};

type State = {
  loading: boolean;
  error?: Error;
  subscribe?: boolean;
  link?: string;
};

export default class SubscriptionContainer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true
    };
  }

  componentDidMount(): void {
    const { pullRequest } = this.props;
    if (pullRequest && pullRequest._links.subscription && pullRequest._links.subscription.href) {
      this.getSubscription(pullRequest);
    }
  }

  getSubscription(pullRequest: PullRequest) {
    if (pullRequest && pullRequest._links.subscription && pullRequest._links.subscription.href) {
      getSubscription(pullRequest._links.subscription.href).then(response => {
        if (response.error) {
          this.setState({
            loading: false,
            error: response.error
          });
        } else {
          if (response._links.subscribe) {
            this.setState({
              loading: false,
              subscribe: false,
              link: response._links.subscribe.href
            });
          } else if (response._links.unsubscribe) {
            this.setState({
              loading: false,
              subscribe: true,
              link: response._links.unsubscribe.href
            });
          }
        }
      });
    }
  }

  handleSubscription = () => {
    const { pullRequest } = this.props;
    const { link } = this.state;
    this.setState({
      loading: true
    });
    if (link) {
      handleSubscription(link).then(response => {
        this.setState({
          error: response.error
        });
        this.getSubscription(pullRequest);
      });
    }
  };

  render() {
    const { error, loading, subscribe } = this.state;
    if (error) {
      return <ErrorNotification error={error} />;
    }

    if (!subscribe) {
      return <SubscribeButton loading={loading} action={this.handleSubscription} />;
    }
    return <UnsubscribeButton loading={loading} action={this.handleSubscription} />;
  }
}
