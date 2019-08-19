package com.cloudogu.scm.review.comment.service;

import sonia.scm.repository.api.DiffFile;
import sonia.scm.repository.api.Hunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class MockedDiffFile implements DiffFile {

  private String oldRevision;
  private String newRevision;
  private String oldPath;
  private String newPath;
  private Collection<Hunk> hunks = new ArrayList<>();

  @Override
  public String getOldRevision() {
    return oldRevision;
  }

  @Override
  public String getNewRevision() {
    return newRevision;
  }

  @Override
  public String getOldPath() {
    return oldPath;
  }

  @Override
  public String getNewPath() {
    return newPath;
  }

  @Override
  public Iterator<Hunk> iterator() {
    return hunks.iterator();
  }

  static class Builder {
    private final MockedDiffFile diffFile = new MockedDiffFile();

    Builder oldRevision(String oldRevision) {
      diffFile.oldRevision = oldRevision;
      return this;
    }

    Builder newRevision(String newRevision) {
      diffFile.newRevision = newRevision;
      return this;
    }

    Builder oldPath(String oldPath) {
      diffFile.oldPath = oldPath;
      return this;
    }

    Builder newPath(String newPath) {
      diffFile.newPath = newPath;
      return this;
    }

    Builder addHunk(Hunk hook) {
      diffFile.hunks.add(hook);
      return this;
    }

    MockedDiffFile get() {
      return diffFile;
    }
  }
}
