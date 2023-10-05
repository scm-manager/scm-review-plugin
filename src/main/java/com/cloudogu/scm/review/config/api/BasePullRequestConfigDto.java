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
package com.cloudogu.scm.review.config.api;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sonia.scm.repository.api.MergeStrategy;

import java.util.List;
import java.util.Set;

@Getter @Setter @NoArgsConstructor
public class BasePullRequestConfigDto extends HalRepresentation {

  public BasePullRequestConfigDto(Links links) {
    super(links);
  }

  private MergeStrategy defaultMergeStrategy;
  private boolean deleteBranchOnMerge;
  private boolean preventMergeFromAuthor;
  private boolean restrictBranchWriteAccess;
  private List<BranchProtectionDto> protectedBranchPatterns;
  private List<String> defaultTasks;
  private List<ProtectionBypassDto> branchProtectionBypasses;
  private List<String> defaultReviewers;
  private Set<String> labels;
  private boolean overwriteDefaultCommitMessage;
  private String commitMessageTemplate;

  @Getter @Setter
  public static class BranchProtectionDto {
    private String branch;
    private String path;
  }

  @Getter @Setter
  public static class ProtectionBypassDto {
    private String name;
    private boolean group;
  }
}
