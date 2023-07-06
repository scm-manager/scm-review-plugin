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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sonia.scm.repository.api.MergeStrategy;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class BasePullRequestConfig {
  private MergeStrategy defaultMergeStrategy = MergeStrategy.MERGE_COMMIT;
  private boolean deleteBranchOnMerge = false;
  private boolean restrictBranchWriteAccess = false;
  @XmlElement(name = "protected-branch-patterns")
  private List<String> protectedBranchPatterns = new ArrayList<>();
  private List<String> defaultTasks = new ArrayList<>();
  @XmlElement(name = "protection-bypasses")
  private List<BasePullRequestConfig.ProtectionBypass> branchProtectionBypasses = new ArrayList<>();
  private boolean preventMergeFromAuthor = false;
  private List<String> defaultReviewers = new ArrayList<>();
  private Set<String> labels = new HashSet<>();
  private boolean overwriteDefaultCommitMessage;
  private String commitMessageTemplate;

  @Getter
  @Setter
  @XmlRootElement(name = "bypass")
  @XmlAccessorType(XmlAccessType.FIELD)
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProtectionBypass {
    private String name;
    private boolean group;
  }
}
