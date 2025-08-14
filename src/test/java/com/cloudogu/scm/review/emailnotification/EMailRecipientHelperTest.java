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
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EMailRecipientHelperTest {

  PullRequest pullRequest = new PullRequest("1", "feature", "develop");

  @BeforeEach
  void mockCurrentUser() {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("ford");
    ThreadContext.bind(subject);
  }

  @AfterEach
  void cleanupThreadContext() {
    ThreadContext.unbindSubject();
  }

  @Test
  void withoutReviewersShouldReturnAllSubscribers() {
    pullRequest.setSubscriber(of("dent", "trillian"));

    EMailRecipientHelper helper = new EMailRecipientHelper(pullRequest);

    assertThat(helper.getSubscriberWithoutReviewers()).containsExactly("dent", "trillian");
    assertThat(helper.getSubscribingReviewers()).isEmpty();
  }

  @Test
  void shouldSeparateSimpleSubscribersFromReviewers() {
    pullRequest.setSubscriber(of("dent", "trillian"));
    pullRequest.setReviewer(singletonMap("trillian", true));

    EMailRecipientHelper helper = new EMailRecipientHelper(pullRequest);

    assertThat(helper.getSubscriberWithoutReviewers()).containsExactly("dent");
    assertThat(helper.getSubscribingReviewers()).containsExactly("trillian");
  }

  @Test
  void shouldRemoveCurrentUserFromSubscribers() {
    pullRequest.setSubscriber(of("ford", "trillian"));

    EMailRecipientHelper helper = new EMailRecipientHelper(pullRequest);

    assertThat(helper.getSubscriberWithoutReviewers()).containsExactly("trillian");
    assertThat(helper.getSubscribingReviewers()).isEmpty();
  }

  @Test
  void shouldRemoveCurrentUserFromReviewers() {
    pullRequest.setSubscriber(of("dent", "ford"));
    pullRequest.setReviewer(singletonMap("ford", true));

    EMailRecipientHelper helper = new EMailRecipientHelper(pullRequest);

    assertThat(helper.getSubscriberWithoutReviewers()).containsExactly("dent");
    assertThat(helper.getSubscribingReviewers()).isEmpty();
  }

  @Test
  void shouldReviewersThatHaveNotSubscribed() {
    pullRequest.setSubscriber(of("dent"));
    pullRequest.setReviewer(singletonMap("trillian", true));

    EMailRecipientHelper helper = new EMailRecipientHelper(pullRequest);

    assertThat(helper.getSubscriberWithoutReviewers()).containsExactly("dent");
    assertThat(helper.getSubscribingReviewers()).isEmpty();
  }
}
