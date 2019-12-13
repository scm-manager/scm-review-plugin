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
