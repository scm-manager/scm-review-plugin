package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.comment.service.BasicComment;
import com.cloudogu.scm.review.comment.service.BasicCommentEvent;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.CommentTransition;
import com.cloudogu.scm.review.comment.service.ExecutedTransition;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import com.cloudogu.scm.review.comment.service.Transition;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

@Slf4j
public class CommentEventMailTextResolver extends BasicPRMailTextResolver<BasicCommentEvent> implements MailTextResolver {

  private final BasicCommentEvent commentEvent;
  private final CommentEventType commentEventType;

  public CommentEventMailTextResolver(CommentEvent commentEvent) {
    this(commentEvent, getBasicEventType(commentEvent, CommentEventMailTextResolver::getCommentModificationEventType));
  }

  public CommentEventMailTextResolver(ReplyEvent commentEvent) {
    this(commentEvent, getBasicEventType(commentEvent, CommentEventMailTextResolver::getReplyModificationEventType));
  }

  private CommentEventMailTextResolver(BasicCommentEvent commentEvent, CommentEventType commentEventType) {
    this.commentEvent = commentEvent;
    this.commentEventType = commentEventType;
  }

  private static <C extends BasicComment> CommentEventType getBasicEventType(BasicCommentEvent<C> commentEvent, BiFunction<C, C, CommentEventType> handleModification) {
    switch (commentEvent.getEventType()) {
      case CREATE:
        return CommentEventType.COMMENT_CREATED;
      case DELETE:
        return CommentEventType.DELETED;
      case MODIFY:
        if (!commentEvent.getItem().getComment().equals(commentEvent.getOldItem().getComment())) {
          return CommentEventType.TEXT_MODIFIED;
        } else {
          return handleModification.apply(commentEvent.getOldItem(), commentEvent.getItem());
        }
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
    } else if(lastTransition == CommentTransition.MAKE_COMMENT) {
      return CommentEventType.COMMENT_CREATED;
    } else {
      log.trace("cannot handle changes of comment");
      return null;
    }
  }

  private static CommentEventType getReplyModificationEventType(Reply oldReply, Reply newReply) {
    log.trace("cannot handle changes of comment");
    return null;
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
  public Map<String, Object> getContentTemplateModel(String basePath, boolean isReviewer) {
    Map<String, Object> model = super.getTemplateModel(basePath, commentEvent, isReviewer);

    switch (commentEventType) {
      case DELETED:
        model.put("oldComment", commentEvent.getOldItem());
        break;
      case COMMENT_CREATED:
        model.put("comment", commentEvent.getItem());
        break;
      case TEXT_MODIFIED:
        model.put("oldComment", commentEvent.getOldItem());
        model.put("comment", commentEvent.getItem());
        break;
    }
    return model;
  }

  private enum CommentEventType {
    DELETED("deleted_comment.mustache", "commentDeleted"),
    COMMENT_CREATED("created_comment.mustache", "commentAdded"),
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
