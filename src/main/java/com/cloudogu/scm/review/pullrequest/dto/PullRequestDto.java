package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("squid:S2160")
public class PullRequestDto extends HalRepresentation {
  private String id;
  private String author;
  @NonNull @Size(min = 1)
  private String source;
  @NonNull @Size(min = 1)
  private String target;
  @NonNull @Size(min = 1)
  private String title;
  private String description;
  private Instant creationDate;
  private Instant lastModified;
  private PullRequestStatus status;
  private List<String> subscriber;

  @Override
  @SuppressWarnings("squid:S1185") // We want to have this method available in this package
  protected HalRepresentation add(Links links) {
    return super.add(links);
  }

}
