package com.cloudogu.scm.review.comment.dto;

import com.cloudogu.scm.review.comment.service.PullRequestComment;
import de.otto.edison.hal.Links;
import org.apache.shiro.SecurityUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.BaseMapper;

import java.net.URI;

import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;

@Mapper
public abstract class PullRequestCommentMapper extends BaseMapper<PullRequestComment, PullRequestCommentDto> {

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  public abstract PullRequestCommentDto map(PullRequestComment pullRequestComment, @Context URI location);

  public abstract PullRequestComment map(PullRequestCommentDto pullRequestCommentDto);

  @AfterMapping
  void appendLinks(@MappingTarget PullRequestCommentDto target, @Context URI location) {
    Links.Builder linksBuilder = linkingTo().self(location.toString());
    String currentUser = SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
    if (currentUser.equals(target.getAuthor())) {
      linksBuilder.single(link("update", location.toString()));
      linksBuilder.single(link("delete", location.toString()));
    }
    target.add(linksBuilder.build());
  }
}
