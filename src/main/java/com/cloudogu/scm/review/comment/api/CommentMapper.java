package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.ContextLine;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.google.common.base.Strings;
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
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

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
  @Inject
  private PossibleTransitionMapper possibleTransitionMapper;
  @Inject
  private MentionMapper mentionMapper;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "author", source = "author", qualifiedByName = "mapAuthor")
  @Mapping(target = "mentions", source = "mentionUserIds", qualifiedByName = "mapMentions")
  abstract CommentDto map(Comment pullRequestComment, @Context Repository repository, @Context String pullRequestId, @Context Collection<CommentTransition> possibleTransitions, @Context BranchRevisionResolver.RevisionResult revisions);

  @Mapping(target = "mentionUserIds", ignore = true)
  abstract Comment map(CommentDto commentDto);

  abstract CommentDto.ContextLineDto map(ContextLine line);

  abstract ContextLine map(CommentDto.ContextLineDto line);

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
  void appendLinks(@MappingTarget CommentDto target, Comment source, @Context Repository repository, @Context String pullRequestId, @Context BranchRevisionResolver.RevisionResult revisions) {
    String namespace = repository.getNamespace();
    String name = repository.getName();
    final Links.Builder linksBuilder = new Links.Builder();
    linksBuilder.self(commentPathBuilder.createCommentSelfUri(namespace, name, pullRequestId, target.getId()));
    if (!target.isSystemComment() && PermissionCheck.mayModifyComment(repository, source)) {
      linksBuilder.single(link("update", commentPathBuilder.createUpdateCommentUri(namespace, name, pullRequestId, target.getId(), revisions)));
      linksBuilder.single(link("possibleTransitions", commentPathBuilder.createPossibleTransitionUri(namespace, name, pullRequestId, target.getId())));
      if (source.getReplies().isEmpty()) {
        linksBuilder.single(link("delete", commentPathBuilder.createDeleteCommentUri(namespace, name, pullRequestId, target.getId(), revisions)));
      }
    }
    target.add(linksBuilder.build());
  }

  @AfterMapping
  void appendReplies(@MappingTarget CommentDto target, Comment source, @Context Repository repository, @Context String pullRequestId, @Context BranchRevisionResolver.RevisionResult revisions) {
    target.withEmbedded(
      "replies",
      source
        .getReplies()
        .stream()
        .map(reply -> replyMapper.map(reply, repository, pullRequestId, source, revisions))
        .collect(toList())
    );
    if (!source.getType().equals(CommentType.TASK_DONE)) {
      appendReplyLink(target, repository, pullRequestId, source.getId(), revisions);
    }
  }

  @AfterMapping
  void appendTransitions(@MappingTarget CommentDto target, Comment source, @Context Repository repository, @Context String pullRequestId) {
    executedTransitionMapper.appendTransitions(target, source, repository.getNamespaceAndName(), pullRequestId);
  }

  @AfterMapping
  void appendPossibleTransitions(@MappingTarget CommentDto target, Comment source, @Context Repository repository, @Context String pullRequestId, @Context Collection<CommentTransition> possibleTransitions) {
    possibleTransitionMapper.appendTransitions(target, possibleTransitions, repository.getNamespace(), repository.getName(), pullRequestId, source.getId());
  }

  @Named("mapMentions")
  Set<DisplayUser> appendMentions(Set<String> userIds) {
    return mentionMapper.mapMentions(userIds);
  }

  Integer mapOptional(OptionalInt optionalInt) {
    return optionalInt.isPresent() ? optionalInt.getAsInt() : null;
  }

  private void appendReplyLink(BasicCommentDto target, Repository repository, String pullRequestId, String commentId, BranchRevisionResolver.RevisionResult revisions) {
    String namespace = repository.getNamespace();
    String name = repository.getName();
    final Links.Builder linksBuilder = new Links.Builder();
    if (PermissionCheck.mayComment(repository)) {
      linksBuilder.single(link("reply", commentPathBuilder.createReplyCommentUri(namespace, name, pullRequestId, commentId, revisions)));
    }
    target.add(linksBuilder.build());
  }

  void setReplyMapper(ReplyMapper replyMapper) {
    this.replyMapper = replyMapper;
  }

  public void setExecutedTransitionMapper(ExecutedTransitionMapperImpl executedTransitionMapper) {
    this.executedTransitionMapper = executedTransitionMapper;
  }

  public void setPossibleTransitionMapper(PossibleTransitionMapper possibleTransitionMapper) {
    this.possibleTransitionMapper = possibleTransitionMapper;
  }

  public void setMentionMapper(MentionMapper mentionMapper) {
    this.mentionMapper = mentionMapper;
  }
}
