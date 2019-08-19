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
