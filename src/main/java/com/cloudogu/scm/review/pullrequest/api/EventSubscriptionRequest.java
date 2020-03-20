package com.cloudogu.scm.review.pullrequest.api;

import lombok.Getter;
import sonia.scm.security.SessionId;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Getter
public class EventSubscriptionRequest {

  @PathParam("namespace")
  private String namespace;

  @PathParam("name")
  private String name;

  @PathParam("pullRequestId")
  private String pullRequestId;

  @HeaderParam("X-SCM-Session-ID")
  private SessionId sessionIdFromHeader;

  @QueryParam("X-SCM-Session-ID")
  private SessionId sessionIdFromQueryParam;

  public SessionId getSessionId() {
    // we support both, because version prior 2.0.0-rc6 send the session id as header
    // and versions after rc6 send it as query param
    return sessionIdFromHeader != null ? sessionIdFromHeader : sessionIdFromQueryParam;
  }
}
