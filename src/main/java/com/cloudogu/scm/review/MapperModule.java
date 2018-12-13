package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.dto.PullRequestCommentMapper;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.google.inject.AbstractModule;
import org.mapstruct.factory.Mappers;
import sonia.scm.plugin.Extension;

@Extension
public class MapperModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(PullRequestMapper.class).to(Mappers.getMapper(PullRequestMapper.class).getClass());
    bind(PullRequestCommentMapper.class).to(Mappers.getMapper(PullRequestCommentMapper.class).getClass());
  }
}
