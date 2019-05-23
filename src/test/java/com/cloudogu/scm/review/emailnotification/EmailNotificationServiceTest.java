package com.cloudogu.scm.review.emailnotification;

import com.google.common.collect.Sets;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.mail.api.MailSendBatchException;
import sonia.scm.mail.api.MailService;
import sonia.scm.mail.api.MailTemplateType;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

  @Mock
  private MailService mailService;

  @Mock(answer = Answers.RETURNS_SELF)
  private MailService.EnvelopeBuilder envelopeBuilder;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MailService.SubjectBuilder subjectBuilder;

  @Mock
  private ScmConfiguration configuration;

  @InjectMocks
  private EmailNotificationService service;

  @Mock
  private MailTextResolver mailTextResolver;


  @Test
  void shouldNotSendEmailForNotConfiguredMailServer() throws Exception {
    when(mailService.isConfigured()).thenReturn(false);

    service.sendEmails(mailTextResolver, Collections.singleton("trillian"), Collections.emptySet());

    verifyNoMoreInteractions(mailService);
  }

  @Nested
  class WithPrincipal {

    @Mock
    private Subject subject;

    private Map<String,Object> subscriberModel = Collections.singletonMap("subscriber", true);

    private Map<String,Object> reviewerModel = Collections.singletonMap("reviewer", true);

    @BeforeEach
    void setUpSubject() {
      ThreadContext.bind(subject);
    }

    @AfterEach
    void tearDownSubject() {
      ThreadContext.unbindSubject();
    }

    @Test
    void shouldSendEmailForSubscriberOnly() throws MailSendBatchException {
      mockDependencies("slarti");

      service.sendEmails(mailTextResolver, Sets.newHashSet("dent", "trillian"), Collections.singleton("marvin"));

      verifier().send("dent", "trillian").notSend("marvin");
    }

    @Test
    void shouldNotSendEmailToPrincipal() throws MailSendBatchException {
      mockDependencies("dent");

      service.sendEmails(mailTextResolver, Sets.newHashSet("dent", "trillian"), Collections.emptySet());

      verifier().send("trillian").notSend("dent");
    }

    @Test
    void shouldSendEmailForReviewers() throws MailSendBatchException {
      mockDependencies("marvin");

      service.sendEmails(mailTextResolver, Collections.singleton("dent"), Collections.singleton("dent"));

      verifier().send("dent").reviewer();
    }

    @Test
    void shouldSendEmailForSubscriber() throws MailSendBatchException {
      mockDependencies("marvin");

      service.sendEmails(mailTextResolver, Collections.singleton("dent"), Collections.emptySet());

      verifier().send("dent").subscriber();
    }

    private Verifier verifier() {
      verify(envelopeBuilder).fromCurrentUser();
      return new Verifier();
    }

    private void mockDependencies(String principal) {
      when(mailService.isConfigured()).thenReturn(true);
      when(configuration.getBaseUrl()).thenReturn("https://scm.hitchhiker.com");

      when(mailTextResolver.getMailSubject(any())).thenReturn("Awesome Subject");
      when(mailTextResolver.getContentTemplatePath()).thenReturn("/path/to/template");
      when(mailTextResolver.getContentTemplateModel(anyString(), anyBoolean())).then(ic -> {
        boolean reviewer = ic.getArgument(1);
        return reviewer ? reviewerModel : subscriberModel;
      });

      when(subject.getPrincipal()).thenReturn(principal);

      when(envelopeBuilder.withSubject("Awesome Subject")).thenReturn(subjectBuilder);
      when(mailService.emailTemplateBuilder()).thenReturn(envelopeBuilder);
    }



    class Verifier {

      Verifier send(String... users) {
        for (String user : users) {
          verify(envelopeBuilder).toUser(user);
        }
        return this;
      }

      Verifier notSend(String... users) {
        for (String user : users) {
          verify(envelopeBuilder, never()).toUser(user);
        }
        return this;
      }

      Verifier reviewer() {
        verify(subjectBuilder.withTemplate("/path/to/template", MailTemplateType.MARKDOWN_HTML)).andModel(reviewerModel);
        return this;
      }

      Verifier subscriber() {
        verify(subjectBuilder.withTemplate("/path/to/template", MailTemplateType.MARKDOWN_HTML)).andModel(subscriberModel);
        return this;
      }
    }

  }


}
