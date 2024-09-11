/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

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
  public ChangeType getChangeType() {
    return ChangeType.RENAME;
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
