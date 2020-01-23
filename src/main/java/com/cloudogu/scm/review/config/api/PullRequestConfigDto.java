package com.cloudogu.scm.review.config.api;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class PullRequestConfigDto extends HalRepresentation {

  public PullRequestConfigDto(Links links) {
    super(links);
  }

  private boolean restrictBranchWriteAccess;
  private List<String> protectedBranchPatterns;
}
