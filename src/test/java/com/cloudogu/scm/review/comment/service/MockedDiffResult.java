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
