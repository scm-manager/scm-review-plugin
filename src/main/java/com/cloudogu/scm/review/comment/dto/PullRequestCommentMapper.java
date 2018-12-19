package com.cloudogu.scm.review.comment.dto;

import com.cloudogu.scm.review.comment.service.PullRequestComment;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.BaseMapper;

import java.net.URI;
import java.util.Map;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class PullRequestCommentMapper extends BaseMapper<PullRequestComment, PullRequestCommentDto> {

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  public abstract PullRequestCommentDto map(PullRequestComment pullRequestComment, @Context Map<String, URI> resourceLinks);

  public abstract PullRequestComment map(PullRequestCommentDto pullRequestCommentDto);

  @AfterMapping
  void appendLinks(@MappingTarget PullRequestCommentDto target, @Context Map<String, URI> resourceLinks) {
    final Links.Builder linksBuilder = new Links.Builder();
    resourceLinks.forEach((s, uri) -> linksBuilder.single(link(s, uri.toString())));
    target.add(linksBuilder.build());
  }
}
