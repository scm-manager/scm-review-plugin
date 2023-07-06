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

package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.google.common.io.CharStreams;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.Contributor;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.BranchCommandBuilder;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.template.Template;
import sonia.scm.template.TemplateEngine;
import sonia.scm.template.TemplateEngineFactory;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.EMail;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeCommitMessageServiceTest {

  @Mock
  private RepositoryServiceFactory serviceFactory;
  @Mock
  private RepositoryService repositoryService;
  @Mock
  private EMail email;
  @Mock
  private UserDisplayManager userDisplayManager;
  @Mock
  private ConfigService configService;

  @InjectMocks
  private MergeCommitMessageService service;

  @Mock
  private BranchCommandBuilder branchCommandBuilder;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private BranchesCommandBuilder branchesCommandBuilder;
  @Mock(answer = Answers.RETURNS_SELF)
  private LogCommandBuilder logCommandBuilder;

  @Mock //(answer = Answers.RETURNS_DEEP_STUBS)
  private TemplateEngineFactory templateEngineFactory;
  @Mock
  private TemplateEngine templateEngine;
  @Mock
  private Template template;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Subject subject;

  private final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();
  private final String pullRequestTitle = "Replace variable X with Y";
  private final DisplayUser pullRequestAuthor = DisplayUser.from(new User("zaphod", "Zaphod Beeblebrox", "zaphod@hitchhiker.com"));
  private final PullRequest pullRequest = new PullRequest("42", "feature", "main");

  @BeforeEach
  void initMocks() {
    when(configService.evaluateConfig(REPOSITORY.getNamespaceAndName())).thenReturn(new BasePullRequestConfig());
  }

  @BeforeEach
  void bindSubject() {
    ThreadContext.bind(subject);
  }

  @AfterEach
  void tearDownSubject() {
    ThreadContext.unbindSubject();
  }

  @Nested
  class ForDefaultCommitMessage {

    @BeforeEach
    void initMocks() {
      lenient().when(serviceFactory.create(REPOSITORY.getNamespaceAndName())).thenReturn(repositoryService);
      lenient().when(repositoryService.getRepository()).thenReturn(REPOSITORY);
      lenient().when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);
      lenient().when(logCommandBuilder.setAncestorChangeset(any())).thenReturn(logCommandBuilder);
      lenient().when(email.getMailOrFallback(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class).getMail());
      when(configService.evaluateConfig(REPOSITORY.getNamespaceAndName())).thenReturn(new BasePullRequestConfig());
    }

    @BeforeEach
    void preparePullRequest() throws IOException {
      when(repositoryService.isSupported(Command.LOG)).thenReturn(true);
      pullRequest.setAuthor("zaphod");
      pullRequest.setTitle(pullRequestTitle);

      Person pullRequestAuthor = new Person("Zaphod Beeblebrox", "zaphod@hitchhiker.com");

      Changeset changesetWithContributor = new Changeset("2", 2L, pullRequestAuthor, "second commit\nwith multiple lines");
      changesetWithContributor.addContributor(new Contributor(Contributor.CO_AUTHORED_BY, new Person("Ford", "prefect@hitchhiker.org")));
      changesetWithContributor.addContributor(new Contributor(Contributor.COMMITTED_BY, new Person("Marvin", "marvin@example.org")));
      ChangesetPagingResult changesets = new ChangesetPagingResult(3, asList(
        new Changeset("3", 3L, new Person("Arthur", "dent@hitchhiker.com"), "third commit"),
        changesetWithContributor,
        new Changeset("1", 1L, pullRequestAuthor, "first commit")
      ));

      when(logCommandBuilder.getChangesets()).thenReturn(changesets);
    }

    @Nested
    class WithCurrentUserNotPullRequestAuthor {

      @BeforeEach
      void setCurrentUser() {
        mockUser("Phil", "Phil Groundhog", "phil@groundhog.com");
        when(userDisplayManager.get("zaphod")).thenReturn(Optional.of(pullRequestAuthor));
      }

      @Test
      void shouldContainPullRequestTitle() {
        String defaultMessage = service.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);
        assertThat(defaultMessage).startsWith(pullRequestTitle + "\n\n");
      }

      @Test
      void shouldContainCommitMessagesFromSingleCommits() {
        String defaultMessage = service.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);
        assertThat(defaultMessage).startsWith(pullRequestTitle + "\n\n" +
          "Squash commits of branch feature:\n" +
          "\n" +
          "- first commit\n" +
          "\n" +
          "- second commit\n" +
          "with multiple lines\n" +
          "\n" +
          "- third commit\n");
      }

      @Test
      void shouldContainPullRequestDescription() {
        final String pullRequestDescription = "This pull request is to replace all occurring variables X with equivalent variables Y.";
        pullRequest.setDescription(pullRequestDescription);

        String defaultMessage = service.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);
        assertThat(defaultMessage).startsWith(pullRequestTitle + "\n\n" +
          pullRequestDescription);
      }

      @Test
      void shouldHaveCommitterFromCurrentUserIsDifferentThanPullRequestAuthor() {
        String defaultMessage = service.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);

        assertThat(defaultMessage).contains("Committed-by: Phil Groundhog <phil@groundhog.com>");
      }

      @Test
      void shouldHaveCommitterFromSingleCommits() {
        String defaultMessage = service.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);

        assertThat(defaultMessage).contains("Co-authored-by: Arthur <dent@hitchhiker.com>");
      }

      @Test
      void shouldHaveCoAuthorFromSingleCommits() {
        String defaultMessage = service.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);

        assertThat(defaultMessage).contains("Co-authored-by: Ford <prefect@hitchhiker.org>");
      }

      @Test
      void shouldNotHaveOtherContributorsThanCoAuthoredFromCommits() {
        String defaultMessage = service.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);

        assertThat(defaultMessage).doesNotContain("marvin@example.org");
      }

      @Test
      void shouldNotHaveCommitterWhenCurrentUserIsPullRequestAuthor() {
        mockUser(pullRequestAuthor.getId(), pullRequestAuthor.getDisplayName(), pullRequestAuthor.getMail());

        String defaultMessage = service.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);

        assertThat(defaultMessage).doesNotContain("Committed-by");
      }

      @Nested
      class WithCustomTemplate {

        @Captor
        private ArgumentCaptor<StringReader> templateCaptor;
        @Captor
        private ArgumentCaptor<Map<String, Object>> model;

        @BeforeEach
        void mockTemplateEngine() throws IOException {
          when(templateEngineFactory.getDefaultEngine())
            .thenReturn(templateEngine);
          when(templateEngine.getTemplate(isNull(), templateCaptor.capture()))
            .thenReturn(template);
          doAnswer(invocation -> {
            invocation.getArgument(0, Writer.class).append("Filled template");
            return null;
          })
            .when(template)
            .execute(any(), model.capture());
        }

        @BeforeEach
        void mockCustomTemplateInConfig() {
          BasePullRequestConfig config = new BasePullRequestConfig();
          config.setOverwriteDefaultCommitMessage(true);
          config.setCommitMessageTemplate("Template");
          when(configService.evaluateConfig(REPOSITORY.getNamespaceAndName()))
            .thenReturn(config);
        }

        @Test
        void shouldUseMessageTemplateIfEnabled() throws IOException {
          String defaultMessage = service.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);

          assertThat(defaultMessage)
            .isEqualTo("Filled template");
          assertThat(CharStreams.toString(templateCaptor.getValue()))
            .isEqualTo("Template");
          assertThat(model.getValue())
            .containsEntry("pullRequest", pullRequest)
            .containsEntry("namespace", REPOSITORY.getNamespaceAndName().getNamespace())
            .containsEntry("repositoryName", REPOSITORY.getNamespaceAndName().getName());
        }
      }
    }
  }

  private void mockUser(String name, String displayName, String mail) {
    when(subject.getPrincipals().oneByType(User.class))
      .thenReturn(new User(name, displayName, mail));
  }
}
