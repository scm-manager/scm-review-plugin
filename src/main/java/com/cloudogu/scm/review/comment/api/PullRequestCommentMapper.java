package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.comment.service.PullRequestComment;
import com.cloudogu.scm.review.comment.service.PullRequestRootComment;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import de.otto.edison.hal.HalRepresentation;
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
import java.util.List;

import static de.otto.edison.hal.Link.link;
import static java.util.stream.Collectors.toList;

@Mapper
public abstract class PullRequestCommentMapper extends BaseMapper<PullRequestRootComment, PullRequestCommentDto> {

  @Inject
  private UserDisplayManager userDisplayManager;
  @Inject
  private CommentPathBuilder commentPathBuilder;


  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "author", source = "author", qualifiedByName = "mapAuthor")
  abstract PullRequestCommentDto map(PullRequestRootComment pullRequestComment, @Context Repository repository, @Context String pullRequestId);

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "author", source = "author", qualifiedByName = "mapAuthor")
  abstract PullRequestCommentDto map(PullRequestComment pullRequestComment, @Context Repository repository, @Context String pullRequestId);

  abstract PullRequestRootComment map(PullRequestCommentDto pullRequestCommentDto);

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
  void appendLinks(@MappingTarget PullRequestCommentDto target, PullRequestComment source, @Context Repository repository, @Context String pullRequestId) {
    String namespace = repository.getNamespace();
    String name = repository.getName();
    final Links.Builder linksBuilder = new Links.Builder();
    linksBuilder.self(commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, target.getId()));
    if (!target.isSystemComment() && PermissionCheck.mayModifyComment(repository, source)) {
      linksBuilder.single(link("update", commentPathBuilder.createUpdateCommentUri(namespace, name, pullRequestId, target.getId())));
      if (!(source instanceof PullRequestRootComment) || ((PullRequestRootComment)source).getReplies().isEmpty()) {
        linksBuilder.single(link("delete", commentPathBuilder.createDeleteCommentUri(namespace, name, pullRequestId, target.getId())));
      }
    }
    target.add(linksBuilder.build());
  }

  @AfterMapping
  void appendReplies(@MappingTarget PullRequestCommentDto target, PullRequestRootComment source, @Context Repository repository, @Context String pullRequestId) {
    target.withEmbedded(
      "replies",
      source
        .getReplies()
        .stream()
        .map(reply -> this.map(reply, repository, pullRequestId))
        .collect(toList())
    );
    List<HalRepresentation> replies = target.getEmbedded().getItemsBy("replies");
    if (!replies.isEmpty()) {
      appendReplyLink((PullRequestCommentDto) replies.get(replies.size() - 1), repository, pullRequestId, source.getId());
    } else {
      appendReplyLink(target, repository, pullRequestId, source.getId());
    }
  }

  private void appendReplyLink(PullRequestCommentDto target, Repository repository, String pullRequestId, String commentId) {
    String namespace = repository.getNamespace();
    String name = repository.getName();
    final Links.Builder linksBuilder = new Links.Builder();
    if (PermissionCheck.mayComment(repository)) {
      linksBuilder.single(link("reply", commentPathBuilder.createReplyCommentUri(namespace, name, pullRequestId, commentId)));
    }
    target.add(linksBuilder.build());
  }

  private DisplayedUserDto createDisplayedUserDto(DisplayUser user) {
    return new DisplayedUserDto(user.getId(), user.getDisplayName());
  }
}
