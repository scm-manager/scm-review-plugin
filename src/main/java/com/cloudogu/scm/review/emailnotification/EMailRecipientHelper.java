/*
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
