package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;

import java.util.Collection;

@Getter
public class MergeCheckResultDto extends HalRepresentation {

  public MergeCheckResultDto(Links links, boolean hasConflicts, Collection<MergeObstacle> mergeObstacles) {
    super(links);
    this.hasConflicts = hasConflicts;
    this.mergeObstacles = mergeObstacles;
  }

  private final boolean hasConflicts;
  private final Collection<MergeObstacle> mergeObstacles;
}
