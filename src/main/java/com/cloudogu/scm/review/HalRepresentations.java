package com.cloudogu.scm.review;

import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;

import java.util.List;

public class HalRepresentations {

  private HalRepresentations() {
  }

  public static HalRepresentation createCollection(
    boolean permittedToCreate,
    String selfLink,
    String createLink,
    List<? extends HalRepresentation> dtoList, String attributeName
  ) {
    Links.Builder builder = Links.linkingTo().self(selfLink);

    if (permittedToCreate) {
      builder.single(Link.link("create", createLink));
    }

    return new HalRepresentation(builder.build(), Embedded.embedded(attributeName, dtoList));
  }

}
