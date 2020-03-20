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

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.MentionEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestApprovalEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.repository.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
class EmailNotificationHookTest {


  @Mock
  EmailNotificationService service;

  @InjectMocks
  EmailNotificationHook emailNotificationHook;
  private PullRequest pullRequest;
  private Set<String> subscriber;
  private Repository repository;
  private PullRequest oldPullRequest;
  private Comment comment;
  private Comment oldComment;
  private Map<String, Boolean> reviewers = new HashMap<>();

  @BeforeEach
  void setUp() {
    pullRequest = TestData.createPullRequest();

    String recipient1 = "user1";
    String recipient2 = "user2";
    subscriber = Sets.newHashSet(Lists.newArrayList(recipient1, recipient2));
    pullRequest.setSubscriber(subscriber);
    String recipient3 = "user3";
    String recipient4 = "user4";
    reviewers.put(recipient3, Boolean.FALSE);
    reviewers.put(recipient4, Boolean.TRUE);
    pullRequest.setReviewer(reviewers);
    repository = createHeartOfGold();
    oldPullRequest = TestData.createPullRequest();
    oldPullRequest.setTitle("old Title");
    oldPullRequest.setDescription("old Description");
    comment = TestData.createComment();
    oldComment = TestData.createComment();
    oldComment.setComment("this is my old comment");
    comment.setComment("this is my modified comment");
  }

  @TestFactory
  Stream<DynamicTest> sendingCommentEmailTestFactory() {
    ArrayList<CommentEvent> events = Lists.newArrayList(
      new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.CREATE),
      new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.MODIFY),
      new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.DELETE)
    );
    return events.stream().map(event ->
      DynamicTest.dynamicTest(event.getEventType().toString(), () -> {
        emailNotificationHook.handleCommentEvents(event);

        verify(service).sendEmails(isA(CommentEventMailTextResolver.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer().keySet()));
        reset(service);
      })
    );
  }

  @Test
  void shouldNotSendSystemEmails() throws Exception {
    Comment systemComment = Comment.createSystemComment("1");
    CommentEvent commentEvent = new CommentEvent(repository, pullRequest, systemComment, oldComment, HandlerEventType.CREATE);
    emailNotificationHook.handleCommentEvents(commentEvent);

    verify(service, never()).sendEmails(any(), any(), any());
    reset(service);
  }

  @TestFactory
  Stream<DynamicTest> sendingPREmailTestFactory() {
    ArrayList<PullRequestEvent> events = Lists.newArrayList(
      new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.CREATE),
      new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.MODIFY)
    );
    return events.stream().map(event ->
      DynamicTest.dynamicTest(event.toString(), () -> {
        emailNotificationHook.handlePullRequestEvents(event);

        verify(service).sendEmails(isA(PullRequestEventMailTextResolver.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer().keySet()));
        reset(service);
      })
    );
  }

  @Test
  void shouldSendEmailsAfterMergingPullRequest() throws Exception {
    PullRequestMergedEvent event = new PullRequestMergedEvent(repository, pullRequest);
    emailNotificationHook.handleMergedPullRequest(event);

    verify(service).sendEmails(isA(PullRequestMergedMailTextResolver.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer().keySet()));
  }

  @Test
  void shouldSendEmailsAfterRejectingPullRequest() throws Exception {
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER);
    emailNotificationHook.handleRejectedPullRequest(event);

    verify(service).sendEmails(isA(PullRequestRejectedMailTextResolver.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer().keySet()));
  }

  @Test
  void shouldSendEmailAfterPullRequestGotApproved() throws MailSendBatchException {
    PullRequestApprovalEvent event = new PullRequestApprovalEvent(repository, pullRequest, PullRequestApprovalEvent.ApprovalCause.APPROVED);
    emailNotificationHook.handlePullRequestApproval(event);

    verify(service).sendEmails(isA(PullRequestApprovalMailTextResolver.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer().keySet()));
  }

  @Test
  void shouldSendEmailAfterPullRequestGotApprovalRemoved() throws MailSendBatchException {
    PullRequestApprovalEvent event = new PullRequestApprovalEvent(repository, pullRequest, PullRequestApprovalEvent.ApprovalCause.APPROVAL_REMOVED);
    emailNotificationHook.handlePullRequestApproval(event);

    verify(service).sendEmails(isA(PullRequestApprovalMailTextResolver.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer().keySet()));
  }

  @Test
  void shouldSendEmailAfterUserGotMentionedInComment() throws MailSendBatchException {
    comment.setComment("@[_anonymous] But why? @[scmadmin]");
    comment.setMentionUserIds(ImmutableSet.of("scmadmin", "_anonymous"));
    oldComment.setMentionUserIds(Collections.emptySet());

    ImmutableSet<String> newMentions = ImmutableSet.of("scmadmin", "_anonymous");

    MentionEvent event = new MentionEvent(repository, pullRequest, comment, oldComment, HandlerEventType.CREATE);
    emailNotificationHook.handleMentionEvents(event);

    verify(service).sendEmails(isA(MentionEventMailTextResolver.class), eq(newMentions), eq(Collections.emptySet()));
  }

  @Test
  void shouldNotSendEmailIfUserWasAlreadyMentionedOnEditingComment() throws MailSendBatchException {
    oldComment.setMentionUserIds(ImmutableSet.of("scmadmin", "_anonymous"));
    comment.setComment("@[_anonymous] But why? @[scmadmin]");
    comment.setMentionUserIds(ImmutableSet.of("scmadmin", "_anonymous"));
    oldComment.setMentionUserIds(Collections.emptySet());

    MentionEvent event = new MentionEvent(repository, pullRequest, comment, oldComment, HandlerEventType.CREATE);
    emailNotificationHook.handleMentionEvents(event);

    verify(service, never()).sendEmails(isA(MentionEventMailTextResolver.class), eq(Collections.emptySet()), eq(Collections.emptySet()));
  }

  @Test
  void shouldSendEmailOnlyToNewMentionsOnEditingComment() throws MailSendBatchException {
    oldComment.setMentionUserIds(ImmutableSet.of("scmadmin"));
    comment.setComment("@[_anonymous] But why? @[scmadmin]");
    comment.setMentionUserIds(ImmutableSet.of("scmadmin", "_anonymous"));

    ImmutableSet<String> newMentions = ImmutableSet.of("_anonymous");

    MentionEvent event = new MentionEvent(repository, pullRequest, comment, oldComment, HandlerEventType.CREATE);
    emailNotificationHook.handleMentionEvents(event);

    verify(service).sendEmails(isA(MentionEventMailTextResolver.class), eq(newMentions), eq(Collections.emptySet()));
  }
}
