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

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.ExecutedTransition;
import com.cloudogu.scm.review.emailnotification.PullRequestStatusChangedMailTextResolver.PullRequestStatusType;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.common.collect.Lists;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;
import sonia.scm.template.Template;
import sonia.scm.template.TemplateEngine;
import sonia.scm.template.TemplateType;

import java.io.Reader;
import java.net.URL;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
class MailTextResolverTest {

  private PullRequest pullRequest;
  private Set<String> subscriber;
  private Repository repository;
  private PullRequest oldPullRequest;
  private Comment comment;
  private Comment oldComment;

  private final Subject subject = mock(Subject.class);

  @BeforeEach
  void setUp() {
    ThreadContext.bind(subject);
    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    when(subject.isPermitted(any(String.class))).thenReturn(true);

    pullRequest = TestData.createPullRequest();
    String recipient1 = "user1";
    String recipient2 = "user2";
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


  private TemplateEngine createEngine() {
    return new TemplateEngine() {
      @Override
      public Template getTemplate(String templatePath) {
        return (writer, model) -> {
          URL resource = EmailNotificationService.class.getClassLoader().getResource(templatePath);
          assertThat(resource).isNotNull();
          assertThat(resource.getPath()).isNotNull();
          Mustache mustache = new DefaultMustacheFactory().compile(resource.getPath());
          mustache.execute(writer, model);
        };
      }

      @Override
      public Template getTemplate(String templateIdentifier, Reader reader) {
        return null;
      }

      @Override
      public TemplateType getType() {
        return null;
      }
    };
  }

  @Test
  void shouldRenderEmailOnModifiedPullRequest() {
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.MODIFY);

    PullRequestEventMailTextResolver renderer = new PullRequestEventMailTextResolver(event, false);

    assertEmail(renderer, "changed");
  }

  @Test
  void shouldRenderEmailOnCreatedPullRequest() {
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.CREATE);

    PullRequestEventMailTextResolver renderer = new PullRequestEventMailTextResolver(event, false);

    assertEmail(renderer, "created");
  }

  @Test
  void shouldNotRenderReviewerEmailOnCreatedPullRequest() {
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.CREATE);

    PullRequestEventMailTextResolver renderer = new PullRequestEventMailTextResolver(event, false);

    assertEmail(renderer, "created")
    .doesNotContain("You are chosen as reviewer for this pull request.");
  }

  @Test
  void shouldRenderEmailOnCreatedComment() {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.CREATE);

    CommentEventMailTextResolver renderer = new CommentEventMailTextResolver(event);

    assertEmail(renderer, "added");
  }

  @Test
  void shouldRenderEmailOnCommentTransition() {
    Comment taskComment = oldComment.clone();
    taskComment.addCommentTransition(new ExecutedTransition<>("new", CommentTransition.MAKE_TASK, System.currentTimeMillis(), "dent"));
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.MODIFY);

    CommentEventMailTextResolver renderer = new CommentEventMailTextResolver(event);

    assertEmail(renderer, "changed");
  }

  @Test
  void shouldRenderEmailOnModifiedComment() {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.MODIFY);

    CommentEventMailTextResolver renderer = new CommentEventMailTextResolver(event);

    assertEmail(renderer, "changed");
  }

  @Test
  void shouldRenderEmailOnDeletedComment() {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.DELETE);

    CommentEventMailTextResolver renderer = new CommentEventMailTextResolver(event);

    assertEmail(renderer, "deleted");
  }

  @Test
  void shouldRenderEmailOnMergedPullRequest() {
    PullRequestMergedEvent event = new PullRequestMergedEvent(repository, pullRequest);

    PullRequestMergedMailTextResolver renderer = new PullRequestMergedMailTextResolver(event);

    assertEmail(renderer, "merged");
  }

  @Test
  void shouldRenderEmailOnRejectedPullRequest() {
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER);

    PullRequestRejectedMailTextResolver renderer = new PullRequestRejectedMailTextResolver(event);

    assertEmail(renderer, "rejected");
  }

  @Test
  void shouldRenderEmailOnPullRequestChangedToDraft() {
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.MODIFY);

    PullRequestStatusChangedMailTextResolver renderer = new PullRequestStatusChangedMailTextResolver(event, PullRequestStatusType.TO_DRAFT, true);

    assertEmail(renderer, "converted to draft");
  }

  @Test
  void shouldRenderEmailOnPullRequestChangedToOpen() {
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.CREATE);

    PullRequestStatusChangedMailTextResolver renderer = new PullRequestStatusChangedMailTextResolver(event, PullRequestStatusType.TO_OPEN, true);

    assertEmail(renderer, "opened for review");
  }

  private AbstractCharSequenceAssert<?, String> assertEmail(MailTextResolver renderer, String event) {
    String mailSubject = renderer.getMailSubject(Locale.ENGLISH);
    return assertThat(mailSubject).isNotEmpty()
      .contains(repository.getName(), repository.getNamespace(), pullRequest.getId(), pullRequest.getTitle(), event);
  }
}
