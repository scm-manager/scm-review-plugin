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

package com.cloudogu.scm.review;

import sonia.scm.ExceptionWithContext;
import sonia.scm.repository.Repository;

import static sonia.scm.ContextEntry.ContextBuilder.entity;

public class PullRequestNotSupportedException extends ExceptionWithContext {

  private static final String CODE = "8xR8t7eBY1";
  private static final String MESSAGE_TEMPLATE = "merge is not supported by repositories of type %s, which is required for pull requests";

  public PullRequestNotSupportedException(Repository repository) {
    super(entity(repository).build(), String.format(MESSAGE_TEMPLATE, repository.getType()));
  }

  @Override
  public String getCode() {
    return CODE;
  }
}
