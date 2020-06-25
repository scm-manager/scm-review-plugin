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
package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.comment.service.BasicCommentEvent;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.ExecutedTransition;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import com.cloudogu.scm.review.comment.service.Transition;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.mail.api.Topic;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;

@Slf4j
public class CommentEventMailTextResolver extends BasicPRMailTextResolver<BasicCommentEvent> implements MailTextResolver {

  private final BasicCommentEvent commentEvent;
  private final CommentEventType commentEventType;

  public CommentEventMailTextResolver(CommentEvent commentEvent) {
    this(commentEvent, getCommentEventType(commentEvent));
  }

  public CommentEventMailTextResolver(ReplyEvent commentEvent) {
    this(commentEvent, getReplyEventType(commentEvent));
  }

  private CommentEventMailTextResolver(BasicCommentEvent commentEvent, CommentEventType commentEventType) {
    this.commentEvent = commentEvent;
    this.commentEventType = commentEventType;
  }

  private static CommentEventType getCommentEventType(CommentEvent commentEvent) {
    switch (commentEvent.getEventType()) {
      case CREATE:
        return CommentEventType.COMMENT_CREATED;
      case DELETE:
        return CommentEventType.COMMENT_DELETED;
      case MODIFY:
        if (!commentEvent.getItem().getComment().equals(commentEvent.getOldItem().getComment())) {
          return CommentEventType.TEXT_MODIFIED;
        } else {
          return getCommentModificationEventType(commentEvent.getOldItem(), commentEvent.getItem());
        }
      default:
        log.warn("the event " + commentEvent.getEventType() + " is not supported for the Mail Renderer ");
        return null;
    }
  }

  private static CommentEventType getReplyEventType(ReplyEvent commentEvent) {
    switch (commentEvent.getEventType()) {
      case CREATE:
        return CommentEventType.REPLY_CREATED;
      case DELETE:
        return CommentEventType.REPLY_DELETED;
      case MODIFY:
        return CommentEventType.REPLY_MODIFIED;
      default:
        log.warn("the event " + commentEvent.getEventType() + " is not supported for the Mail Renderer ");
        return null;
    }
  }

  private static CommentEventType getCommentModificationEventType(Comment oldComment, Comment newComment) {
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
      case REPLY_DELETED:
        model.put("oldComment", commentEvent.getOldItem());
        break;
      case COMMENT_CREATED:
      case REPLY_CREATED:
        model.put("comment", commentEvent.getItem());
        break;
      case TEXT_MODIFIED:
      case REPLY_MODIFIED:
        model.put("oldComment", commentEvent.getOldItem());
        model.put("comment", commentEvent.getItem());
        break;
    }
    return model;
  }

  @Override
  public Topic getTopic() {
    return commentEvent instanceof ReplyEvent ? TOPIC_REPLIES : TOPIC_COMMENTS;
  }

  private enum CommentEventType {
    COMMENT_CREATED("created_comment.mustache", "commentAdded"),
    COMMENT_DELETED("deleted_comment.mustache", "commentDeleted"),
    TEXT_MODIFIED("modified_comment.mustache", "commentChanged"),
    REPLY_CREATED("created_reply.mustache", "replyAdded"),
    REPLY_DELETED("deleted_reply.mustache", "replyDeleted"),
    REPLY_MODIFIED("modified_reply.mustache", "replyChanged"),
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
