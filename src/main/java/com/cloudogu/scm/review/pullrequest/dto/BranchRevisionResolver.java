package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import sonia.scm.repository.Branch;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class BranchRevisionResolver {
  private final RepositoryServiceFactory repositoryServiceFactory;
  private final PullRequestService pullRequestService;

  @Inject
  public BranchRevisionResolver(RepositoryServiceFactory repositoryServiceFactory, PullRequestService pullRequestService) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.pullRequestService = pullRequestService;
  }

  public RevisionResult getRevisions(String namespace, String name, String pullRequestId) {
    PullRequest pullRequest = pullRequestService.get(namespace, name, pullRequestId);
    return getRevisions(new NamespaceAndName(namespace, name), pullRequest);
  }

  public RevisionResult getRevisions(NamespaceAndName namespaceAndName, PullRequest pullRequest) {
    try (RepositoryService repositoryService = repositoryServiceFactory.create(namespaceAndName)) {
      RevisionResolver resolver = new RevisionResolver(repositoryService);
      String sourceRevision = resolver.resolve(pullRequest.getSource());
      String targetRevision = resolver.resolve(pullRequest.getTarget());
      return new RevisionResult(sourceRevision, targetRevision);
    }
  }

  private static class RevisionResolver {

    private final RepositoryService repositoryService;
    private List<Branch> branches;

    private RevisionResolver(RepositoryService service) {
      this.repositoryService = service;
    }

    public String resolve(String branch) {
      try {
        if (!isBranchAvailable(branch)) {
          return "";
        }
        return resolveChangesetId(branch);
      } catch (IOException e) {
        throw new InternalRepositoryException(
          repositoryService.getRepository(),
          "could not determine revision for branch " + branch,
          e
        );
      }
    }

    private String resolveChangesetId(String branch) throws IOException {
      return repositoryService.getLogCommand()
        .setBranch(branch)
        .setPagingLimit(1)
        .getChangesets()
        .iterator()
        .next()
        .getId();
    }

    private boolean isBranchAvailable(String branch) throws IOException {
      return getBranches()
        .stream()
        .anyMatch(b -> branch.equals(b.getName()));
    }

    private List<Branch> getBranches() throws IOException {
      if (branches == null) {
        branches = repositoryService.getBranchesCommand().getBranches().getBranches();
      }
      return branches;
    }
  }

  public static class RevisionResult {
    private final String sourceRevision;
    private final String targetRevision;

    public RevisionResult(String sourceRevision, String targetRevision) {
      this.sourceRevision = sourceRevision;
      this.targetRevision = targetRevision;
    }

    public String getSourceRevision() {
      return sourceRevision;
    }

    public String getTargetRevision() {
      return targetRevision;
    }
  }
}
