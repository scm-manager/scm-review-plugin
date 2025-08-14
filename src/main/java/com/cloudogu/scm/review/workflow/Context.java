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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import lombok.AllArgsConstructor;
import lombok.Value;
import sonia.scm.repository.Repository;

@Value
@AllArgsConstructor
public class Context {
  private Repository repository;
  private PullRequest pullRequest;
  private Object configuration;

  public <C> C getConfiguration(Class<C> configurationType) {
    return configurationType.cast(configuration);
  }

  public Context(Repository repository, PullRequest pullRequest) {
    this(repository, pullRequest, null);
  }
}
