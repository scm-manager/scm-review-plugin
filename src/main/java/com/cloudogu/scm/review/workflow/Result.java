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

import lombok.Getter;

@Getter
public final class Result {
  private final boolean failed;
  private final Class<? extends Rule> rule;
  private final Object context;

  private Result(boolean failed, Class<? extends Rule> rule) {
    this(failed, rule, null);
  }

  private Result(boolean failed, Class<? extends Rule> rule, Object context) {
    this.failed = failed;
    this.rule = rule;
    this.context = context;
  }

  public static Result success(Class<? extends Rule> rule) {
    return new Result(false, rule);
  }

  public static Result failed(Class<? extends Rule> rule) {
    return new Result(true, rule);
  }

  public static Result failed(Class<? extends Rule> rule, Object context) {
    return new Result(true, rule, context);
  }

  public boolean isSuccess() {
    return !failed;
  }
}
