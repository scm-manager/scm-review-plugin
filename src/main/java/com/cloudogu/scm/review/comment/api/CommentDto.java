package com.cloudogu.scm.review.comment.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CommentDto extends BasicCommentDto {

  @Valid
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LocationDto location;

  private boolean systemComment;
  private boolean outdated;

  private String type;

  private InlineContextDto context;

  @Getter
  @Setter
  static class InlineContextDto {
    private List<ContextLineDto> lines;
  }

  @Getter
  @Setter
  static class ContextLineDto {
    private Integer oldLineNumber;
    private Integer newLineNumber;
    private String content;
  }
}
