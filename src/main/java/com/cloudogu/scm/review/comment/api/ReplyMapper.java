/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.Links;
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

import jakarta.inject.Inject;
import jakarta.inject.Named;
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
    if (PermissionCheck.mayModifyComment(repository, source) && !source.isSystemReply()) {
      linksBuilder.single(link("update", commentPathBuilder.createUpdateReplyUri(namespace, name, pullRequestId, comment.getId(), target.getId(), revisions)));
      linksBuilder.single(link("delete", commentPathBuilder.createDeleteReplyUri(namespace, name, pullRequestId, comment.getId(), target.getId(), revisions)));
    }
    applyEnrichers(new EdisonHalAppender(linksBuilder, new Embedded.Builder()), source, repository);
    target.add(linksBuilder.build());
  }

  @Named("mapMentions")
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
