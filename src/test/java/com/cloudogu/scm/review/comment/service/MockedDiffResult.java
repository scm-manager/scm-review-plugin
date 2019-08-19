package com.cloudogu.scm.review.comment.service;

import sonia.scm.repository.api.DiffFile;
import sonia.scm.repository.api.DiffResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MockedDiffResult implements DiffResult {

  private String oldRevision;
  private String newRevision;
  private final List<DiffFile> diffFiles = new ArrayList<>();

  @Override
  public String getOldRevision() {
    return oldRevision;
  }

  @Override
  public String getNewRevision() {
    return newRevision;
  }

  @Override
  public Iterator<DiffFile> iterator() {
    return diffFiles.iterator();
  }

  static class Builder {
    private final MockedDiffResult diffResult = new MockedDiffResult();

    Builder newRevision(String newRevision) {
      diffResult.newRevision = newRevision;
      return this;
    }

    Builder oldRevision(String oldRevision) {
      diffResult.oldRevision = oldRevision;
      return this;
    }

    Builder diffFile(DiffFile diffFile) {
      diffResult.diffFiles.add(diffFile);
      return this;
    }

    public MockedDiffResult get() {
      return diffResult;
    }
  }
}
