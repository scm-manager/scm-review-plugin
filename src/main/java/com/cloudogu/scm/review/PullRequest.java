package com.cloudogu.scm.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "pull-request")
@XmlAccessorType(XmlAccessType.FIELD)
public class PullRequest {

  @NotNull @Size(min = 1)
  private String source;
  @NotNull @Size(min = 1)
  private String target;
  @NotNull @Size(min = 1)
  private String title;
  private String description;
  @Setter
  private String author;
  @Setter
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant creationDate;
}
