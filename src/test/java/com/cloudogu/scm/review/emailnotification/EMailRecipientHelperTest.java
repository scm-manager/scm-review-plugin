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
