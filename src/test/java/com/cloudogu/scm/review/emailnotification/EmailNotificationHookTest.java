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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;

import java.util.Set;

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
  EmailNotificationHook emailNotificationHook;
  private PullRequest pullRequest;
  private Set<Recipient> subscriber;
  private Repository repository;
  private PullRequest oldPullRequest;
  private PullRequestComment comment;
  private PullRequestComment oldComment;

  @BeforeEach
  void setUp() {
    pullRequest = TestData.createPullRequest();

    Recipient recipient1 = new Recipient("user1", "email1@d.de");
    Recipient recipient2 = new Recipient("user2", "email1@d.de");
    subscriber = Sets.newHashSet(Lists.newArrayList(recipient1, recipient2));
    pullRequest.setSubscriber(subscriber);
    repository = createHeartOfGold();
    oldPullRequest = TestData.createPullRequest();
    oldPullRequest.setTitle("old Title");
    oldPullRequest.setDescription("old Description");
    comment = TestData.createComment();
    oldComment = TestData.createComment();
    oldComment.setComment("this is my old comment");
    comment.setComment("this is my modified comment");
  }


  @Test
  void shouldSendEmailsAfterModifyingPullRequest() {
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.MODIFY);

    emailNotificationHook.handlePullRequestEvents(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getRecipients())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new Recipient[2]));
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      assertThat(emailContext.getOldPullRequest()).isEqualToComparingFieldByFieldRecursively(oldPullRequest);
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      return true;
    }), eq(Notification.MODIFIED_PULL_REQUEST));
  }

  @Test
  void shouldSendEmailsAfterCreatingComment() {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, null, HandlerEventType.CREATE);

    emailNotificationHook.handleCommentEvents(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getRecipients())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new Recipient[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getComment()).isEqualToComparingFieldByFieldRecursively(comment);
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      assertThat(emailContext.getOldComment()).isNull();
      return true;
    }), eq(Notification.CREATED_COMMENT));
  }

  @Test
  void shouldSendEmailsAfterModifyingComment() {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.MODIFY);

    emailNotificationHook.handleCommentEvents(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getRecipients())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new Recipient[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getComment()).isEqualToComparingFieldByFieldRecursively(comment);
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      assertThat(emailContext.getOldComment()).isEqualToComparingFieldByFieldRecursively(oldComment);
      return true;
    }), eq(Notification.MODIFIED_COMMENT));
  }

  @Test
  void shouldSendEmailsAfterDeletingComment() {
    CommentEvent event = new CommentEvent(repository, pullRequest, null, oldComment, HandlerEventType.DELETE);

    emailNotificationHook.handleCommentEvents(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getRecipients())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new Recipient[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getComment()).isNull();
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      assertThat(emailContext.getOldComment()).isEqualToComparingFieldByFieldRecursively(oldComment);
      return true;
    }), eq(Notification.DELETED_COMMENT));
  }

  @Test
  void shouldSendEmailsAfterMergingPullRequest() {
    PullRequestMergedEvent event = new PullRequestMergedEvent(repository, pullRequest);

    emailNotificationHook.handleMergedPullRequest(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getRecipients())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new Recipient[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      return true;
    }), eq(Notification.MERGED_PULL_REQUEST));
  }

  @Test
  void shouldSendEmailsAfterRejectingPullRequest() {
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest);

    emailNotificationHook.handleRejectedPullRequest(event);

    verify(service).sendEmail(argThat(emailContext -> {
      assertThat(emailContext.getRecipients())
        .hasSize(2)
        .containsExactlyInAnyOrder(subscriber.toArray(new Recipient[2]));
      assertThat(emailContext.getRepository()).isEqualToComparingFieldByFieldRecursively(repository);
      assertThat(emailContext.getPullRequest()).isEqualToComparingFieldByFieldRecursively(pullRequest);
      return true;
    }), eq(Notification.REJECTED_PULL_REQUEST));
  }

}
