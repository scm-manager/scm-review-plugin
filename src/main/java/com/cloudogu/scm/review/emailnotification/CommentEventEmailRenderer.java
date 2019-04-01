package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.comment.service.CommentEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.HandlerEventType;
import sonia.scm.template.Template;
import sonia.scm.template.TemplateEngine;
import sonia.scm.template.TemplateEngineFactory;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class CommentEventEmailRenderer extends BasicPREmailRenderer<CommentEvent> implements EmailRenderer {

  private final CommentEvent commentEvent;
  private CommentEventType commentEventType;

  public CommentEventEmailRenderer(CommentEvent commentEvent) {
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
  public String getMailContent(String basePath, TemplateEngineFactory templateEngineFactory, boolean isReviewer) throws IOException {
    String path = commentEventType.getTemplatePath();
    TemplateEngine templateEngine = templateEngineFactory.getEngineByExtension(path);
    Template template = templateEngine.getTemplate(path);

    return getMailContent(basePath, commentEvent, template);
  }


  @Override
  public Map<String, Object> getTemplateModel(String basePath, CommentEvent event, boolean isReviewer) {
    Map<String, Object> model = super.getTemplateModel(basePath, event, isReviewer);
    switch (commentEventType) {
      case DELETE:
        model.put("oldComment", event.getOldItem());
        break;
      case CREATE:
        model.put("comment", event.getItem());
        break;
      case MODIFY:
        model.put("oldComment", event.getOldItem());
        model.put("comment", event.getItem());
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
