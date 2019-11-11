package com.cloudogu.scm.review.pullrequest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MergeCommitDto {
  private String commitMessage;
  private String source;
  private String target;
  private DisplayedUserDto author;
  private boolean shouldDeleteSourceBranch;
}
