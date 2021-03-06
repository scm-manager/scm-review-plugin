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
package com.cloudogu.scm.review.config.service;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "config")
public class PullRequestConfig {

  private boolean restrictBranchWriteAccess = false;
  @XmlElement(name = "protected-branch-patterns")
  private List<String> protectedBranchPatterns = new ArrayList<>();
  private boolean preventMergeFromAuthor = false;

  public boolean isRestrictBranchWriteAccess() {
    return restrictBranchWriteAccess;
  }

  public void setRestrictBranchWriteAccess(boolean restrictBranchWriteAccess) {
    this.restrictBranchWriteAccess = restrictBranchWriteAccess;
  }

  public List<String> getProtectedBranchPatterns() {
    return protectedBranchPatterns;
  }

  public void setProtectedBranchPatterns(List<String> protectedBranchPatterns) {
    this.protectedBranchPatterns = protectedBranchPatterns;
  }

  public boolean isPreventMergeFromAuthor() {
    return preventMergeFromAuthor;
  }

  public void setPreventMergeFromAuthor(boolean preventMergeFromAuthor) {
    this.preventMergeFromAuthor = preventMergeFromAuthor;
  }
}
