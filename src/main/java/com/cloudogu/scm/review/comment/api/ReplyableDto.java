package com.cloudogu.scm.review.comment.api;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;

public class ReplyableDto extends HalRepresentation {
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
