package com.cloudogu.scm.review.guard;

import com.cloudogu.scm.editor.ChangeGuard;
import com.cloudogu.scm.editor.ChangeObstacle;
import com.cloudogu.scm.review.config.service.ConfigService;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

@Extension(requires = "scm-editor-plugin")
public class MergeOnlyChangeGuard implements ChangeGuard {

  private final RepositoryManager repositoryManager;
  private final ConfigService configService;

  @Inject
  public MergeOnlyChangeGuard(RepositoryManager repositoryManager, ConfigService configService) {
    this.repositoryManager = repositoryManager;
    this.configService = configService;
  }

  @Override
  public Collection<ChangeObstacle> getObstacles(NamespaceAndName namespaceAndName, String branch, Changes changes) {
    Repository repository = repositoryManager.get(namespaceAndName);
    if (configService.isBranchProtected(repository, branch)) {
      return Collections.singleton(new ChangeObstacle() {
        @Override
        public String getMessage() {
          return "The branch " + branch + " can only be written using pull requests";
        }

        @Override
        public String getKey() {
          return "scm-review-plugin.obstacle";
        }
      });
    } else {
      return Collections.emptyList();
    }
  }
}
