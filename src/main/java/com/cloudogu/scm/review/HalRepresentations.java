package com.cloudogu.scm.review;

import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import sonia.scm.repository.RepositoryPermissions;

import javax.ws.rs.core.UriInfo;
import java.util.List;

public class HalRepresentations {

  public static HalRepresentation createCollection(UriInfo uriInfo, String repositoryId, List<? extends HalRepresentation> dtoList, String attributeName) {
    String href = uriInfo.getAbsolutePath().toASCIIString();

    Links.Builder builder = Links.linkingTo().self(href);

    if (RepositoryPermissions.push(repositoryId).isPermitted()) {
      builder.single(Link.link("create", href));
    }

    return new HalRepresentation(builder.build(), Embedded.embedded(attributeName, dtoList));
  }

}
