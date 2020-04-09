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
import classNames from "classnames";
import styled from "styled-components";
import { binder } from "@scm-manager/ui-extensions";
import { AvatarImage, CardColumn, Icon, Tag } from "@scm-manager/ui-components";
import ReviewerIcon from "../table/ReviewerIcon";
import { DataType } from "./DataType";

const PullRequestEntryWrapper = styled.div`
  .overlay-column {
    width: calc(50% - 2.25rem);

    @media screen and (max-width: 768px) {
      width: calc(100% - 1.5rem);
    }
  }
`;

const TodoTag = styled(Tag)`
  margin-left: 0.5em;
  pointer-events: all;
`;

const ReviewerIconWithPointer = styled(ReviewerIcon)`
  pointer-events: all;
`;

const FixedSizedIcon = styled(Icon)`
  width: 64px;
  height: 64px;
`;

type Props = {
  data: DataType;
};

const MyPullRequest: FC<Props> = ({ data }) => {
  const [t] = useTranslation("plugins");
  const { namespace, name, pullRequest } = data;

  const link = `/repo/${namespace}/${name}/pull-request/${pullRequest.id}`;

  const avatar = binder.hasExtension("avatar.factory") ? (
    <AvatarImage className="level-item" person={data.pullRequest.author} />
  ) : (
    <FixedSizedIcon name="code-branch fa-rotate-180 fa-fw fa-3x" color="inherit" />
  );

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
      <Icon name="long-arrow-alt-right" />
      &nbsp;{pullRequest.target}
      {todos > 0 && (
        <TodoTag label={`${todos}`} title={t("scm-review-plugin.pullRequest.tasks.todo", { count: todos })} />
      )}
    </div>
  );

  return (
    <div className={classNames("card-columns", "content")}>
      <PullRequestEntryWrapper className={classNames("box", "box-link-shadow", "column", "is-clipped")}>
        <CardColumn
          link={link}
          avatar={avatar}
          title={title}
          description={subtitle}
          footerLeft={branches}
          footerRight={<ReviewerIconWithPointer reviewers={pullRequest.reviewer} />}
        />
      </PullRequestEntryWrapper>
    </div>
  );
};

export default MyPullRequest;
