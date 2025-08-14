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
