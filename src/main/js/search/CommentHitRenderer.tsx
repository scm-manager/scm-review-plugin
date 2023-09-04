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
            <TextHitField field="comment" hit={hit} truncateValueAt={1024} />
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
