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
import { DateFromNow, Hit, Notification, RepositoryAvatar, TextHitField } from "@scm-manager/ui-components";
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

const CommentWrapper = styled.p`
  white-space: pre-wrap;
`;

const CommentHitRenderer: FC<HitProps> = ({ hit }) => {
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
        <div className="is-flex is-clipped">
          <div className="ml-2">
            <Link
              className="is-ellipsis-overflow"
              to={`/repo/${repository.namespace}/${repository.name}/pull-request/${
                (hit.fields.pullRequestId as ValueHitField).value
              }/comments#comment-${(hit.fields.id as ValueHitField).value}`}
            >
              <CommentWrapper>
                <TextHitField field="comment" hit={hit} truncateValueAt={1024} />
              </CommentWrapper>
            </Link>
            <div className="mt-2">
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
            </div>
          </div>
        </div>
      </Hit.Content>
      <Hit.Right>
        {hit?._embedded?.user ? <p>{hit._embedded.user.displayName}</p> : null}
        <DateFromNow date={(hit.fields.date as ValueHitField)?.value as Date} />
      </Hit.Right>
    </Hit>
  );
};

export default CommentHitRenderer;
