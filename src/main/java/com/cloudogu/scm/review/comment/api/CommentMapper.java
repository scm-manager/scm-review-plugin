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
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

import static de.otto.edison.hal.Link.link;
import static java.util.stream.Collectors.toList;

@Mapper
public abstract class CommentMapper {

  @Inject
  private UserDisplayManager userDisplayManager;
  @Inject
  private CommentPathBuilder commentPathBuilder;
  @Inject
  private ReplyMapper replyMapper;
  @Inject
  private ExecutedTransitionMapper executedTransitionMapper;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "author", source = "author", qualifiedByName = "mapAuthor")
  abstract CommentDto map(Comment pullRequestComment, @Context Repository repository, @Context String pullRequestId);

  abstract Comment map(CommentDto commentDto);

  @Named("mapAuthor")
  DisplayedUserDto mapAuthor(String authorId) {
    return new DisplayUserMapper(userDisplayManager).map(authorId);
  }

  String mapAuthor(DisplayedUserDto author) {
    if (author == null) {
      return null;
    } else {
      return author.getId();
    }
  }

  @AfterMapping
  void appendLinks(@MappingTarget CommentDto target, Comment source, @Context Repository repository, @Context String pullRequestId) {
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
  void appendReplies(@MappingTarget CommentDto target, Comment source, @Context Repository repository, @Context String pullRequestId) {
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

  @AfterMapping
  void appendTransitions(@MappingTarget CommentDto target, Comment source) {
    target.withEmbedded(
      "transitions",
      source
        .getExecutedTransitions()
        .stream()
        .map(t -> executedTransitionMapper.map(t))
        .collect(toList())
    );
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

  void setReplyMapper(ReplyMapper replyMapper) {
    this.replyMapper = replyMapper;
  }

  public void setExecutedTransitionMapper(ExecutedTransitionMapperImpl executedTransitionMapper) {
    this.executedTransitionMapper = executedTransitionMapper;
  }
}
