package com.cloudogu.scm.review.comment.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Location implements Cloneable {

  private String file;
  private String hunk;
  private Integer oldLineNumber;
  private Integer newLineNumber;

}
