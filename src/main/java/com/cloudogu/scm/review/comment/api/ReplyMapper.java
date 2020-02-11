package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.google.common.base.Strings;
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

import java.util.Set;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class ReplyMapper {

  @Inject
  private UserDisplayManager userDisplayManager;
  @Inject
  private CommentPathBuilder commentPathBuilder;
  @Inject
  private ExecutedTransitionMapper executedTransitionMapper;
  @Inject
  private MentionMapper mentionMapper;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "mentions", source = "mentionUserIds", qualifiedByName = "mapMentions")
  abstract ReplyDto map(Reply reply, @Context Repository repository, @Context String pullRequestId, @Context Comment comment, @Context BranchRevisionResolver.RevisionResult revisions);

  @Mapping(target = "mentionUserIds", ignore = true)
  abstract Reply map(ReplyDto replyDto);

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
  void appendLinks(@MappingTarget ReplyDto target, Reply source, @Context Repository repository, @Context String pullRequestId, @Context Comment comment, @Context BranchRevisionResolver.RevisionResult revisions) {
    String namespace = repository.getNamespace();
    String name = repository.getName();
    final Links.Builder linksBuilder = new Links.Builder();
    linksBuilder.self(commentPathBuilder.createReplySelfUri(namespace, name, pullRequestId, comment.getId(), target.getId()));
    if (PermissionCheck.mayModifyComment(repository, source)) {
      linksBuilder.single(link("update", commentPathBuilder.createUpdateReplyUri(namespace, name, pullRequestId, comment.getId(), target.getId(), revisions)));
      linksBuilder.single(link("delete", commentPathBuilder.createDeleteReplyUri(namespace, name, pullRequestId, comment.getId(), target.getId(), revisions)));
    }
    target.add(linksBuilder.build());
  }

  @Named("mapMentions")
  Set<DisplayUser> appendMentions(Set<String> userIds) {
    return mentionMapper.mapMentions(userIds);
  }

  @AfterMapping
  void parseMentions(@MappingTarget Reply reply, ReplyDto dto) {
    if (mentionMapper != null && !Strings.isNullOrEmpty(dto.getComment())) {
      reply.setMentionUserIds(mentionMapper.extractMentionsFromComment(dto.getComment()));
    }
  }

  @AfterMapping
  void appendTransitions(@MappingTarget ReplyDto target, Reply source, @Context Repository repository, @Context String pullRequestId) {
    executedTransitionMapper.appendTransitions(target, source, repository.getNamespaceAndName(), pullRequestId);
  }

  void setExecutedTransitionMapper(ExecutedTransitionMapperImpl executedTransitionMapper) {
    this.executedTransitionMapper = executedTransitionMapper;
  }
}
