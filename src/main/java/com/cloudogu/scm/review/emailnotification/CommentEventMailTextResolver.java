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

package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.comment.service.BasicComment;
import com.cloudogu.scm.review.comment.service.BasicCommentEvent;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.ExecutedTransition;
import com.cloudogu.scm.review.comment.service.Transition;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.mail.api.Topic;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class CommentEventMailTextResolver extends BasicPRMailTextResolver<BasicCommentEvent<?>> implements MailTextResolver {

  private final BasicCommentEvent<?> commentEvent;
  private final CommentEventType commentEventType;

  public CommentEventMailTextResolver(BasicCommentEvent<?> commentEvent) {
    this.commentEvent = commentEvent;
    this.commentEventType = getCommentEventType(commentEvent);
  }

  private static CommentEventType getCommentEventType(BasicCommentEvent<?> commentEvent) {
    switch (commentEvent.getEventType()) {
      case CREATE:
        return CommentEventType.COMMENT_CREATED;
      case DELETE:
        return CommentEventType.COMMENT_DELETED;
      case MODIFY:
        if (!commentEvent.getItem().getComment().equals(commentEvent.getOldItem().getComment())) {
          return CommentEventType.TEXT_MODIFIED;
        } else {
          return getCommentModificationEventType(commentEvent.getItem());
        }
      default:
        log.warn("the event " + commentEvent.getEventType() + " is not supported for the Mail Renderer ");
        return null;
    }
  }

  private static CommentEventType getCommentModificationEventType(BasicComment newComment) {
    List<ExecutedTransition> transitions = newComment.getExecutedTransitions();
    Transition lastTransition = transitions.get(transitions.size() - 1).getTransition();
    if (lastTransition == CommentTransition.MAKE_TASK) {
      return CommentEventType.TASK_CREATED;
    } else if (lastTransition == CommentTransition.SET_DONE) {
      return CommentEventType.TASK_DONE;
    } else if (lastTransition == CommentTransition.REOPEN) {
      return CommentEventType.TASK_REOPEN;
    } else if (lastTransition == CommentTransition.MAKE_COMMENT) {
      return CommentEventType.COMMENT_CREATED;
    } else {
      log.trace("cannot handle changes of comment");
      return null;
    }
  }

  @Override
  public String getMailSubject(Locale locale) {
    return getMailSubject(commentEvent, commentEventType.getDisplayEventName(), locale);
  }

  @Override
  public String getContentTemplatePath() {
    return commentEventType.getTemplatePath();
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath) {
    Map<String, Object> model = super.getTemplateModel(basePath, commentEvent);

    switch (commentEventType) {
      case COMMENT_DELETED:
        model.put("oldComment", commentEvent.getOldItem());
        break;
      case COMMENT_CREATED:
      case TASK_CREATED:
      case TASK_REOPEN:
      case TASK_DONE:
        model.put("comment", commentEvent.getItem());
        break;
      case TEXT_MODIFIED:
        model.put("oldComment", commentEvent.getOldItem());
        model.put("comment", commentEvent.getItem());
        break;
      default:
        // no more values needed
    }
    return model;
  }

  @Override
  public Topic getTopic() {
    return TOPIC_COMMENTS;
  }

  @Override
  public String getPullRequestId() {
    return this.commentEvent.getPullRequest().getId();
  }

  private enum CommentEventType {
    COMMENT_CREATED("created_comment.mustache", "commentAdded"),
    COMMENT_DELETED("deleted_comment.mustache", "commentDeleted"),
    TEXT_MODIFIED("modified_comment.mustache", "commentChanged"),
    TASK_DONE("task_done.mustache", "taskDone"),
    TASK_REOPEN("task_reopened.mustache", "taskReopened"),
    TASK_CREATED("task_created.mustache", "taskCreated");

    protected static final String PATH_BASE = "com/cloudogu/scm/email/template/";

    private final String template;
    private final String displayEventName;

    CommentEventType(String template, String displayEventName) {
      this.template = template;
      this.displayEventName = displayEventName;
    }

    public String getTemplatePath() {
      return PATH_BASE + template;
    }

    public String getDisplayEventName() {
      return displayEventName;
    }
  }

}
