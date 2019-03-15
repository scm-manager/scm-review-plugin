package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.emailnotification.EmailNotificationHook;
import com.cloudogu.scm.review.emailnotification.EmailNotificationService;
import com.cloudogu.scm.review.emailnotification.Notification;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
class EmailNotificationHookTest {


  @Mock
  EmailNotificationService service;

  @InjectMocks
  EmailNotificationHook emailNotificationHook ;
  private PullRequest pullRequest;
  private ArrayList<String> subscriber;
  private Repository repository;
  private PullRequest oldPullRequest;
  private PullRequestComment comment;
  private PullRequestComment oldComment;

  @BeforeEach
  void setUp() {
    pullRequest = TestData.createPullRequest();
    subscriber = Lists.newArrayList("email1@d.de", "email1@d.de");
    pullRequest.setSubscriber(subscriber);
    repository = createHeartOfGold();
    oldPullRequest = TestData.createPullRequest();
    oldPullRequest.setTitle("new Title");
    comment = TestData.createComment();
    oldComment = TestData.createComment();
  }

  @Test
  void shouldSendEmailsAfterCreatingPullRequest(){
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, null, HandlerEventType.CREATE);

    emailNotificationHook.handlePullRequestEvents(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getReceivers())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new String[2]));
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getOldPullRequest()).isNull();
      return true;
    }), eq(Notification.CREATED_PULL_REQUEST));
  }

  @Test
  void shouldSendEmailsAfterModifyingPullRequest(){
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.MODIFY);

    emailNotificationHook.handlePullRequestEvents(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getReceivers())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new String[2]));
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      assertThat(emailContext.getOldPullRequest()).isEqualToComparingFieldByFieldRecursively(oldPullRequest);
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      return true;
    }), eq(Notification.MODIFIED_PULL_REQUEST));
  }

  @Test
  void shouldSendEmailsAfterDeletingPullRequest(){
    PullRequestEvent event = new PullRequestEvent(repository, null, pullRequest , HandlerEventType.DELETE);

    emailNotificationHook.handlePullRequestEvents(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getReceivers())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new String[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getOldPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      assertThat(emailContext.getPullRequest()).isNull();
      return true;
    }), eq(Notification.DELETED_PULL_REQUEST));
  }

  @Test
  void shouldSendEmailsAfterCreatingComment(){
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, null,HandlerEventType.CREATE);

    emailNotificationHook.handleCommentEvents(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getReceivers())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new String[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getComment()).isEqualToComparingFieldByFieldRecursively(comment);
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      assertThat(emailContext.getOldComment()).isNull();
      return true;
    }), eq(Notification.CREATED_COMMENT));
  }

  @Test
  void shouldSendEmailsAfterModifyingComment(){
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.MODIFY);

    emailNotificationHook.handleCommentEvents(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getReceivers())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new String[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getComment()).isEqualToComparingFieldByFieldRecursively(comment);
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      assertThat(emailContext.getOldComment()).isEqualToComparingFieldByFieldRecursively(oldComment);
      return true;
    }), eq(Notification.MODIFIED_COMMENT));
  }

  @Test
  void shouldSendEmailsAfterDeletingComment(){
    CommentEvent event = new CommentEvent(repository, pullRequest, null, oldComment, HandlerEventType.DELETE);

    emailNotificationHook.handleCommentEvents(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getReceivers())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new String[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getComment()).isNull();
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      assertThat(emailContext.getOldComment()).isEqualToComparingFieldByFieldRecursively(oldComment);
      return true;
    }), eq(Notification.DELETED_COMMENT));
  }

  @Test
  void shouldSendEmailsAfterMergingPullRequest(){
    PullRequestMergedEvent event = new PullRequestMergedEvent(repository, pullRequest);

    emailNotificationHook.handleMergedPullRequest(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getReceivers())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new String[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      return true;
    }), eq(Notification.MERGED_PULL_REQUEST));
  }

  @Test
  void shouldSendEmailsAfterRejectingPullRequest(){
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest);

    emailNotificationHook.handleRejectedPullRequest(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getReceivers())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new String[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      return true;
    }), eq(Notification.REJECTED_PULL_REQUEST));
  }

}
