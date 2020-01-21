package com.cloudogu.scm.review.config.api;

import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GlobalPullRequestConfigDto extends PullRequestConfigDto {

  public GlobalPullRequestConfigDto(Links links) {
    super(links);
  }

  private boolean disableRepositoryConfiguration;
}
