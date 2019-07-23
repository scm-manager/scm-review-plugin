package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.PullRequestComments;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.migration.UpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryDAO;
import sonia.scm.repository.xml.XmlRepositoryDAO;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;
import sonia.scm.version.Version;

@Extension
public class PullRequestCommentDataUpdateStep implements UpdateStep {

  private static final Logger LOG = LoggerFactory.getLogger(PullRequestCommentDataUpdateStep.class);
  private static final String STORE_NAME = "pullRequestComment";
  private final RepositoryDAO repositoryDAO;
  private final DataStoreFactory dataStoreFactory;

  @Inject
  public PullRequestCommentDataUpdateStep(XmlRepositoryDAO repositoryDAO, DataStoreFactory dataStoreFactory) {
    this.repositoryDAO = repositoryDAO;
    this.dataStoreFactory = dataStoreFactory;
  }

  @Override
  public void doUpdate() {
    for(Repository repository : repositoryDAO.getAll()) {
      DataStore<PullRequestComments> dataStore = dataStoreFactory.withType(PullRequestComments.class).withName(STORE_NAME).forRepository(repository).build();
      LOG.debug("update pullrequest comments for repository with id {}", repository.getId());
      dataStore.getAll().forEach((key, properties) -> properties.getComments().forEach(comment -> {
        if (comment.getType() == null) {
          comment.setType(CommentType.COMMENT);
        }
        dataStore.put(key, properties);
      })
      );
    }
  }

  @Override
  public Version getTargetVersion() {
    return Version.parse("2.0.1");
  }

  @Override
  public String getAffectedDataType() {
    return "sonia.scm.pullrequest.comment.data.xml";
  }
}
