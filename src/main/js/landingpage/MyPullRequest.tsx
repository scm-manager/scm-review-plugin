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
import { binder } from "@scm-manager/ui-extensions";
import { AvatarImage, CardColumnSmall, Icon, Tag } from "@scm-manager/ui-components";
import ReviewerIcon from "../table/ReviewerIcon";
import { DataType } from "./DataType";

type Props = {
  data: DataType;
};

const TodoTag = styled(Tag)`
  margin-left: 0.5em;
  pointer-events: all;
`;

const ReviewerIconWithPointer = styled(ReviewerIcon)`
  pointer-events: all;
`;

const BranchesContainer = styled.div`
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const Title = styled.div`
  border: 0 !important;
`;

const TitleContainer = styled.strong`
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const SubtitleContainer = styled.div`
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const MyPullRequest: FC<Props> = ({ data }) => {
  const [t] = useTranslation("plugins");
  const { namespace, name, pullRequest } = data;

  const link = `/repo/${namespace}/${name}/pull-request/${pullRequest.id}`;
  const person = {
    name: data.pullRequest.author?.displayName,
    mail: data.pullRequest.author?.mail
  };
  const avatar = binder.hasExtension("avatar.factory") ? (
    <AvatarImage className="level-item" person={person} />
  ) : (
    <Icon name="code-branch" className="fa-rotate-180 fa-fw fa-lg" color="inherit" />
  );

  const title = (
    <>
      #{pullRequest.id} {pullRequest.title}
    </>
  );
  const subtitle = t("scm-review-plugin.landingpage.myPullRequests.cardSubtitle", { namespace, name });

  const todos = pullRequest.tasks?.todo || 0;
  const branches = (
    <BranchesContainer className="is-hidden-mobile is-flex-grow-1 is-flex-shrink-1 mr-2">
      {pullRequest.source}&nbsp;
      <Icon name="long-arrow-alt-right" />
      &nbsp;{pullRequest.target}
      {todos > 0 && (
        <TodoTag label={`${todos}`} title={t("scm-review-plugin.pullRequest.tasks.todo", { count: todos })} />
      )}
    </BranchesContainer>
  );
  const footerRight = <ReviewerIconWithPointer reviewers={pullRequest.reviewer} />;

  const footer = (
    <>
      <SubtitleContainer>{subtitle}</SubtitleContainer>
      <div className="is-flex is-justify-content-space-between mt-2">
        {branches} {footerRight}
      </div>
    </>
  );

  const titleComponent = (
    <Title className="media">
      <TitleContainer className="media-content">{title}</TitleContainer>
      <Tag>{pullRequest.status}</Tag>
    </Title>
  );

  return <CardColumnSmall link={link} contentLeft={titleComponent} contentRight={""} footer={footer} avatar={avatar} />;
};

export default MyPullRequest;
