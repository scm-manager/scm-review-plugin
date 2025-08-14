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
import { DateFromNow, Notification, RepositoryAvatar, TextHitField } from "@scm-manager/ui-components";
import {
  DisplayedUser,
  HalRepresentationWithEmbedded,
  HitField,
  ValueHitField,
  EmbeddedRepository
} from "@scm-manager/ui-types";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { EmergencyMergeTag, SystemTag } from "../comment/tags";
import styled from "styled-components";
import { CardList } from "@scm-manager/ui-layout";
import classNames from "classnames";

const HitTitleWrapper = styled.div`
    text-wrap: wrap;
`;

type Embedded = EmbeddedRepository & {
  user: DisplayedUser;
};

type HitType = HalRepresentationWithEmbedded<Embedded> & {
  score: number;
  fields: { [name: string]: HitField };
};

type HitProps = {
  hit: HitType;
};

const StyledLink = styled(Link)`
  gap: 0.5rem;
`;

const CommentHitRenderer: FC<HitProps> = ({ hit }) => {
  const [t] = useTranslation("plugins");
  const repository = hit._embedded?.repository;
  const hasTag =
    (hit.fields.systemComment as ValueHitField).value || (hit.fields.emergencyMerged as ValueHitField).value;

  if (!repository) {
    return <Notification type="danger">{t("scm-review-plugin.search.invalidResult")}</Notification>;
  }

  return (
    <CardList.Card key={(hit.fields.id as ValueHitField).value as string}>
      <CardList.Card.Row>
        <CardList.Card.Title>
          <StyledLink
            to={`/repo/${repository.namespace}/${repository.name}/pull-request/${
              (hit.fields.pullRequestId as ValueHitField).value
            }/comments#comment-${(hit.fields.id as ValueHitField).value}`}
            className={classNames("is-flex", "is-justify-content-flex-start", "is-align-items-center")}
          >
            <RepositoryAvatar repository={repository} size={16} />
            <HitTitleWrapper className="is-white-space-pre is-flex-basis-0 is-flex-grow-1 is-flex-shrink-1">
              <TextHitField field="comment" hit={hit} truncateValueAt={1024} />
            </HitTitleWrapper>
          </StyledLink>
        </CardList.Card.Title>
      </CardList.Card.Row>
      {hasTag && (
        <CardList.Card.Row className="mt-2">
          {(hit.fields.systemComment as ValueHitField)?.value && (
            <p>
              <SystemTag />
            </p>
          )}
          {(hit.fields.emergencyMerged as ValueHitField)?.value && (
            <p>
              <EmergencyMergeTag />
            </p>
          )}
        </CardList.Card.Row>
      )}
      <CardList.Card.Row className="is-flex is-flex-wrap-wrap is-size-7 has-text-secondary">
        {hit?._embedded?.user ? <p>{hit._embedded.user.displayName}&nbsp; </p> : null}
        <DateFromNow className="is-relative" date={(hit.fields.date as ValueHitField)?.value as Date} />
      </CardList.Card.Row>
    </CardList.Card>
  );
};

export default CommentHitRenderer;
