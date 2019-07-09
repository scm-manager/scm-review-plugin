package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.api.CommentMapper;
import com.cloudogu.scm.review.comment.api.ExecutedTransitionMapper;
import com.cloudogu.scm.review.comment.api.ReplyMapper;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.google.inject.AbstractModule;
import org.mapstruct.factory.Mappers;
import sonia.scm.plugin.Extension;

@Extension
public class ModuleBinder extends AbstractModule {

  @Override
  protected void configure() {
    bind(PullRequestMapper.class).to(Mappers.getMapper(PullRequestMapper.class).getClass());
    bind(CommentMapper.class).to(Mappers.getMapper(CommentMapper.class).getClass());
    bind(ReplyMapper.class).to(Mappers.getMapper(ReplyMapper.class).getClass());
    bind(ExecutedTransitionMapper.class).to(Mappers.getMapper(ExecutedTransitionMapper.class).getClass());
    bind(PullRequestService.class).to(DefaultPullRequestService.class);
  }
}
