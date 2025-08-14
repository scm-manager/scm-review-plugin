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
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.Contributor;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.template.TemplateEngineFactory;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.EMail;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Map.entry;
import static org.apache.shiro.SecurityUtils.getSubject;
import static sonia.scm.ContextEntry.ContextBuilder.entity;

class MergeCommitMessageService {

  private static final String MERGE_COMMIT_MESSAGE_TEMPLATE = String.join("\n",
    "Merge of branch {0} into {1}",
    "",
    "Automatic merge by SCM-Manager.");
  private static final String SQUASH_COMMIT_MESSAGE_TEMPLATE = String.join("\n",
    "{3}",
    "",
    "{2}");

  private static final Logger LOG = LoggerFactory.getLogger(MergeCommitMessageService.class);

  private final RepositoryServiceFactory serviceFactory;
  private final EMail email;
  private final UserDisplayManager userDisplayManager;
  private final ConfigService configService;
  private final TemplateEngineFactory templateEngineFactory;

  @Inject
  MergeCommitMessageService(RepositoryServiceFactory serviceFactory,
                            EMail email,
                            UserDisplayManager userDisplayManager,
                            ConfigService configService,
                            TemplateEngineFactory templateEngineFactory
  ) {
    this.serviceFactory = serviceFactory;
    this.email = email;
    this.userDisplayManager = userDisplayManager;
    this.configService = configService;
    this.templateEngineFactory = templateEngineFactory;
  }

  public String determineDefaultMessage(NamespaceAndName namespaceAndName, PullRequest pullRequest, MergeStrategy strategy) {
    BasePullRequestConfig config = configService.evaluateConfig(namespaceAndName);
    if (config.isOverwriteDefaultCommitMessage()) {
      return renderTemplate(config, namespaceAndName, pullRequest);
    }
    if (strategy == null) {
      return "";
    }
    switch (strategy) {
      case SQUASH:
        return createDefaultSquashCommitMessage(namespaceAndName, pullRequest);
      case FAST_FORWARD_IF_POSSIBLE: // should be same as merge (fallback message only)
      case MERGE_COMMIT: // should be default
      default:
        return createDefaultMergeCommitMessage(pullRequest);
    }
  }

  private String renderTemplate(BasePullRequestConfig config, NamespaceAndName namespaceAndName, PullRequest pullRequest) {
    try {
      Map<String, Object> model = createTemplateModel(namespaceAndName, pullRequest);
      StringWriter writer = new StringWriter();
      templateEngineFactory
        .getDefaultEngine()
        .getTemplate(null, new StringReader(config.getCommitMessageTemplate()))
        .execute(writer, model);
      return writer.toString();
    } catch (IOException e) {
      LOG.warn("failed to render commit message template for repository {}", namespaceAndName, e);
      return "Error processing the custom message template";
    }
  }

  private Map<String, Object> createTemplateModel(NamespaceAndName namespaceAndName, PullRequest pullRequest) throws IOException {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      List<Changeset> changesets = getChangesetsFromLogCommand(pullRequest, repositoryService);
      Set<Contributor> contributors = computeContributors(changesets);
      return Map.ofEntries(
        entry("namespace", namespaceAndName.getNamespace()),
        entry("repositoryName", namespaceAndName.getName()),
        entry("pullRequest", pullRequest),
        entry("contributors", contributors),
        entry("changesets", changesets),
        entry("currentUser", DisplayUser.from(currentUser())),
        entry("date", Instant.now().toString()),
        entry("localDate", LocalDateTime.now().toString()),
        entry("author", userDisplayManager.get(pullRequest.getAuthor()).orElseGet(() -> DisplayUser.from(new User(pullRequest.getAuthor()))))
      );
    }
  }

  private String createDefaultSquashCommitMessage(NamespaceAndName namespaceAndName, PullRequest pullRequest) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      if (repositoryService.isSupported(Command.LOG)) {
        try {
          StringBuilder builder = new StringBuilder();
          List<Changeset> changesetsFromLogCommand = getChangesetsFromLogCommand(pullRequest, repositoryService);
          Set<Contributor> contributors = computeContributors(changesetsFromLogCommand);
          builder.append("Squash commits of branch ").append(pullRequest.getSource()).append(":\n\n");
          for (int i = changesetsFromLogCommand.size() - 1; i >= 0; --i) {
            Changeset changeset = changesetsFromLogCommand.get(i);
            builder.append("- ").append(changeset.getDescription()).append("\n\n");
          }

          if (pullRequest.getDescription() != null && !pullRequest.getDescription().isEmpty()) {
            builder = new StringBuilder(pullRequest.getDescription());
          }

          appendSquashContributors(builder, pullRequest, contributors);

          return MessageFormat.format(SQUASH_COMMIT_MESSAGE_TEMPLATE, pullRequest.getSource(), pullRequest.getTarget(), builder.toString(), pullRequest.getTitle());
        } catch (IOException e) {
          throw new InternalRepositoryException(entity("Branch", pullRequest.getSource()).in(repositoryService.getRepository()),
            "Could not read changesets from repository");
        }
      } else {
        return createDefaultMergeCommitMessage(pullRequest);
      }
    }
  }

  private Set<Contributor> computeContributors(List<Changeset> changesets) {
    Set<Contributor> contributors = new HashSet<>();
    changesets.forEach(
      changeset -> {
        contributors.add(new Contributor(Contributor.CO_AUTHORED_BY, changeset.getAuthor()));
        Collection<Contributor> contributorsFromChangeset = changeset.getContributors();
        if (contributorsFromChangeset != null) {
          contributors.addAll(contributorsFromChangeset);
        }
      }
    );
    return contributors;
  }

  private String createDefaultMergeCommitMessage(PullRequest pullRequest) {
    return MessageFormat.format(MERGE_COMMIT_MESSAGE_TEMPLATE, pullRequest.getSource(), pullRequest.getTarget());
  }

  private void appendSquashContributors(StringBuilder builder, PullRequest pullRequest, Set<Contributor> contributors) {
    builder.append("\n");
    userDisplayManager.get(pullRequest.getAuthor()).ifPresent(prAuthor -> {
      appendCoAuthors(builder, contributors, prAuthor);
      appendReviewers(builder, pullRequest, prAuthor, currentUser());
    });
  }

  private void appendReviewers(StringBuilder builder, PullRequest pullRequest, DisplayUser prAuthor, User commiter) {
    for (Map.Entry<String, Boolean> reviews : pullRequest.getReviewer().entrySet()) {
      userDisplayManager.get(reviews.getKey()).ifPresent(reviewer -> {
        if (!reviews.getValue()) {
          return;
        }

        if (prAuthor.getDisplayName().equals(reviewer.getDisplayName())) {
          return;
        }

        if (commiter.getDisplayName().equals(reviewer.getDisplayName())) {
          return;
        }

        builder.append("\nReviewed-by: ").append(reviewer.getDisplayName()).append(" <").append(reviewer.getMail()).append(">");
      });
    }
  }

  private void appendCoAuthors(StringBuilder builder, Set<Contributor> contributors, DisplayUser prAuthor) {
    for (Contributor contributor : contributors) {
      userDisplayManager.get(contributor.getPerson().getName()).ifPresent(contributorUser -> {
        if (prAuthor.getDisplayName().equals(contributorUser.getDisplayName())) {
          return;
        }

        if (!contributor.getType().equals(Contributor.CO_AUTHORED_BY)) {
          return;
        }

        appendCoAuthor(builder, contributorUser.getDisplayName(), contributorUser.getMail());
      });
    }
  }

  private void appendCoAuthor(StringBuilder builder, String name, String mail) {
    builder.append("\n" + Contributor.CO_AUTHORED_BY + ": ").append(name).append(" <").append(mail).append(">");
  }

  private List<Changeset> getChangesetsFromLogCommand(PullRequest pullRequest, RepositoryService repositoryService) throws IOException {
    if (repositoryService.isSupported(Command.LOG)) {
      return repositoryService.getLogCommand()
        .setBranch(pullRequest.getSource())
        .setAncestorChangeset(pullRequest.getTarget())
        .getChangesets()
        .getChangesets();
    } else {
      return emptyList();
    }
  }

  private static User currentUser() {
    return getSubject().getPrincipals().oneByType(User.class);
  }
}
