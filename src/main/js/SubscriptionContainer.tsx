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
