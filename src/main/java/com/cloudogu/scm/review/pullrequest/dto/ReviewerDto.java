package com.cloudogu.scm.review.pullrequest.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.AllArgsConstructor;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class ReviewerDto extends DisplayedUserDto {
  private boolean approved;

  public ReviewerDto(String id, String displayName, String mail, boolean approved) {
    super(id, displayName, mail);
    this.approved = approved;
  }
}
