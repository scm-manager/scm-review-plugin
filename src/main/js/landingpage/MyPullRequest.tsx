/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
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
