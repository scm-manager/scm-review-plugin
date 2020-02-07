package com.cloudogu.scm.review.comment.api;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.Size;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Mention {

  @NonNull
  @Size(min = 1)
  private String id;

  @Size(min = 1)
  private String display;

}
