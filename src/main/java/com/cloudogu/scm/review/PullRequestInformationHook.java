package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.github.legman.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static java.util.Optional.ofNullable;

@EagerSingleton @Extension
public class PullRequestInformationHook {

  private static final Logger LOG = LoggerFactory.getLogger(PullRequestInformationHook.class);

  private final PullRequestService service;
  private final RepositoryServiceFactory serviceFactory;
  private final ScmConfiguration configuration;

  @Inject
  public PullRequestInformationHook(DefaultPullRequestService service, RepositoryServiceFactory serviceFactory, ScmConfiguration configuration) {
    this.service = service;
    this.serviceFactory = serviceFactory;
    this.configuration = configuration;
  }

  @Subscribe(async = false)
  public void checkForInformation(PostReceiveRepositoryHookEvent event) {
    try (RepositoryService repositoryService = serviceFactory.create(event.getRepository())) {
      if (!repositoryService.isSupported(Command.MERGE)) {
        LOG.trace("ignoring post receive event for repository {}", event.getRepository().getNamespaceAndName());
        return;
      }
      List<String> effectedBranches =
        event
          .getContext()
          .getBranchProvider()
          .getCreatedOrModified();
      effectedBranches.forEach(branch -> {
        List<PullRequest> pullRequests = service.getAll(event.getRepository().getNamespace(), event.getRepository().getName());
        boolean prFound = new Worker(event).process(pullRequests, branch);
        if (!prFound) {
          event.getContext()
            .getMessageProvider()
            .sendMessage("Create new pull request here: " + createCreateLink(event.getRepository(), branch));
        }
      });
    }
  }

  private class Worker {
    private final PostReceiveRepositoryHookEvent event;

    private boolean prFound = false;

    private Worker(PostReceiveRepositoryHookEvent event) {
      this.event = event;
    }

    private boolean process(List<PullRequest> pullRequests, String branch) {
      pullRequests
        .stream()
        .filter(this::pullRequestHasStatusOpen)
        .filter(pr -> branch.equals(pr.getSource()))
        .forEach(this::messageForExistingPullRequest);
      return prFound;
    }

    private void messageForExistingPullRequest(PullRequest pullRequest) {
      this.prFound = true;
      event.getContext()
        .getMessageProvider()
        .sendMessage("Check pull request here: " + createPullRequestLink(pullRequest));
    }

    private boolean pullRequestHasStatusOpen(PullRequest pullRequest) {
      return pullRequest.getStatus() == PullRequestStatus.OPEN;
    }

    private String createPullRequestLink(PullRequest pullRequest) {
      Repository repository = event.getRepository();
      return String.format(
        "%s/repo/%s/%s/pull-requests/%s/",
        configuration.getBaseUrl(),
        repository.getNamespace(),
        repository.getName(),
        pullRequest.getId());
    }
  }

  private String createCreateLink(Repository repository, String source) {
    return String.format(
      "%s/repo/%s/%s/pull-requests/add/changesets/?source=%s",
      configuration.getBaseUrl(),
      repository.getNamespace(),
      repository.getName(),
      source);
  }
}
