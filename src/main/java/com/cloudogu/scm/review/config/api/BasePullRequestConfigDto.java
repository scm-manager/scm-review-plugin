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
