package com.cloudogu.scm.review;

import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;

import javax.ws.rs.core.UriInfo;
import java.util.List;

public class HalRepresentations {

  private HalRepresentations() {
  }

  public static HalRepresentation createCollection(UriInfo uriInfo, boolean permittedToCreate, List<? extends HalRepresentation> dtoList, String attributeName) {
    String href = uriInfo.getAbsolutePath().toASCIIString();

    Links.Builder builder = Links.linkingTo().self(href);

    if (permittedToCreate) {
      builder.single(Link.link("create", href));
    }

    return new HalRepresentation(builder.build(), Embedded.embedded(attributeName, dtoList));
  }

}
