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
import org.apache.shiro.SecurityUtils;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Extension
@Requires("scm-landingpage-plugin")
public class MyPullRequests implements MyDataProvider {

  private final OpenPullRequestProvider pullRequestProvider;
  private final PullRequestMapper mapper;

  @Inject
  public MyPullRequests(OpenPullRequestProvider pullRequestProvider, PullRequestMapper mapper) {
    this.pullRequestProvider = pullRequestProvider;
    this.mapper = mapper;
  }

  @Override
  public Iterable<MyData> getData() {
    String subject = SecurityUtils.getSubject().getPrincipal().toString();
    Collection<MyData> result = new ArrayList<>();
    pullRequestProvider.findOpenAndDraftPullRequests((repository, stream) -> stream
        .filter(pr -> pr.getAuthor().equals(subject))
        .forEach(pr -> result.add(new MyPullRequestData(repository, pr, mapper)))
    );
    return result;
  }

  @Override
  public Optional<String> getType() {
    return Optional.of(MyPullRequestData.class.getSimpleName());
  }
}
