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
  private DisplayedUserDto author;
  private boolean shouldDeleteSourceBranch;
}
