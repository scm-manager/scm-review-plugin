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
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { AvatarImage, CardColumn, Icon, Tag } from "@scm-manager/ui-components";
import ReviewerIcon from "../../table/ReviewerIcon";

const PullRequestEntryWrapper = styled.div`
  .overlay-column {
    width: calc(50% - 3rem);

    @media screen and (max-width: 768px) {
      width: calc(100% - 1.5rem);
    }
  }
`;

const TodoTag = styled(Tag).attrs(props => ({}))`
  margin-left: 0.5em;
  pointer-events: all;
`;

const ReviewerIconWithPointer = styled(ReviewerIcon).attrs(props => ({}))`
  pointer-events: all;
`;

type Props = {
  data: any;
};
const MyPullRequest: FC<Props> = ({ data }) => {
  const [t] = useTranslation("plugins");
  const { namespace, name, pullRequest } = data;

  const link = `/repo/${namespace}/${name}/pull-request/${pullRequest.id}`;

  const avatar = <AvatarImage className="level-item" person={data.pullRequest.author} />;

  const title = (
    <b>
      #{pullRequest.id} {pullRequest.title}
    </b>
  );
  const subtitle = t("scm-review-plugin.landingpage.myPullRequests.cardSubtitle", { namespace, name });

  const todos = pullRequest.tasks.todo;
  const branches = (
    <div className="level-item">
      {pullRequest.source}&nbsp;
      <Icon name={"fas fa-long-arrow-alt-right"} />
      &nbsp;{pullRequest.target}
      {todos > 0 && (
        <TodoTag label={`${todos}`} title={t("scm-review-plugin.pullRequest.tasks.todo", { count: todos })} />
      )}
    </div>
  );

  return (
    <div className={"card-columns is-multiline"}>
      <PullRequestEntryWrapper className="box box-link-shadow column is-clipped">
        <CardColumn
          avatar={avatar}
          title={title}
          description={subtitle}
          link={link}
          footerLeft={branches}
          footerRight={<ReviewerIconWithPointer reviewers={pullRequest.reviewer} />}
        />
      </PullRequestEntryWrapper>
    </div>
  );
};

export default MyPullRequest;
