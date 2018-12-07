package com.cloudogu.scm.review.comment.dto;

import com.cloudogu.scm.review.comment.service.PullRequestComment;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.BaseMapper;

import java.net.URI;

import static de.otto.edison.hal.Links.linkingTo;

@Mapper
public abstract class PullRequestCommentMapper extends BaseMapper<PullRequestComment, PullRequestCommentDto> {


  public abstract PullRequestComment map(PullRequestCommentDto pullRequestCommentDto) ;

  @AfterMapping
  void appendLinks(@MappingTarget PullRequestCommentDto target, @Context URI location) {
    Links.Builder linksBuilder = linkingTo()
      .self(location.toString());
    target.add(linksBuilder.build());
  }
}
