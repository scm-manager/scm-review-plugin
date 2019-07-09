package com.cloudogu.scm.review.comment.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;


@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CommentDto extends BasicCommentDto {

  @Valid
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LocationDto location;

  private boolean systemComment;

  private String type;
}
