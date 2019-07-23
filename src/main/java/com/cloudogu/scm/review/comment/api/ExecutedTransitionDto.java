package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ExecutedTransitionDto extends HalRepresentation {

  private String id;

  private String transition;

  private Instant date;

  private DisplayedUserDto user;

  public ExecutedTransitionDto(Links links) {
    super(links);
  }
}
