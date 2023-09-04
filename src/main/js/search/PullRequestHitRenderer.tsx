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
import classNames from "classnames";
import styled from "styled-components";
import { DateFromNow, HitProps, Notification, RepositoryAvatar, TextHitField } from "@scm-manager/ui-components";
import { HighlightedHitField, ValueHitField } from "@scm-manager/ui-types";
import { Link } from "react-router-dom";
import { CardList, CardListBox } from "@scm-manager/ui-layout";
import { Trans, useTranslation } from "react-i18next";
import { evaluateTagColor } from "../pullRequest";

const StyledLink = styled(Link)`
  gap: 0.5rem;
`;

const SubtitleRow = styled(CardListBox.Card.Row)`
  white-space: pre;
`;

const PullRequestHitRenderer: FC<HitProps> = ({ hit }) => {
  const [t] = useTranslation("plugins");
  const repository = hit._embedded?.repository;
  const description = hit.fields["description"];

  if (!repository) {
    return <Notification type="danger">{t("scm-review-plugin.search.invalidResult")}</Notification>;
  }

  return (
    <CardList.Card key={(hit.fields.id as ValueHitField).value as string}>
      <CardList.Card.Row>
        <CardList.Card.Title>
          <StyledLink
            to={`/repo/${repository.namespace}/${repository.name}/pull-request/${
              (hit.fields.id as ValueHitField).value
            }`}
            className={classNames("is-flex", "is-justify-content-flex-start", "is-align-items-center")}
          >
            <RepositoryAvatar repository={repository} size={16} />
            <span>{(hit.fields.title as ValueHitField).value as string}</span>
            <span className="has-text-secondary" aria-label={t("scm-review-plugin.pullRequests.aria.id")}>
              <span aria-hidden>#</span>
              {(hit.fields.id as ValueHitField).value as string}
            </span>
          </StyledLink>
        </CardList.Card.Title>
      </CardList.Card.Row>
      {((description as ValueHitField).value || (description as HighlightedHitField).fragments) && (
        <CardList.Card.Row className="">
          <TextHitField field="description" hit={hit} truncateValueAt={1024} />
        </CardList.Card.Row>
      )}
      <SubtitleRow className="is-flex is-flex-wrap-wrap is-size-7 has-text-secondary">
        <div>
          <Trans
            t={t}
            i18nKey="scm-review-plugin.search.subtitle"
            values={{
              source: (hit.fields.source as ValueHitField).value as string,
              target: (hit.fields.target as ValueHitField).value as string
            }}
            components={{
              highlight: <span className="has-text-default" />,
              date: (
                <DateFromNow
                  className="is-relative"
                  date={
                    hit.fields.lastModified
                      ? ((hit.fields.lastModified as ValueHitField)?.value as Date)
                      : ((hit.fields.creationDate as ValueHitField)?.value as Date)
                  }
                />
              ),
              space: <span />
            }}
          />
        </div>
        <span
          className={classNames(
            "tag is-rounded ml-auto",
            `is-${evaluateTagColor((hit.fields.status as ValueHitField).value as string)}`
          )}
          aria-label={t("scm-review-plugin.pullRequests.aria.status")}
        >
          {(hit.fields.status as ValueHitField).value as string}
        </span>
      </SubtitleRow>
    </CardList.Card>
  );
};

export default PullRequestHitRenderer;
