package com.cloudogu.scm.review.comment.service;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Location {

  private String file;
  private String hunk;
  private Integer oldLineNumber;
  private Integer newLineNumber;

}
