/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.Links;
import jakarta.inject.Inject;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.HalAppenderMapper;
import sonia.scm.repository.Repository;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;
import sonia.scm.web.EdisonHalAppender;

import java.util.Set;

import static de.otto.edison.hal.Link.link;

@Mapper
public abstract class ReplyMapper extends HalAppenderMapper {

  @Inject
  private UserDisplayManager userDisplayManager;
  @Inject
  private CommentPathBuilder commentPathBuilder;
  @Inject
  private ExecutedTransitionMapper executedTransitionMapper;
  @Inject
  private MentionMapper mentionMapper;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "mentions", source = "mentionUserIds")
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
    if (PermissionCheck.mayModifyComment(repository, source) && !source.isSystemReply()) {
      linksBuilder.single(link("update", commentPathBuilder.createUpdateReplyUri(namespace, name, pullRequestId, comment.getId(), target.getId(), revisions)));
      linksBuilder.single(link("updateWithImages", commentPathBuilder.createUpdateReplyWithImageUri(namespace, name, pullRequestId, comment.getId(), target.getId(), revisions)));
      linksBuilder.single(link("delete", commentPathBuilder.createDeleteReplyUri(namespace, name, pullRequestId, comment.getId(), target.getId(), revisions)));
    }
    applyEnrichers(new EdisonHalAppender(linksBuilder, new Embedded.Builder()), source, repository);
    target.add(linksBuilder.build());
  }

  Set<DisplayUser> appendMentions(Set<String> userIds) {
    return mentionMapper.mapMentions(userIds);
  }

  @AfterMapping
  void appendTransitions(@MappingTarget ReplyDto target, Reply source, @Context Repository repository, @Context String pullRequestId) {
    executedTransitionMapper.appendTransitions(target, source, repository.getNamespaceAndName(), pullRequestId);
  }

  void setExecutedTransitionMapper(ExecutedTransitionMapperImpl executedTransitionMapper) {
    this.executedTransitionMapper = executedTransitionMapper;
  }
}
