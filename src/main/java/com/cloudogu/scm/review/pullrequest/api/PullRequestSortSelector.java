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

package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import lombok.Getter;
import sonia.scm.store.QueryableStore;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestQueryFields.ID;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestQueryFields.LASTMODIFIED;
import static com.cloudogu.scm.review.pullrequest.service.PullRequestQueryFields.STATUS;

@Getter
public enum PullRequestSortSelector {
  ID_ASC(new Sort(ID, new QueryableStore.OrderOptions(QueryableStore.Order.ASC, true))),
  ID_DESC(new Sort(ID, new QueryableStore.OrderOptions(QueryableStore.Order.DESC, true))),
  STATUS_ASC(new Sort(STATUS, new QueryableStore.OrderOptions(QueryableStore.Order.ASC, false))),
  STATUS_DESC(new Sort(STATUS, new QueryableStore.OrderOptions(QueryableStore.Order.DESC, false))),
  LAST_MOD_ASC(new Sort(LASTMODIFIED, new QueryableStore.OrderOptions(QueryableStore.Order.ASC, false))),
  LAST_MOD_DESC(new Sort(LASTMODIFIED, new QueryableStore.OrderOptions(QueryableStore.Order.DESC, false)));

  private final Sort sort;

  PullRequestSortSelector(Sort sort) {
    this.sort = sort;
  }

  public record Sort(QueryableStore.QueryField<PullRequest, ?> field, QueryableStore.OrderOptions orderOptions) {
  }
}
