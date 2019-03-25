package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.Recipient;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.common.collect.Lists;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;
import sonia.scm.template.Template;
import sonia.scm.template.TemplateEngine;
import sonia.scm.template.TemplateEngineFactory;
import sonia.scm.template.TemplateType;
import sonia.scm.user.User;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
class EmailRendererTest {


  private PullRequest pullRequest;
  private Set<Recipient> subscriber;
  private Repository repository;
  private PullRequest oldPullRequest;
  private PullRequestComment comment;
  private PullRequestComment oldComment;

  private final Subject subject = mock(Subject.class);

  @BeforeEach
  void setUp() {
    ThreadContext.bind(subject);
    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principals);
    when(subject.isPermitted(any(String.class))).thenReturn(true);
    String currentUser = "username";
    when(principals.getPrimaryPrincipal()).thenReturn(currentUser);
    User user1 = new User();
    user1.setName("user1");
    user1.setDisplayName("User 1");
    when(principals.oneByType(User.class)).thenReturn(user1);

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
  void shouldRenderEmailOnModifiedPullRequest() throws IOException {
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.MODIFY);

    PullRequestEventEmailRenderer renderer = new PullRequestEventEmailRenderer(event);

    assertEmail(renderer, "modified");
  }

  @Test
  void shouldRenderEmailOnCreatedPullRequest() throws IOException {
    PullRequestEvent event = new PullRequestEvent(repository, pullRequest, oldPullRequest, HandlerEventType.CREATE);

    PullRequestEventEmailRenderer renderer = new PullRequestEventEmailRenderer(event);

    assertEmail(renderer, "created");
  }

  @Test
  void shouldRenderEmailOnCreatedComment() throws IOException {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.CREATE);

    CommentEventEmailRenderer renderer = new CommentEventEmailRenderer(event);

    assertEmail(renderer, "created");
  }

  @Test
  void shouldRenderEmailOnModifiedComment() throws IOException {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.MODIFY);

    CommentEventEmailRenderer renderer = new CommentEventEmailRenderer(event);

    assertEmail(renderer, "modified");
  }

  @Test
  void shouldRenderEmailOnDeletedComment() throws IOException {
    CommentEvent event = new CommentEvent(repository, pullRequest, comment, oldComment, HandlerEventType.DELETE);

    CommentEventEmailRenderer renderer = new CommentEventEmailRenderer(event);

    assertEmail(renderer, "deleted");
  }

  @Test
  void shouldRenderEmailOnMergedPullRequest() throws IOException {
    PullRequestMergedEvent event = new PullRequestMergedEvent(repository, pullRequest);

    PullRequestMergedEmailRenderer renderer = new PullRequestMergedEmailRenderer(event);

    assertEmail(renderer, "merged");
  }

  @Test
  void shouldRenderEmailOnRejectedPullRequest() throws IOException {
    PullRequestRejectedEvent event = new PullRequestRejectedEvent(repository, pullRequest);

    PullRequestRejectedEmailRenderer renderer = new PullRequestRejectedEmailRenderer(event);

    assertEmail(renderer, "rejected");
  }

  private void assertEmail(EmailRenderer renderer, String event) throws IOException {
    String mailSubject = renderer.getMailSubject();
    TemplateEngineFactory templateFactory = mock(TemplateEngineFactory.class);
    when(templateFactory.getEngineByExtension(any())).thenReturn(createEngine());
    String mailContent = renderer.getMailContent("http://scm-manager.com/scm", templateFactory);

    assertThat(mailSubject).isNotEmpty()
      .contains(repository.getName(), repository.getNamespace(), pullRequest.getId(), pullRequest.getTitle(), event);

    assertThat(mailContent).isNotEmpty()
      .contains(repository.getName(), repository.getNamespace(), pullRequest.getId(), pullRequest.getTitle(), event);
  }
}
