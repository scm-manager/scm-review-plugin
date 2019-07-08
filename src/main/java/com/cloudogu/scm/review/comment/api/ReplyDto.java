package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReplyDto extends ReplyableDto {

  @NonNull
  @Size(min = 1)
  private String comment;

  private String id;

  @Valid
  private DisplayedUserDto author;

  private Instant date;
}
