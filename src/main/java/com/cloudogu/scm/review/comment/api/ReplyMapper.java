package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.repository.Repository;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class ReplyMapper {

  @Inject
  private UserDisplayManager userDisplayManager;
  @Inject
  private CommentPathBuilder commentPathBuilder;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  abstract ReplyDto map(Reply reply, @Context Repository repository, @Context String pullRequestId, @Context Comment comment);

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
  void appendLinks(@MappingTarget ReplyDto target, Reply source, @Context Repository repository, @Context String pullRequestId, @Context Comment comment) {
    String namespace = repository.getNamespace();
    String name = repository.getName();
    final Links.Builder linksBuilder = new Links.Builder();
    linksBuilder.self(commentPathBuilder.createReplySelfUri(namespace, name, pullRequestId, comment.getId(), target.getId()));
    if (PermissionCheck.mayModifyComment(repository, source)) {
      linksBuilder.single(link("update", commentPathBuilder.createUpdateReplyUri(namespace, name, pullRequestId, comment.getId(), target.getId())));
      linksBuilder.single(link("delete", commentPathBuilder.createDeleteReplyUri(namespace, name, pullRequestId, comment.getId(), target.getId())));
    }
    target.add(linksBuilder.build());
  }
}
