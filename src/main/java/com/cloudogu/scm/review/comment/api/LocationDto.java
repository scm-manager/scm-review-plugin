package com.cloudogu.scm.review.comment.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class LocationDto {

  @NonNull
  @Size(min = 1)
  private String file;

  @Size(min = 1)
  private String hunk;

  private Integer oldLineNumber;

  private Integer newLineNumber;

}