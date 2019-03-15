package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import lombok.Getter;
import lombok.Setter;
import sonia.scm.repository.Repository;

import java.util.List;

@Getter
@Setter
public class EmailContext {

  private Repository repository;
  private PullRequest pullRequest;
  private PullRequest oldPullRequest;
  private PullRequestComment comment;
  private PullRequestComment oldComment;


  private List<String> receivers;
  private String sender;
  private String subject;

  private String content;

}
