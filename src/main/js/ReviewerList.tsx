import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import styled from "styled-components";
import { Icon } from "@scm-manager/ui-components";
import { PullRequest, Reviewer } from "./types/PullRequest";

type Props = WithTranslation & {
  pullRequest: PullRequest;
  reviewer: Reviewer[];
};

type State = {};

const UserLabel = styled.div.attrs(() => ({
  className: "field-label is-inline-flex"
}))`
  text-align: left;
  margin-right: 0;
  min-width: 5.5em;
`;

const UserField = styled.div.attrs(() => ({
  className: "field-body is-inline-flex"
}))`
  flex-grow: 8;
`;

const UserInlineListItem = styled.li`
  display: inline-block;
  font-weight: bold;
`;

class ReviewerList extends React.Component<Props, State> {
  render() {
    const { pullRequest, reviewer, t } = this.props;
    const reviewers = reviewer ? reviewer : pullRequest.reviewer;
    return (
      <>
        {reviewers.length > 0 ? (
          <div className="field is-horizontal">
            <UserLabel>{t("scm-review-plugin.pullRequest.reviewer")}:</UserLabel>
            <UserField>
              <ul className="is-separated">
                {reviewers.map(reviewer => {
                  if (reviewer.approved) {
                    return (
                      <UserInlineListItem key={reviewer.id}>
                        {reviewer.displayName}{" "}
                        <Icon
                          title={t("scm-review-plugin.pullRequest.details.approved", { name: reviewer.displayName })}
                          name="check"
                          color="success"
                        />
                      </UserInlineListItem>
                    );
                  }
                  return <UserInlineListItem key={reviewer.id}>{reviewer.displayName}</UserInlineListItem>;
                })}
              </ul>
            </UserField>
          </div>
        ) : (
          ""
        )}
      </>
    );
  }
}

export default withTranslation("plugins")(ReviewerList);
