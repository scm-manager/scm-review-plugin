package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.comment.service.CommentEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.HandlerEventType;

import java.util.Map;

@Slf4j
public class CommentEventMailTextResolver extends BasicPRMailTextResolver<CommentEvent> implements MailTextResolver {

  private final CommentEvent commentEvent;
  private CommentEventType commentEventType;

  public CommentEventMailTextResolver(CommentEvent commentEvent) {
    this.commentEvent = commentEvent;
    try {
      commentEventType = CommentEventType.valueOf(commentEvent.getEventType().name());
    } catch (Exception e) {
      log.warn("the event " + commentEvent.getEventType() + " is not supported for the Mail Renderer ");
    }
  }

  @Override
  public String getMailSubject() {
    return getMailSubject(commentEvent, commentEventType.getDisplayEventName());
  }

  @Override
  public String getContentTemplatePath() {
    return commentEventType.getTemplatePath();
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath, boolean isReviewer) {
    Map<String, Object> model = super.getTemplateModel(basePath, commentEvent, isReviewer);
    switch (commentEventType) {
      case DELETE:
        model.put("oldComment", commentEvent.getOldItem());
        break;
      case CREATE:
        model.put("comment", commentEvent.getItem());
        break;
      case MODIFY:
        model.put("oldComment", commentEvent.getOldItem());
        model.put("comment", commentEvent.getItem());
        break;
    }
    return model;
  }

  private enum CommentEventType {

    DELETE("deleted_comment.mustache", "Comment deleted", HandlerEventType.DELETE),
    CREATE("created_comment.mustache", "Comment added", HandlerEventType.CREATE),
    MODIFY("modified_comment.mustache", "Comment changed", HandlerEventType.MODIFY);

    protected static final String PATH_BASE = "com/cloudogu/scm/email/template/";

    private final String template;
    private final String displayEventName;
    private final HandlerEventType type;


    CommentEventType(String template, String displayEventName, HandlerEventType type) {
      this.template = template;
      this.displayEventName = displayEventName;
      this.type = type;
    }

    public String getTemplatePath() {
      return PATH_BASE + template;
    }

    public String getDisplayEventName() {
      return displayEventName;
    }

    public HandlerEventType getType() {
      return type;
    }
  }

}
