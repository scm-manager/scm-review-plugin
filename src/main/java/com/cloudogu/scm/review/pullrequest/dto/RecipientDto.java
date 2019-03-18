package com.cloudogu.scm.review.pullrequest.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecipientDto {

  private String name;
  private String email;
}
