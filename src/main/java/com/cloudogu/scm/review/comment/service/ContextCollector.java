/**
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
