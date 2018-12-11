package com.cloudogu.scm.review.comment.dto;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.Size;
import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("squid:S2160")
public class PullRequestCommentDto extends HalRepresentation {

  @NonNull
  @Size(min = 1)
  @SuppressWarnings("squid:S1700")
  private String comment;

  private String author;

  private Instant date;

  @Override
  @SuppressWarnings("squid:S1185") // We want to have this method available in this package
  protected HalRepresentation add(Links links) {
    return super.add(links);
  }
}
