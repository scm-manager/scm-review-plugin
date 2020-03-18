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
import { getApproval, handleApproval } from "./pullRequest";
import ApprovalButton from "./ApprovalButton";
import DisapprovalButton from "./DisapprovalButton";
import { Link } from "@scm-manager/ui-types";

type Props = {
  pullRequest: PullRequest;
  refreshReviewer: () => void;
};

type State = {
  loading: boolean;
  error?: Error;
};

export default class ApprovalContainer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false
    };
  }

  handleApproval = () => {
    const { refreshReviewer } = this.props;
    this.setState({
      loading: true
    });
    const link = this.createHandleApprovalLink();
    if (link) {
      handleApproval(link)
        .then(response => {
          this.setState(
            {
              loading: false
            },
            () => refreshReviewer()
          );
        })
        .catch((error: Error) => {
          this.setState({
            loading: false,
            error
          });
        });
    }
  };

  createHandleApprovalLink = () => {
    const { pullRequest } = this.props;
    if (pullRequest._links && (pullRequest._links.approve as Link)) {
      return (pullRequest._links.approve as Link).href;
    }
    return (pullRequest._links.disapprove as Link).href;
  };

  render() {
    const { error, loading } = this.state;
    if (error) {
      return <ErrorNotification error={error} />;
    }

    if (this.props.pullRequest._links && !!this.props.pullRequest._links.approve) {
      return <ApprovalButton loading={loading} action={this.handleApproval} />;
    } else if (this.props.pullRequest._links && !!this.props.pullRequest._links.disapprove) {
      return <DisapprovalButton loading={loading} action={this.handleApproval} />;
    } else {
      return null;
    }
  }
}
