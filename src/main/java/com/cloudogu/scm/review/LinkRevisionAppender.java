package com.cloudogu.scm.review;

import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;

public class LinkRevisionAppender {

  public static String append(String uri, BranchRevisionResolver.RevisionResult revisionResult) {
    return uri + "?sourceRevision=" + revisionResult.getSourceRevision() + "&targetRevision=" + revisionResult.getTargetRevision();
  }
}
