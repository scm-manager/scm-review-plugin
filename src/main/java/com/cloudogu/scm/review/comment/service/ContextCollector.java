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

package com.cloudogu.scm.review.comment.service;

import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * This class collects subsequent items "around" a central item in this sequence, trying to get exactly
 * {@link ContextCollector#CONTEXT_SIZE} items overall.
 */
class ContextCollector<T> {

  private static final int CONTEXT_SIZE = 7;

  private final EvictingQueue<T> context = EvictingQueue.create(CONTEXT_SIZE);

  private Integer trailingItemCount = null;

  void add(T item) {
    if (!enoughContextAdded()) {
      context.add(item);
    }
  }

  void addCentral(T item) {
    context.add(item);
    trailingItemCount = CONTEXT_SIZE / 2;
  }

  List<T> getContext() {
    return new ArrayList<>(context);
  }

  private boolean enoughContextAdded() {
    if (trailingItemCount == null) {
      return false;
    } else {
      trailingItemCount = trailingItemCount - 1;
      return trailingItemCount < 0 && context.size() >= CONTEXT_SIZE;
    }
  }
}
