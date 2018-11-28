package com.cloudogu.scm.review;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("squid:S2160")
public class PullRequestDto extends HalRepresentation {
  private String id;
  private String author;
  private String source;
  private String target;
  private String title;
  private String description;
  private Instant creationDate;

  @Override
  @SuppressWarnings("squid:S1185") // We want to have this method available in this package
  protected HalRepresentation add(Links links) {
    return super.add(links);
  }

}
