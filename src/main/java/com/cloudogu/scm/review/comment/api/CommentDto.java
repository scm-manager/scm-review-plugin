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

package com.cloudogu.scm.review.comment.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;


@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CommentDto extends BasicCommentDto {

  @Valid
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LocationDto location;

  private boolean systemComment;
  private boolean outdated;
  private boolean emergencyMerged;

  private String type;

  private Map<String, String> systemCommentParameters;

  private InlineContextDto context;

  @Getter
  @Setter
  static class InlineContextDto {
    private List<ContextLineDto> lines;
  }

  @Getter
  @Setter
  static class ContextLineDto {
    private Integer oldLineNumber;
    private Integer newLineNumber;
    private String content;
  }
}
