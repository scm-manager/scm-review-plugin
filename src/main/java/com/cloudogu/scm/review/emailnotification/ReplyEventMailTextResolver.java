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

import com.cloudogu.scm.review.comment.service.BasicCommentEvent;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.mail.api.Topic;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class ReplyEventMailTextResolver extends BasicPRMailTextResolver<BasicCommentEvent<?>> implements MailTextResolver {

  private final ReplyEvent replyEvent;
  private final ReplyEventType replyEventType;

  public ReplyEventMailTextResolver(ReplyEvent replyEvent) {
    this.replyEvent = replyEvent;
    this.replyEventType = getReplyEventType(replyEvent);
  }

  private static ReplyEventType getReplyEventType(ReplyEvent commentEvent) {
    switch (commentEvent.getEventType()) {
      case CREATE:
        return ReplyEventType.REPLY_CREATED;
      case DELETE:
        return ReplyEventType.REPLY_DELETED;
      case MODIFY:
        return ReplyEventType.REPLY_MODIFIED;
      default:
        log.warn("the event " + commentEvent.getEventType() + " is not supported for the Mail Renderer ");
        return null;
    }
  }

  @Override
  public String getMailSubject(Locale locale) {
    return getMailSubject(replyEvent, replyEventType.getDisplayEventName(), locale);
  }

  @Override
  public String getContentTemplatePath() {
    return replyEventType.getTemplatePath();
  }

  @Override
  public Map<String, Object> getContentTemplateModel(String basePath) {
    Map<String, Object> model = super.getTemplateModel(basePath, replyEvent);

    switch (replyEventType) {
      case REPLY_DELETED:
        model.put("oldComment", replyEvent.getOldItem());
        break;
      case REPLY_CREATED:
        model.put("comment", replyEvent.getItem());
        break;
      case REPLY_MODIFIED:
        model.put("oldComment", replyEvent.getOldItem());
        model.put("comment", replyEvent.getItem());
        break;
      default:
        // no more values needed
    }
    return model;
  }

  @Override
  public Topic getTopic() {
    return TOPIC_REPLIES;
  }

  @Override
  public String getPullRequestId() {
    return this.replyEvent.getPullRequest().getId();
  }

  private enum ReplyEventType {
    REPLY_CREATED("created_reply.mustache", "replyAdded"),
    REPLY_DELETED("deleted_reply.mustache", "replyDeleted"),
    REPLY_MODIFIED("modified_reply.mustache", "replyChanged");

    protected static final String PATH_BASE = "com/cloudogu/scm/email/template/";

    private final String template;
    private final String displayEventName;

    ReplyEventType(String template, String displayEventName) {
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
