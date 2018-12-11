package com.cloudogu.scm.review.api;

import com.cloudogu.scm.review.service.PullRequest;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.BaseMapper;

import java.net.URI;

import static de.otto.edison.hal.Links.linkingTo;

@Mapper
public abstract class PullRequestToPullRequestDtoMapper extends BaseMapper<PullRequest, PullRequestDto> {

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  public abstract PullRequestDto map(PullRequest pullRequest, @Context URI location);

  public abstract PullRequest map(PullRequestDto pullRequestDto);

  @AfterMapping
  void appendLinks(@MappingTarget PullRequestDto target, @Context URI location) {
    Links.Builder linksBuilder = linkingTo()
      .self(location.toString())
      .single(Link.link("reject", location.toString() + "/reject"));
    target.add(linksBuilder.build());
  }
}
