/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review;

import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import de.otto.edison.hal.paging.NumberedPaging;
import de.otto.edison.hal.paging.PagingRel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;

import static com.damnhandy.uri.template.UriTemplate.fromTemplate;
import static de.otto.edison.hal.Links.linkingTo;

public class PagedCollections {

  private PagedCollections() {
  }

  public static PagedCollectionDto createPagedCollection(HalRepresentation halRepresentation,
                                                         int page,
                                                         int pageTotal) {
    PagedCollectionDto pagedCollection = new PagedCollectionDto(halRepresentation);
    pagedCollection.setPage(page);
    pagedCollection.setPageTotal(pageTotal);
    return pagedCollection;
  }

  public static Links.Builder createPagedSelfLinks(NumberedPaging page, String selfLink) {
    return linkingTo()
      .with(page.links(
        fromTemplate(selfLink + "{?page,pageSize}"),
        EnumSet.allOf(PagingRel.class)));
  }

  @Getter
  @Setter
  @EqualsAndHashCode(callSuper = true)
  static class PagedCollectionDto extends HalRepresentation {
    private int page;
    private int pageTotal;

    PagedCollectionDto(HalRepresentation halRepresentation) {
      super(halRepresentation.getLinks(), halRepresentation.getEmbedded());
    }
  }
}
