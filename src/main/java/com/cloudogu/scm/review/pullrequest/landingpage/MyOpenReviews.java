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
import com.cloudogu.scm.landingpage.mytasks.MyTaskProvider;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import jakarta.inject.Inject;
import org.apache.shiro.SecurityUtils;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;

import java.util.ArrayList;
import java.util.Collection;

@Extension
@Requires("scm-landingpage-plugin")
public class MyOpenReviews implements MyTaskProvider {
  private final OpenPullRequestProvider pullRequestProvider;
  private final PullRequestMapper mapper;

  @Inject
  public MyOpenReviews(OpenPullRequestProvider pullRequestProvider, PullRequestMapper mapper) {
    this.pullRequestProvider = pullRequestProvider;
    this.mapper = mapper;
  }

  @Override
  public Iterable<MyTask> getTasks() {
    String subject = SecurityUtils.getSubject().getPrincipal().toString();
    Collection<MyTask> result = new ArrayList<>();
    pullRequestProvider.findOpenPullRequests((repository, stream) -> stream
        .filter(pr -> pr.getReviewer().entrySet().stream().anyMatch(entry -> entry.getKey().equals(subject) && !entry.getValue()))
        .forEach(pr -> result.add(new MyPullRequestReview(repository, pr, mapper)))
    );
    return result;
  }
}
