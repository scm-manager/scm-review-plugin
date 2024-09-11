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

package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.apache.shiro.SecurityUtils;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableSet;

class EMailRecipientHelper {

  private final Set<String> subscribingReviewers;
  private final Set<String> subscriberWithoutReviewers;

  EMailRecipientHelper(PullRequest pullRequest) {
    Set<String> subscriber = pullRequest.getSubscriber();
    Set<String> reviewer = pullRequest.getReviewer().keySet();

    Object currentUser = SecurityUtils.getSubject().getPrincipal();

    subscriberWithoutReviewers = unmodifiableSet(subscriber.stream()
      .filter(recipient -> !reviewer.contains(recipient))
      .filter(recipient -> !recipient.equals(currentUser))
      .collect(Collectors.toSet()));

    subscribingReviewers = unmodifiableSet(subscriber.stream()
      .filter(reviewer::contains)
      .filter(recipient -> !recipient.equals(currentUser))
      .collect(Collectors.toSet()));
  }

  Set<String> getSubscribingReviewers() {
    return subscribingReviewers;
  }

  Set<String> getSubscriberWithoutReviewers() {
    return subscriberWithoutReviewers;
  }
}
