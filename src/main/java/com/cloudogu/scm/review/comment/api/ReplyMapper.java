package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.BaseMapper;
import sonia.scm.repository.Repository;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import javax.inject.Named;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class ReplyMapper extends BaseMapper<Reply, ReplyDto> {

  @Inject
  private UserDisplayManager userDisplayManager;
  @Inject
  private CommentPathBuilder commentPathBuilder;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "author", source = "author", qualifiedByName = "mapAuthor")
  abstract ReplyDto map(Reply reply, @Context Repository repository, @Context String pullRequestId);

  abstract Reply map(ReplyDto replyDto);

  @Named("mapAuthor")
  DisplayedUserDto mapAuthor(String authorId) {
    return userDisplayManager.get(authorId).map(this::createDisplayedUserDto).orElse(new DisplayedUserDto(authorId, authorId));
  }

  String mapAuthor(DisplayedUserDto author) {
    if (author == null) {
      return null;
    } else {
      return author.getId();
    }
  }

  @AfterMapping
  void appendLinks(@MappingTarget ReplyDto target, PullRequestComment source, @Context Repository repository, @Context String pullRequestId) {
    String namespace = repository.getNamespace();
    String name = repository.getName();
    final Links.Builder linksBuilder = new Links.Builder();
    linksBuilder.self(commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, target.getId()));
    if (PermissionCheck.mayModifyComment(repository, source)) {
      linksBuilder.single(link("update", commentPathBuilder.createUpdateCommentUri(namespace, name, pullRequestId, target.getId())));
      linksBuilder.single(link("delete", commentPathBuilder.createDeleteCommentUri(namespace, name, pullRequestId, target.getId())));
    }
    target.add(linksBuilder.build());
  }

  private DisplayedUserDto createDisplayedUserDto(DisplayUser user) {
    return new DisplayedUserDto(user.getId(), user.getDisplayName());
  }
}
