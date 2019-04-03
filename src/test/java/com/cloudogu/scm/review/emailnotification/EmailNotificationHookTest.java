package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.Recipient;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
class EmailNotificationHookTest {


  @Mock
  EmailNotificationService service;

  @InjectMocks
  EmailNotificationHook emailNotificationHook;
  private PullRequest pullRequest;
  private Set<Recipient> subscriber;
  private Repository repository;
  private PullRequest oldPullRequest;
  private PullRequestComment comment;
  private PullRequestComment oldComment;
  private HashSet<Recipient> reviewers;

  @BeforeEach
  void setUp() {
    pullRequest = TestData.createPullRequest();

    Recipient recipient1 = new Recipient("user1", "email1@d.de");
    Recipient recipient2 = new Recipient("user2", "email1@d.de");
    subscriber = Sets.newHashSet(Lists.newArrayList(recipient1, recipient2));
    pullRequest.setSubscriber(subscriber);
    Recipient recipient3 = new Recipient("user3", "email1@d.de");
    Recipient recipient4 = new Recipient("user4", "email1@d.de");
    reviewers = Sets.newHashSet(Lists.newArrayList(recipient3, recipient4));
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
      DynamicTest.dynamicTest(event.toString(), () -> {
        emailNotificationHook.handleCommentEvents(event);

        verify(service).sendEmails(isA(CommentEventEmailRenderer.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer()));
        reset(service);
      })
    );
  }

  @Test
  void shouldNotSendSystemEmails() throws IOException, MailSendBatchException {
    comment.setSystemComment(true);
    CommentEvent commentEvent = new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.CREATE);
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

        verify(service).sendEmails(isA(PullRequestEventEmailRenderer.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer()));
        reset(service);
      })
    );
  }

  @Test
  void shouldSendEmailsAfterMergingPullRequest() throws IOException, MailSendBatchException {
    PullRequestMergedEvent event = new PullRequestMergedEvent(repository, pullRequest);
    emailNotificationHook.handleMergedPullRequest(event);

    verify(service).sendEmails(isA(PullRequestMergedEmailRenderer.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer()));
  }

  @Test
  void shouldSendEmailsAfterRejectingPullRequest() throws IOException, MailSendBatchException {
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest);
    emailNotificationHook.handleRejectedPullRequest(event);

    verify(service).sendEmails(isA(PullRequestRejectedEmailRenderer.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer()));
  }

}
