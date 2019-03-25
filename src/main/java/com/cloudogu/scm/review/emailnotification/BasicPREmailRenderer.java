package com.cloudogu.scm.review.emailnotification;

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.pullrequest.service.BasicPullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.collect.Maps;
import sonia.scm.repository.Repository;
import sonia.scm.template.Template;

import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Map;

public abstract class BasicPREmailRenderer<E extends BasicPullRequestEvent> implements EmailRenderer {

  private static final String SUBJECT_PATTERN = "Re: [SCM-Manager] [{4}] Pull request #{0}: {1} ({2}/{3})";
  private static final String SCM_PULL_REQUEST_URL_PATTERN = "{0}/repo/{1}/{2}/pull-request/{3}";


  protected String getMailSubject(E event, String displayEventName) {
    Repository repository = event.getRepository();
    PullRequest pullRequest = event.getPullRequest();
    return MessageFormat.format(SUBJECT_PATTERN, pullRequest.getId(), pullRequest.getTitle(), repository.getNamespace(), repository.getName(), displayEventName);
  }

  protected String getMailContent(String basePath, E event, Template template) throws IOException {
    StringWriter writer = new StringWriter();
    template.execute(writer, getTemplateModel(basePath, event));
    return writer.toString();
  }

  private String getPullRequestLink(String baseUrl, E event) {
    Repository repository = event.getRepository();
    PullRequest pullRequest = event.getPullRequest();
    return MessageFormat.format(SCM_PULL_REQUEST_URL_PATTERN, baseUrl, repository.getNamespace(), repository.getName(), pullRequest.getId());
  }

  /**
   * The basic environment used by all templates
   *
   * @param basePath the path url of the ui
   * @param event    the fired event
   * @return basic environment used by all templates
   */
  protected Map<String, Object> getTemplateModel(String basePath, E event) {
    Map<String, Object> result = Maps.newHashMap();
    result.put("title", getMailSubject());
    result.put("displayName", CurrentUserResolver.getCurrentUserDisplayName());
    result.put("link", getPullRequestLink(basePath, event));
    result.put("repository", event.getRepository());
    result.put("pullRequest", event.getPullRequest());
    return result;
  }


}
