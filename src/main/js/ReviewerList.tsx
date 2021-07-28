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
import React, { FC } from "react";
import { useTranslation, WithTranslation, withTranslation } from "react-i18next";
import styled from "styled-components";
import { Icon } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";

type Props = {
  pullRequest: PullRequest;
};

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

const ReviewerList: FC<Props> = ({ pullRequest }) => {
  const [t] = useTranslation("plugins");

  const reviewers = pullRequest.reviewer || [];

  return (
    <>
      {reviewers?.length > 0 ? (
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
};

export default ReviewerList;
