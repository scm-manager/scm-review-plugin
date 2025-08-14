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

package com.cloudogu.scm.review.pullrequest.api;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import sonia.scm.security.SessionId;

@Getter
public class EventSubscriptionRequest {

  @PathParam("namespace")
  private String namespace;

  @PathParam("name")
  private String name;

  @PathParam("pullRequestId")
  private String pullRequestId;

  @HeaderParam(SessionId.PARAMETER)
  private SessionId sessionIdFromHeader;

  @QueryParam(SessionId.PARAMETER)
  private SessionId sessionIdFromQueryParam;

  public SessionId getSessionId() {
    // we support both, because version prior 2.0.0-rc6 send the session id as header
    // and versions after rc6 send it as query param
    return sessionIdFromHeader != null ? sessionIdFromHeader : sessionIdFromQueryParam;
  }
}
