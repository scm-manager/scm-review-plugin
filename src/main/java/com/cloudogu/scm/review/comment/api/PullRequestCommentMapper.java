package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.repository.Repository;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

import static de.otto.edison.hal.Link.link;
import static java.util.stream.Collectors.toList;

@Mapper
public abstract class PullRequestCommentMapper  {

  @Inject
  private UserDisplayManager userDisplayManager;
  @Inject
  private CommentPathBuilder commentPathBuilder;
  @Inject
  private ReplyMapper replyMapper;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "author", source = "author", qualifiedByName = "mapAuthor")
  abstract PullRequestCommentDto map(Comment pullRequestComment, @Context Repository repository, @Context String pullRequestId);

  abstract Comment map(PullRequestCommentDto pullRequestCommentDto);

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
  void appendLinks(@MappingTarget PullRequestCommentDto target, Comment source, @Context Repository repository, @Context String pullRequestId) {
    String namespace = repository.getNamespace();
    String name = repository.getName();
    final Links.Builder linksBuilder = new Links.Builder();
    linksBuilder.self(commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, target.getId()));
    if (!target.isSystemComment() && PermissionCheck.mayModifyComment(repository, source)) {
      linksBuilder.single(link("update", commentPathBuilder.createUpdateCommentUri(namespace, name, pullRequestId, target.getId())));
      if (source.getReplies().isEmpty()) {
        linksBuilder.single(link("delete", commentPathBuilder.createDeleteCommentUri(namespace, name, pullRequestId, target.getId())));
      }
    }
    target.add(linksBuilder.build());
  }

  @AfterMapping
  void appendReplies(@MappingTarget PullRequestCommentDto target, Comment source, @Context Repository repository, @Context String pullRequestId) {
    target.withEmbedded(
      "replies",
      source
        .getReplies()
        .stream()
        .map(reply -> replyMapper.map(reply, repository, pullRequestId, source))
        .collect(toList())
    );
    List<HalRepresentation> replies = target.getEmbedded().getItemsBy("replies");
    if (!replies.isEmpty()) {
      appendReplyLink((ReplyableDto) replies.get(replies.size() - 1), repository, pullRequestId, source.getId());
    } else {
      appendReplyLink(target, repository, pullRequestId, source.getId());
    }
  }

  private void appendReplyLink(ReplyableDto target, Repository repository, String pullRequestId, String commentId) {
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

  void setReplyMapper(ReplyMapper replyMapper) {
    this.replyMapper = replyMapper;
  }
}
