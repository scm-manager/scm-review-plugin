package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.comment.service.CommentType;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.migration.UpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.xml.XmlRepositoryDAO;
import sonia.scm.version.Version;

@Extension
public class PullRequestCommentDataUpdateStep implements UpdateStep {

  private static final Logger LOG = LoggerFactory.getLogger(PullRequestCommentDataUpdateStep.class);
  private final XmlRepositoryDAO repositoryDAO;

  @Inject
  public PullRequestCommentDataUpdateStep(XmlRepositoryDAO repositoryDAO) {
    this.repositoryDAO = repositoryDAO;
  }

  @Override
  public void doUpdate() {
      for(Repository repository : repositoryDAO.getAll()) {
        if (repository.getType() == null) {
          LOG.debug("update pullrequest comments to new data structure for repository id {}", repository.getId());
          repository.setType(String.valueOf(CommentType.COMMENT));
        }
      }
  }

  @Override
  public Version getTargetVersion() {
    return Version.parse("2.0.1");
  }

  @Override
  public String getAffectedDataType() {
    return "sonia.scm.review.pullrequest.comment.data.xml";
  }
}
