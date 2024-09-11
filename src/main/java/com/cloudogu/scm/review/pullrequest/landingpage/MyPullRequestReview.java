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

import com.cloudogu.scm.landingpage.mytasks.MyTask;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import lombok.Getter;
import sonia.scm.repository.Repository;

@Getter
public class MyPullRequestReview extends MyTask {
  private final String namespace;
  private final String name;
  private final PullRequestDto pullRequest;

  MyPullRequestReview(Repository repository, PullRequest pullRequest, PullRequestMapper mapper) {
    this(repository.getNamespace(), repository.getName(), mapper.map(pullRequest, repository));
  }

  MyPullRequestReview(String namespace, String name, PullRequestDto pullRequest) {
    super(MyPullRequestReview.class.getSimpleName());
    this.namespace = namespace;
    this.name = name;
    this.pullRequest = pullRequest;
  }
}
