package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.io.IOException;

public class BranchRevisionResolver {
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public BranchRevisionResolver(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  public RevisionResult getRevisions(NamespaceAndName namespaceAndName, PullRequest pullRequest) {
    try (RepositoryService repositoryService = repositoryServiceFactory.create(namespaceAndName)) {
      String sourceRevision = getRevision(repositoryService, pullRequest.getSource());
      String targetRevision = getRevision(repositoryService, pullRequest.getTarget());
      return new RevisionResult(sourceRevision, targetRevision);
    }
  }

  private String getRevision(RepositoryService repositoryService, String branch) {
    try {
      LogCommandBuilder logCommand = repositoryService.getLogCommand();
      ChangesetPagingResult changesets = logCommand.setBranch(branch).setPagingLimit(1).getChangesets();
      return changesets.iterator().next().getId();
    } catch (IOException e) {
      throw new InternalRepositoryException(repositoryService.getRepository(), "could not determine revision for branch " + branch, e);
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
