import React from "react";
import { ErrorNotification } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";
import { getApproval, handleApproval } from "./pullRequest";
import DisapprovalButton from "./DisapprovalButton";
import ApprovalButton from "./ApprovalButton";

type Props = {
  pullRequest: PullRequest;
};

type State = {
  loading: boolean;
  error?: Error;
  approve?: boolean;
  link?: string;
};

export default class ApprovalContainer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true
    };
  }

  componentDidMount(): void {
    const { pullRequest } = this.props;
    if (pullRequest && pullRequest._links.approval && pullRequest._links.approval.href) {
      this.getApproval(pullRequest);
    }
  }

  getApproval(pullRequest: PullRequest) {
    if (pullRequest && pullRequest._links.approval && pullRequest._links.approval.href) {
      getApproval(pullRequest._links.approval.href).then(response => {
        if (response.error) {
          this.setState({
            loading: false,
            error: response.error
          });
        } else {
          if (response._links.approve) {
            this.setState({
              loading: false,
              approve: false,
              link: response._links.approve.href
            });
          } else if (response._links.disapprove) {
            this.setState({
              loading: false,
              approve: true,
              link: response._links.disapprove.href
            });
          }
        }
      });
    }
  }

  handleApproval = () => {
    const { pullRequest } = this.props;
    const { link } = this.state;
    this.setState({
      loading: true
    });
    if (link) {
      handleApproval(link).then(response => {
        this.setState({
          error: response.error
        });
        this.getApproval(pullRequest);
      });
    }
  };

  render() {
    const { error, loading, approve } = this.state;
    if (error) {
      return <ErrorNotification error={error} />;
    }

    if (!approve) {
      return <ApprovalButton loading={loading} action={this.handleApproval} />;
    }
    return <DisapprovalButton loading={loading} action={this.handleApproval} />;
  }
}
