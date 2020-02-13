package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.comment.service.MentionEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class MentionEventMailTextResolver extends BasicPRMailTextResolver<MentionEvent> implements MailTextResolver {

  private final MentionEvent mentionEvent;

  public static final String EVENT_DISPLAY_NAME = "newMention";
  protected static final String EVENT_TEMPLATE_PATH = "com/cloudogu/scm/email/template/new_mention.mustache";


  public MentionEventMailTextResolver(MentionEvent mentionEvent) {
    this.mentionEvent = mentionEvent;
  }

  @Override
  public String getMailSubject(Locale locale) {
    return getMailSubject(mentionEvent, EVENT_DISPLAY_NAME, locale);
  }

  @Override
  public String getContentTemplatePath() {
    return EVENT_TEMPLATE_PATH;
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath, boolean isReviewer) {
    Map<String, Object> model = getTemplateModel(basePath, mentionEvent, false);
    model.put("comment", mentionEvent.getItem());
    return model;
  }
}
