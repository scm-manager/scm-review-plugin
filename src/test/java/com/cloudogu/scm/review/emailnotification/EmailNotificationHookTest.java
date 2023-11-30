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
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestApprovalEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestReopenedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestUpdatedMailEvent;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.repository.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.cloudogu.scm.review.emailnotification.MailTextResolver.TOPIC_PR_CHANGED;
import static com.cloudogu.scm.review.emailnotification.MailTextResolver.TOPIC_PR_UPDATED;
import static com.google.common.collect.ImmutableSet.of;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
class EmailNotificationHookTest {

  String currentUser = "current-user";
  String subscribedButNotReviewer = "subscribed-but-not-reviewer";
  String subscribedAndReviewer = "subscribed-and-reviewer";
  String reviewerButNotSubscribed= "reviewer-but-not-subscribed";

  @Mock
  EmailNotificationService service;

  @InjectMocks
  EmailNotificationHook emailNotificationHook;

  private PullRequest pullRequest;
  private Repository repository;
  private PullRequest oldPullRequest;
  private Comment comment;
  private Comment oldComment;
  private final Map<String, Boolean> reviewers = new HashMap<>();

  @BeforeEach
  void setUp() {
    pullRequest = TestData.createPullRequest();
    Set<String> subscriber = of(currentUser, subscribedButNotReviewer, subscribedAndReviewer);
    pullRequest.setSubscriber(subscriber);

    reviewers.put(subscribedAndReviewer, Boolean.FALSE);
    reviewers.put(reviewerButNotSubscribed, Boolean.TRUE);
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
      new CommentEvent(repository, pullRequest, comment, null, HandlerEventType.CREATE),
      new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.MODIFY),
      new CommentEvent(repository, pullRequest, null, oldComment, HandlerEventType.DELETE)
    );
    return events.stream().map(event ->
      DynamicTest.dynamicTest(event.getEventType().toString(), () -> {
        emailNotificationHook.handleCommentEvents(event);

        verify(service).sendEmail(eq(of(subscribedButNotReviewer, subscribedAndReviewer)), isA(CommentEventMailTextResolver.class));
        reset(service);
      })
    );
  }

  @TestFactory
  Stream<DynamicTest> sendingReplyEmailTestFactory() {
    Reply reply = Reply.createReply("1", "42", currentUser);
    Reply oldReply = Reply.createReply("1", "have to think", currentUser);
    comment.setAuthor("first author");
    comment.setReplies(asList(reply, Reply.createReply("0", "dumb question", "former participant")));
    ArrayList<ReplyEvent> events = Lists.newArrayList(
      new ReplyEvent(repository, pullRequest, reply, null, comment, HandlerEventType.CREATE),
      new ReplyEvent(repository, pullRequest, reply, oldReply, comment, HandlerEventType.MODIFY),
      new ReplyEvent(repository, pullRequest, null, oldReply, comment, HandlerEventType.DELETE)
    );
    return events.stream().map(event ->
      DynamicTest.dynamicTest(event.getEventType().toString(), () -> {
        emailNotificationHook.handleReplyEvents(event);

        verify(service).sendEmail(eq(of(subscribedButNotReviewer, subscribedAndReviewer)), isA(CommentEventMailTextResolver.class));
        verify(service).sendEmail(eq(of("first author", "former participant")), isA(ReplyEventMailTextResolver.class));
        reset(service);
      })
    );
  }

  @Test
  void shouldNotSendSystemEmails() throws Exception {
    Comment systemComment = Comment.createSystemComment("1");
    CommentEvent commentEvent = new CommentEvent(repository, pullRequest, systemComment, oldComment, HandlerEventType.CREATE);
    emailNotificationHook.handleCommentEvents(commentEvent);

    verify(service, never()).sendEmail(any(), any());
    verify(service, never()).sendEmail(any(), any());
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

        verify(service).sendEmail(eq(of(subscribedButNotReviewer)), isA(PullRequestEventMailTextResolver.class));
        verify(service).sendEmail(eq(of(subscribedAndReviewer)), isA(PullRequestEventMailTextResolver.class));
        reset(service);
      })
    );
  }

  @Test
  void shouldSendEmailsAfterUpdatingPullRequest() throws Exception {
    PullRequestUpdatedMailEvent event = new PullRequestUpdatedMailEvent(repository, pullRequest);
    emailNotificationHook.handleUpdatedPullRequest(event);

    ArgumentCaptor<PullRequestUpdatedMailTextResolver> captor = ArgumentCaptor.forClass(PullRequestUpdatedMailTextResolver.class);
    verify(service).sendEmail(eq(of(subscribedButNotReviewer, subscribedAndReviewer)), captor.capture());

    PullRequestUpdatedMailTextResolver resolver = captor.getValue();
    assertThat(resolver.getMailSubject(Locale.ENGLISH)).contains("PR updated");
    assertThat(resolver.getTopic()).isEqualTo(TOPIC_PR_UPDATED);
    assertThat(resolver.getContentTemplatePath()).contains("updated_pull_request");
  }

  @Test
  void shouldSendEmailsAfterUpdatingDraftToPullRequest() throws Exception {
    PullRequest oldPullRequest = pullRequest.toBuilder().status(PullRequestStatus.DRAFT).build();
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.MODIFY);
    emailNotificationHook.handlePullRequestEvents(event);

    ArgumentCaptor<PullRequestStatusChangedMailTextResolver> captor = ArgumentCaptor.forClass(PullRequestStatusChangedMailTextResolver.class);
    verify(service).sendEmail(eq(of(subscribedAndReviewer)), captor.capture());
    PullRequestStatusChangedMailTextResolver resolver = captor.getValue();
    assertThat(resolver.getMailSubject(Locale.ENGLISH)).contains("PR opened for review");
    assertThat(resolver.getTopic()).isEqualTo(TOPIC_PR_CHANGED);
  }

  @Test
  void shouldSendEmailsAfterChangingPullRequestToDraft() throws Exception {
    PullRequest oldPullRequest = pullRequest.toBuilder().build();
    PullRequest draftPullRequest = pullRequest.toBuilder().status(PullRequestStatus.DRAFT).build();
    PullRequestEvent event = new PullRequestEvent(repository, draftPullRequest, oldPullRequest, HandlerEventType.MODIFY);
    emailNotificationHook.handlePullRequestEvents(event);

    ArgumentCaptor<PullRequestStatusChangedMailTextResolver> captor = ArgumentCaptor.forClass(PullRequestStatusChangedMailTextResolver.class);
    verify(service).sendEmail(eq(of(subscribedAndReviewer)), captor.capture());
    PullRequestStatusChangedMailTextResolver resolver = captor.getValue();
    assertThat(resolver.getMailSubject(Locale.ENGLISH)).contains("PR converted to draft");
    assertThat(resolver.getTopic()).isEqualTo(TOPIC_PR_CHANGED);
  }

  @Test
  void shouldSendEmailsAfterMergingPullRequest() throws Exception {
    PullRequestMergedEvent event = new PullRequestMergedEvent(repository, pullRequest);
    emailNotificationHook.handleMergedPullRequest(event);

    verify(service).sendEmail(eq(of(subscribedButNotReviewer, subscribedAndReviewer)), isA(PullRequestMergedMailTextResolver.class));
  }

  @Test
  void shouldSendEmailsAfterRejectingPullRequest() throws Exception {
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER);
    emailNotificationHook.handleRejectedPullRequest(event);

    verify(service).sendEmail(eq(of(subscribedButNotReviewer, subscribedAndReviewer)), isA(PullRequestRejectedMailTextResolver.class));
  }

  @Test
  void shouldSendEmailsAfterReopeningPullRequest() throws Exception {
    PullRequestReopenedEvent event = new PullRequestReopenedEvent(repository, pullRequest);
    emailNotificationHook.handleReopenedPullRequest(event);

    verify(service).sendEmail(eq(of(subscribedButNotReviewer, subscribedAndReviewer)), isA(PullRequestReopenedMailTextResolver.class));
  }

  @Test
  void shouldSendEmailAfterPullRequestGotApproved() throws MailSendBatchException {
    PullRequestApprovalEvent event = new PullRequestApprovalEvent(repository, pullRequest, PullRequestApprovalEvent.ApprovalCause.APPROVED);
    emailNotificationHook.handlePullRequestApproval(event);

    verify(service).sendEmail(eq(of(subscribedButNotReviewer, subscribedAndReviewer)), isA(PullRequestApprovalMailTextResolver.class));
  }

  @Test
  void shouldSendEmailAfterPullRequestGotApprovalRemoved() throws MailSendBatchException {
    PullRequestApprovalEvent event = new PullRequestApprovalEvent(repository, pullRequest, PullRequestApprovalEvent.ApprovalCause.APPROVAL_REMOVED);
    emailNotificationHook.handlePullRequestApproval(event);

    verify(service).sendEmail(eq(of(subscribedButNotReviewer, subscribedAndReviewer)), isA(PullRequestApprovalMailTextResolver.class));
  }

  @Test
  void shouldSendEmailAfterUserGotMentionedInComment() throws MailSendBatchException {
    comment.setComment("@[_anonymous] But why? @[scmadmin]");
    comment.setMentionUserIds(of("scmadmin", "_anonymous"));
    oldComment.setMentionUserIds(Collections.emptySet());

    ImmutableSet<String> newMentions = of("scmadmin", "_anonymous");

    MentionEvent event = new MentionEvent(repository, pullRequest, comment, oldComment, HandlerEventType.CREATE);
    emailNotificationHook.handleMentionEvents(event);

    verify(service).sendEmail(eq(newMentions), isA(MentionEventMailTextResolver.class));
  }

  @Test
  void shouldNotSendEmailIfUserWasAlreadyMentionedOnEditingComment() throws MailSendBatchException {
    oldComment.setMentionUserIds(of("scmadmin", "_anonymous"));
    comment.setComment("@[_anonymous] But why? @[scmadmin]");
    comment.setMentionUserIds(of("scmadmin", "_anonymous"));
    oldComment.setMentionUserIds(Collections.emptySet());

    MentionEvent event = new MentionEvent(repository, pullRequest, comment, oldComment, HandlerEventType.CREATE);
    emailNotificationHook.handleMentionEvents(event);

    verify(service).sendEmail(eq(Collections.emptySet()), isA(MentionEventMailTextResolver.class));
  }

  @Test
  void shouldSendEmailOnlyToNewMentionsOnEditingComment() throws MailSendBatchException {
    oldComment.setMentionUserIds(of("scmadmin"));
    comment.setComment("@[_anonymous] But why? @[scmadmin]");
    comment.setMentionUserIds(of("scmadmin", "_anonymous"));

    ImmutableSet<String> newMentions = of("_anonymous");

    MentionEvent event = new MentionEvent(repository, pullRequest, comment, oldComment, HandlerEventType.CREATE);
    emailNotificationHook.handleMentionEvents(event);

    verify(service).sendEmail(eq(newMentions), isA(MentionEventMailTextResolver.class));
    verify(service).sendEmail(eq(Collections.emptySet()), isA(MentionEventMailTextResolver.class));
  }

  @Test
  void shouldNotSendEmailsAfterRejectingDraft() throws Exception {
    pullRequest.setStatus(PullRequestStatus.DRAFT);
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER);
    emailNotificationHook.handleRejectedPullRequest(event);

    verify(service, never()).sendEmail(eq(of(subscribedButNotReviewer, subscribedAndReviewer)), isA(PullRequestRejectedMailTextResolver.class));
  }

  @BeforeEach
  void mockCurrentUser() {
    Subject subject = mock(Subject.class);
    lenient().when(subject.getPrincipal()).thenReturn(currentUser);
    ThreadContext.bind(subject);
  }

  @AfterEach
  void cleanupThreadContext() {
    ThreadContext.unbindSubject();
  }
}
