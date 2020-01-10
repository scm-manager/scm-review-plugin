package com.cloudogu.scm.review.pullrequest.dto;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import sonia.scm.repository.spi.MergeConflictResult;

import java.util.List;

@Getter
public class MergeConflictResultDto extends HalRepresentation {

  public MergeConflictResultDto(Links links, List<MergeConflictResult.SingleMergeConflict> conflicts) {
    super(links);
    this.conflicts = conflicts;
  }

  private final List<MergeConflictResult.SingleMergeConflict> conflicts;
}
