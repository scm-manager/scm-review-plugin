package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.Recipient;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.common.collect.Lists;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.codemonkey.simplejavamail.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.repository.Repository;
import sonia.scm.template.Template;
import sonia.scm.template.TemplateEngine;
import sonia.scm.template.TemplateEngineFactory;
import sonia.scm.template.TemplateType;
import sonia.scm.user.User;

import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

  @Mock
  TemplateEngineFactory templateEngineFactory;

  @Mock
  private MailService mailService;

  @Mock
  private ScmConfiguration configuration;
  @InjectMocks
  EmailNotificationService service;

  private PullRequest pullRequest;
  private ArrayList<Recipient> subscriber;
  private Repository repository;
  private PullRequest oldPullRequest;
  private PullRequestComment comment;
  private PullRequestComment oldComment;

  private final Subject subject = mock(Subject.class);
  private Recipient recipient1;
  private Recipient recipient2;
  private EmailContext emailContext;

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

    when(configuration.getBaseUrl()).thenReturn("http://www.scm-manager.com");

    pullRequest = TestData.createPullRequest();
    recipient1 = new Recipient("user1", "email1@d.de");
    recipient2 = new Recipient("user2", "email1@d.de");
    subscriber = Lists.newArrayList(recipient1, recipient2);
    pullRequest.setSubscriber(subscriber);
    repository = createHeartOfGold();
    oldPullRequest = TestData.createPullRequest();
    oldPullRequest.setTitle("old Title");
    oldPullRequest.setDescription("old Description");
    comment = TestData.createComment();
    oldComment = TestData.createComment();
    oldComment.setComment("this is my old comment");
    comment.setComment("this is my modified comment");

    emailContext = new EmailContext();
    emailContext.setPullRequest(pullRequest);
    emailContext.setRepository(repository);
    emailContext.setRecipients(pullRequest.getSubscriber());

    TemplateEngine templateEngine = createEngine();
    when(templateEngineFactory.getEngineByExtension(any())).thenReturn(templateEngine);
  }

  @TestFactory
  Stream<DynamicTest> testSendingEmails() {
    return Stream.of(Notification.values())
      .map(notification -> DynamicTest.dynamicTest(
        "Should send " + notification + " Email",
        () -> testSendEmail(notification)));
  }

  private TemplateEngine createEngine() {
    return new TemplateEngine() {
      @Override
      public Template getTemplate(String templatePath) {
        return (writer, model) -> {
          URL resource = Notification.class.getClassLoader().getResource(templatePath);
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


  private void testSendEmail(Notification notification) throws MailSendBatchException {
    if (notification == Notification.DELETED_COMMENT) {
      emailContext.setComment(null);
      emailContext.setOldComment(oldComment);
    }
    if (notification == Notification.MODIFIED_COMMENT) {
      emailContext.setOldComment(oldComment);
      emailContext.setComment(comment);
    }
    if (notification == Notification.CREATED_COMMENT) {
      emailContext.setComment(comment);
    }
    if (notification == Notification.MODIFIED_PULL_REQUEST) {
      emailContext.setOldPullRequest(oldPullRequest);
    }

    service.sendEmail(emailContext, notification);

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailService, times(2)).send(emailCaptor.capture());
    reset(mailService);
    assertSentEmails(emailCaptor, notification);
  }

  private void assertSentEmails(ArgumentCaptor<Email> emailCaptor, Notification notification) {

    List<Email> capturedEmails = emailCaptor.getAllValues();
    Email email1 = capturedEmails.get(0);
    Email email2 = capturedEmails.get(1);
    assertThat(email1.getSubject())
      .contains(pullRequest.getId(), pullRequest.getTitle(), repository.getNamespace(), repository.getName());
    assertThat(email1.getRecipients()).hasSize(1);
    assertThat(email1.getRecipients().get(0)).isEqualToComparingOnlyGivenFields(recipient1, "name", "address");
    assertThat(email1.getTextHTML()).contains(pullRequest.getId());
    assertThat(email1.getTextHTML()).contains("http://www.scm-manager.com/repo/hitchhiker/HeartOfGold/pull-request/id");

    assertThat(email2.getRecipients()).hasSize(1);
    assertThat(email2.getRecipients().get(0)).isEqualToComparingOnlyGivenFields(recipient2, "name", "address");
    assertThat(email2.getTextHTML()).contains(pullRequest.getId());
    assertThat(email2.getTextHTML()).contains("http://www.scm-manager.com/repo/hitchhiker/HeartOfGold/pull-request/id");
    if (notification == Notification.DELETED_COMMENT) {
      assertThat(email1.getTextHTML()).contains(oldComment.getComment());
    }
    if (notification == Notification.MODIFIED_COMMENT) {
      assertThat(email1.getTextHTML()).contains(oldComment.getComment());
      assertThat(email1.getTextHTML()).contains(comment.getComment());
    }
     if (notification == Notification.CREATED_COMMENT) {
      assertThat(email1.getTextHTML()).contains(comment.getComment());
    }
    if (notification == Notification.MODIFIED_PULL_REQUEST) {
      assertThat(email1.getTextHTML())
        .contains(oldPullRequest.getTitle())
        .contains(oldPullRequest.getDescription())
      ;
    }
  }
}
