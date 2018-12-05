package com.cloudogu.scm.review;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

@Getter
@Setter
@RequiredArgsConstructor
@XmlRootElement(name = "pull-request")
@XmlAccessorType(XmlAccessType.FIELD)
public class PullRequest {

  private String id;
  @NonNull @Size(min = 1)
  private String source;
  @NonNull @Size(min = 1)
  private String target;
  @NonNull @Size(min = 1)
  private String title;
  private String description;
  private String author;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant creationDate;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant lastModified;
  private PullRequestStatus status;

  public PullRequest() {
  }
}
