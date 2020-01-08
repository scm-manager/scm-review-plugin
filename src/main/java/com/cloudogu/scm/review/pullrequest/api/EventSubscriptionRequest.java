package com.cloudogu.scm.review.pullrequest.api;

import lombok.Getter;
import sonia.scm.security.SessionId;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;

@Getter
public class EventSubscriptionRequest {

  @PathParam("namespace")
  private String namespace;

  @PathParam("name")
  private String name;

  @PathParam("pullRequestId")
  private String pullRequestId;

  @HeaderParam("X-SCM-Session-ID")
  private SessionId sessionId;
}
