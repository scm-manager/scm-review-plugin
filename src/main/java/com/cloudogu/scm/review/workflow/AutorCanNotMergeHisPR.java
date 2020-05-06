package com.cloudogu.scm.review.workflow;


import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.pullrequest.service.MergeGuard;
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Extension
public class AutorCanNotMergeHisPR implements Rule {



  @Override
  public Result validate(Context context) {

    boolean authorApproved = context.getPullRequest().getAuthor().equals("SCM Administrator");

    if (authorApproved) {
      return failed();
    }
    return success();
  }

}
