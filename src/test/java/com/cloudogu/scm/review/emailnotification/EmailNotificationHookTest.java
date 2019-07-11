package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.BasicComment;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
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
import sonia.scm.repository.Repository;

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
  private Set<String> subscriber;
  private Repository repository;
  private PullRequest oldPullRequest;
  private Comment comment;
  private Comment oldComment;
  private HashSet<String> reviewers;

  @BeforeEach
  void setUp() {
    pullRequest = TestData.createPullRequest();

    String recipient1 = "user1";
    String recipient2 = "user2";
    subscriber = Sets.newHashSet(Lists.newArrayList(recipient1, recipient2));
    pullRequest.setSubscriber(subscriber);
    String recipient3 = "user3";
    String recipient4 = "user4";
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
      DynamicTest.dynamicTest(event.getEventType().toString(), () -> {
        emailNotificationHook.handleCommentEvents(event);

        verify(service).sendEmails(isA(CommentEventMailTextResolver.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer()));
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

        verify(service).sendEmails(isA(PullRequestEventMailTextResolver.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer()));
        reset(service);
      })
    );
  }

  @Test
  void shouldSendEmailsAfterMergingPullRequest() throws Exception {
    PullRequestMergedEvent event = new PullRequestMergedEvent(repository, pullRequest);
    emailNotificationHook.handleMergedPullRequest(event);

    verify(service).sendEmails(isA(PullRequestMergedMailTextResolver.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer()));
  }

  @Test
  void shouldSendEmailsAfterRejectingPullRequest() throws Exception {
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest);
    emailNotificationHook.handleRejectedPullRequest(event);

    verify(service).sendEmails(isA(PullRequestRejectedMailTextResolver.class), eq(pullRequest.getSubscriber()), eq(pullRequest.getReviewer()));
  }

}
