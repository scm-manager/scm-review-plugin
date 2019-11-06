package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.MergeStrategyNotSupportedException;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.cloudogu.scm.review.pullrequest.dto.MergeCommitDto;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.MergeCommandBuilder;
import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;

public class MergeService {

  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public MergeService(RepositoryServiceFactory serviceFactory) {
    this.serviceFactory = serviceFactory;
  }

  public MergeCommandResult merge(NamespaceAndName namespaceAndName, MergeCommitDto mergeCommitDto, MergeStrategy strategy) {
    try (RepositoryService repositoryService = serviceFactory.create(namespaceAndName)) {
      MergeCommandBuilder mergeCommand = repositoryService.getMergeCommand();
      isAllowedToMerge(repositoryService.getRepository(), mergeCommand, strategy);
      prepareMergeCommand(mergeCommand, mergeCommitDto, strategy);
      MergeCommandResult mergeCommandResult = mergeCommand.executeMerge();
      if (isFastForwardPossible(strategy, mergeCommandResult)) {
        return mergeCommandResult;
      }
      prepareMergeCommand(mergeCommand, mergeCommitDto, MergeStrategy.MERGE_COMMIT);
      return mergeCommand.executeMerge();
    }
  }

  private boolean isFastForwardPossible(MergeStrategy strategy, MergeCommandResult mergeCommandResult) {
    return strategy == MergeStrategy.FAST_FORWARD_IF_POSSIBLE && mergeCommandResult.isSuccess();
  }

  private void isAllowedToMerge(Repository repository, MergeCommandBuilder mergeCommand, MergeStrategy strategy) {
    PermissionCheck.mayMerge(repository);
    if (!mergeCommand.isSupported(strategy)) {
      throw new MergeStrategyNotSupportedException(repository, strategy);
    }
  }

  private void prepareMergeCommand(MergeCommandBuilder mergeCommand, MergeCommitDto mergeCommitDto, MergeStrategy strategy) {
    mergeCommand.setBranchToMerge(mergeCommitDto.getSource());
    mergeCommand.setTargetBranch(mergeCommitDto.getTarget());
    mergeCommand.setMessageTemplate(mergeCommitDto.getCommitMessage());
    mergeCommand.setMergeStrategy(strategy);
    DisplayedUserDto author = mergeCommitDto.getAuthor();
    mergeCommand.setAuthor(new Person(author.getDisplayName(), author.getMail()));
  }
}
