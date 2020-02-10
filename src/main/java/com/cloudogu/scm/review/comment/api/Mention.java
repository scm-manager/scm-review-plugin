package com.cloudogu.scm.review.comment.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
public class Mention {

  @NonNull
  @Size(min = 1)
  private String id;

  @Size(min = 1)
  private String display;

}
