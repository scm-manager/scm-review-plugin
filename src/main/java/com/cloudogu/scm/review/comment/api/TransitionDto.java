package com.cloudogu.scm.review.comment.api;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter @Setter
public class TransitionDto extends HalRepresentation {

  @SuppressWarnings("squid:S2637") // name is marked as "not null", but we need this default constructor for jackson
  public TransitionDto() {
  }

  public TransitionDto(Links links, String name) {
    super(links);
    this.name = name;
  }

  @NotNull
  private String name;
}
