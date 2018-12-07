package com.cloudogu.scm.review.service;

import sonia.scm.ContextEntry;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;

import static sonia.scm.NotFoundException.notFound;

public class RepositoryResolver {

  private final RepositoryManager manager;
  private final RepositoryServiceFactory serviceFactory;

  @Inject
  public RepositoryResolver(RepositoryManager manager, RepositoryServiceFactory serviceFactory) {
    this.manager = manager;
    this.serviceFactory = serviceFactory;
  }

  public Repository resolve(NamespaceAndName namespaceAndName) {
    Repository repository = manager.get(namespaceAndName);
    if (repository == null) {
      throw notFound(ContextEntry.ContextBuilder.entity(namespaceAndName));
    }

    if (!repositorySupportsMerge(repository)) {
      throw new PullRequestNotSupportedException(repository);
    }

    return repository;
  }

  private boolean repositorySupportsMerge(Repository repository) {
    try (RepositoryService service = serviceFactory.create(repository)) {
      return service.isSupported(Command.MERGE);
    }
  }
}
