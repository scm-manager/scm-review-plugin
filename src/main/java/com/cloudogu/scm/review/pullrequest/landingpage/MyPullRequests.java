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

package com.cloudogu.scm.review.pullrequest.landingpage;

import com.cloudogu.scm.landingpage.mydata.MyData;
import com.cloudogu.scm.landingpage.mydata.MyDataProvider;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestQueryFields;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import jakarta.inject.Inject;
import org.apache.shiro.SecurityUtils;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.store.QueryableStore;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Extension
@Requires("scm-landingpage-plugin")
public class MyPullRequests implements MyDataProvider {

  private final PullRequestMapper mapper;

  private final PullRequestStoreFactory storeFactory;
  private final RepositoryManager repositoryManager;

  @Inject
  public MyPullRequests(PullRequestMapper mapper, PullRequestStoreFactory storeFactory, RepositoryManager repositoryManager) {
    this.mapper = mapper;
    this.storeFactory = storeFactory;
    this.repositoryManager = repositoryManager;
  }

  @Override
  public Iterable<MyData> getData() {
    String subject = SecurityUtils.getSubject().getPrincipal().toString();
    try (QueryableStore<PullRequest> store = storeFactory.getOverall()) {
      return store
        .query(
          PullRequestQueryFields.AUTHOR.eq(subject),
          PullRequestQueryFields.STATUS.in(PullRequestStatus.OPEN, PullRequestStatus.DRAFT)
        ).withIds()
        .findAll()
        .stream().map(
          result -> {
            Repository repository = repositoryManager.get(result.getParentId(Repository.class).get());
            if (repository == null) {
              return null;
            }
            PullRequest pullRequest = result.getEntity();
            return new MyPullRequestData(repository, pullRequest, mapper);
          }
        ).filter(Objects::nonNull)
        .collect(Collectors.toList());
    }
  }

  @Override
  public Optional<String> getType() {
    return Optional.of(MyPullRequestData.class.getSimpleName());
  }
}
