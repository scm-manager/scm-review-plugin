package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.CurrentUserResolver;
import sonia.scm.plugin.Extension;

@Extension
public class AutorCanNotMerge implements Rule {

  @Override
  public Result validate(Context context) {


    boolean authorApproved = context.getPullRequest().getAuthor().equals(CurrentUserResolver.getCurrentUserDisplayName());

    if (authorApproved) {
      return failed();
    }
    return success();
  }

}
