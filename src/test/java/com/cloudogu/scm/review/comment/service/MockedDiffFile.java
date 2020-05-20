/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
