package com.cloudogu.scm.review;

import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

@Getter
@RequiredArgsConstructor
@XmlRootElement(name = "pull-request")
@XmlAccessorType(XmlAccessType.FIELD)
public class PullRequest {

  @Size(min = 1)
  @Setter
  private String id;
  @NotNull @NonNull @Size(min = 1)
  private String source;
  @NotNull @NonNull @Size(min = 1)
  private String target;
  @NotNull @NonNull @Size(min = 1)
  private String title;
  @Setter
  private String description;
  @Setter
  private String author;
  @Setter
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant creationDate;
  @Setter
  private Status status;

  public enum Status {
    OPEN,
    MERGED,
    REJECTED
  }

  public PullRequest() {
    status = Status.OPEN;
  }
}
