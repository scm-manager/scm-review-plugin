package com.cloudogu.scm.review.pullrequest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DisplayedUserDto {

  private String id;
  private String displayName;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String mail;

}
