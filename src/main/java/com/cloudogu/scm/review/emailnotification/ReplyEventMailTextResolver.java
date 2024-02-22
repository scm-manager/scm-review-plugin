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
