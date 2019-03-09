package com.cloudogu.scm.review.comment.dto;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.Size;
import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class PullRequestCommentDto extends HalRepresentation {

  @NonNull
  @Size(min = 1)
  private String comment;

  private String author;

  private Instant date;

  private LocationDto location;

  /**
   * suppress squid:S1185 (Overriding methods should do more than simply call the same method in the super class)
   * because we want to have this method available in this package
   */
  @SuppressWarnings("squid:S1185")
  @Override
  protected HalRepresentation add(Links links) {
    return super.add(links);
  }
}
