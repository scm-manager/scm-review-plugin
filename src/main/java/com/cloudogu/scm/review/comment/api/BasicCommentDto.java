package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import sonia.scm.user.DisplayUser;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Getter
@Setter
abstract class BasicCommentDto extends HalRepresentation {

  @NonNull
  @Size(min = 1)
  private String comment;

  private String id;

  @Valid
  private DisplayedUserDto author;

  private Set<DisplayedUserDto> mentions;

  private Instant date;

  /**
   * suppress squid:S1185 (Overriding methods should do more than simply call the same method in the super class)
   * because we want to have this method available in this package
   */
  @SuppressWarnings("squid:S1185")
  @Override
  protected HalRepresentation add(Links links) {
    return super.add(links);
  }

  /**
   * suppress squid:S1185 (Overriding methods should do more than simply call the same method in the super class)
   * because we want to have this method available in this package
   */
  @SuppressWarnings("squid:S1185")
  @Override
  protected HalRepresentation withEmbedded(String rel, List<? extends HalRepresentation> embeddedItems) {
    return super.withEmbedded(rel, embeddedItems);
  }
}
