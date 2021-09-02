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
import {
  DateFromNow,
  Hit,
  HitProps,
  Notification,
  RepositoryAvatar,
  Tag,
  TextHitField
} from "@scm-manager/ui-components";
import { ValueHitField } from "@scm-manager/ui-types";
import styled from "styled-components";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { evaluateTagColor } from "../pullRequest";

const ShortTag = styled(Tag).attrs(() => ({
  className: "is-medium",
  color: "light"
}))`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 25em;
`;

const PullRequestHitRenderer: FC<HitProps> = ({ hit }) => {
  const [t] = useTranslation("plugins");
  const repository = hit._embedded?.repository;

  if (!repository) {
    return <Notification type="danger">{t("scm-review-plugin.search.invalidResult")}</Notification>;
  }

  return (
    <Hit>
      <Hit.Left>
        <RepositoryAvatar repository={repository} size={48} />
      </Hit.Left>
      <Hit.Content>
        <div className="is-flex">
          <div className="ml-2">
            <Link
              className="is-ellipsis-overflow"
              to={`/repo/${repository.namespace}/${repository.name}/pull-request/${
                (hit.fields.id as ValueHitField).value
              }`}
            >
              {(hit.fields.title as ValueHitField).value as string}
            </Link>
            <p>
              <TextHitField field="description" hit={hit} truncateValueAt={1024} />
            </p>
            <p className="mt-1">
              <ShortTag
                label={(hit.fields.source as ValueHitField).value as string}
                title={t("scm-review-plugin.search.source")}
              />{" "}
              <i className="fas fa-long-arrow-alt-right" />{" "}
              <ShortTag
                label={(hit.fields.target as ValueHitField).value as string}
                title={t("scm-review-plugin.search.target")}
              />
            </p>
          </div>
        </div>
      </Hit.Content>
      <Hit.Right className="is-flex is-flex-direction-column">
        <DateFromNow
          date={
            hit.fields.lastModified
              ? ((hit.fields.lastModified as ValueHitField)?.value as Date)
              : ((hit.fields.creationDate as ValueHitField)?.value as Date)
          }
        />
        <Tag
          className="is-medium mt-1"
          color={evaluateTagColor((hit.fields.status as ValueHitField).value as string)}
          label={t("scm-review-plugin.pullRequest.statusLabel." + (hit.fields.status as ValueHitField).value)}
          icon={((hit.fields.emergencyMerged as ValueHitField).value as boolean) ? "exclamation-triangle" : undefined}
        />
      </Hit.Right>
    </Hit>
  );
};

export default PullRequestHitRenderer;
