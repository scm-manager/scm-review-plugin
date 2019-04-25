package com.cloudogu.scm.review.comment.dto;

import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.BaseMapper;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.Map;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class PullRequestCommentMapper extends BaseMapper<PullRequestComment, PullRequestCommentDto> {

  @Inject
  private UserDisplayManager userDisplayManager;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "author", source = "author", qualifiedByName = "mapAuthor")
  public abstract PullRequestCommentDto map(PullRequestComment pullRequestComment, @Context Map<String, URI> resourceLinks);

  public abstract PullRequestComment map(PullRequestCommentDto pullRequestCommentDto);

  @Named("mapAuthor")
  DisplayedUserDto mapAuthor(String authorId) {
    return userDisplayManager.get(authorId).map(this::createDisplayedUserDto).orElse(new DisplayedUserDto(authorId, authorId));
  }

  String mapAuthor(DisplayedUserDto author) {
    return author.getId();
  }

  @AfterMapping
  void appendLinks(@MappingTarget PullRequestCommentDto target, @Context Map<String, URI> resourceLinks) {
    final Links.Builder linksBuilder = new Links.Builder();
    resourceLinks.forEach((s, uri) -> linksBuilder.single(link(s, uri.toString())));
    target.add(linksBuilder.build());
  }

  private DisplayedUserDto createDisplayedUserDto(DisplayUser user) {
    return new DisplayedUserDto(user.getId(), user.getDisplayName());
  }
}
