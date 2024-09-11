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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
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

  private final User pullRequestAuthorUser = new User("zaphod", "Zaphod Beeblebrox", "zaphod@hitchhiker.com");
  private final DisplayUser pullRequestAuthor = DisplayUser.from(pullRequestAuthorUser);

  private final User arthurCoAuthorUser = new User("arthur", "Arthur", "dent@hitchhiker.com");
  private final DisplayUser arthurCoAuthor = DisplayUser.from(arthurCoAuthorUser);

  private final User marvinCoAuthorUser = new User("marvin", "Marvin", "marvin@example.org");
  private final DisplayUser marvinCoAuthor = DisplayUser.from(marvinCoAuthorUser);

  private final User fordCoAuthorUser = new User("ford", "Ford", "prefect@hitchhiker.org");
  private final DisplayUser fordCoAuthor = DisplayUser.from(fordCoAuthorUser);

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

      Person pullRequestAuthor = new Person(pullRequestAuthorUser.getName(), pullRequestAuthorUser.getMail());

      Changeset changesetWithContributor = new Changeset("2", 2L, pullRequestAuthor, "second commit\nwith multiple lines");
      changesetWithContributor.addContributor(
        new Contributor(Contributor.CO_AUTHORED_BY, new Person(fordCoAuthorUser.getName(), fordCoAuthorUser.getMail()))
      );
      changesetWithContributor.addContributor(
        new Contributor(Contributor.COMMITTED_BY, new Person(marvinCoAuthorUser.getName(), marvinCoAuthorUser.getMail()))
      );
      ChangesetPagingResult changesets = new ChangesetPagingResult(3, asList(
        new Changeset("3", 3L, new Person(arthurCoAuthorUser.getName(), arthurCoAuthorUser.getMail()), "third commit"),
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
        when(userDisplayManager.get(pullRequestAuthorUser.getName())).thenReturn(Optional.of(pullRequestAuthor));
        when(userDisplayManager.get(arthurCoAuthorUser.getName())).thenReturn(Optional.of(arthurCoAuthor));
        when(userDisplayManager.get(marvinCoAuthorUser.getName())).thenReturn(Optional.of(marvinCoAuthor));
        when(userDisplayManager.get(fordCoAuthorUser.getName())).thenReturn(Optional.of(fordCoAuthor));
      }

      @Test
      void shouldOnlyContainReviewersWithApprovalThatAreNotAuthorOrCommitter() {
        Map<String, Boolean> reviewers = new HashMap<>();
        reviewers.put("Phil", true);
        reviewers.put(pullRequestAuthorUser.getName(), true);
        reviewers.put(arthurCoAuthorUser.getName(), false);
        reviewers.put(marvinCoAuthorUser.getName(), true);
        pullRequest.setReviewer(reviewers);

        String defaultMessage = service.determineDefaultMessage(REPOSITORY.getNamespaceAndName(), pullRequest, MergeStrategy.SQUASH);

        assertThat(defaultMessage).doesNotContain("Reviewed-by: Phil Groundhog <phil@groundhog.com>");
        assertThat(defaultMessage).doesNotContain("Reviewed-by: Zaphod Beeblebrox <zaphod@hitchhiker.com>");
        assertThat(defaultMessage).doesNotContain("Reviewed-by: Arthur <dent@hitchhiker.com>");
        assertThat(defaultMessage).contains("Reviewed-by: Marvin <marvin@example.org>");
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

        assertThat(defaultMessage).doesNotContain("Committed-by: Phil Groundhog <phil@groundhog.com>");
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

      @Test
      void shouldNotHaveCommitter() {
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
          reset(userDisplayManager);
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
