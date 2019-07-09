package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.otto.edison.hal.HalRepresentation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CommentDto extends ReplyableDto {

  @NonNull
  @Size(min = 1)
  private String comment;

  private String id;

  @Valid
  private DisplayedUserDto author;

  private Instant date;

  @Valid
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LocationDto location;

  private boolean systemComment;

  private String type;

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
