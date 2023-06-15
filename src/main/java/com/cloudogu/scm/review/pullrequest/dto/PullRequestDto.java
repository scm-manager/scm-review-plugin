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
package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("squid:S2160")
public class PullRequestDto extends HalRepresentation {
  private String id;
  private DisplayedUserDto author;
  private DisplayedUserDto reviser;
  private Instant closeDate;
  @NotBlank
  private String source;
  @NotBlank
  private String target;
  @NotBlank
  private String title;
  private String description;
  private Instant creationDate;
  private Instant lastModified;
  private PullRequestStatus status;
  private Set<ReviewerDto> reviewer = new HashSet<>();
  private Set<String> labels = new HashSet<>();
  private TasksDto tasks;
  private String sourceRevision;
  private String targetRevision;
  private Collection<String> markedAsReviewed;
  private boolean emergencyMerged;
  private List<String> ignoredMergeObstacles;
  private List<String> initialTasks = new ArrayList<>();

  public PullRequestDto(Links links, Embedded embedded) {
    super(links, embedded);
  }
}
