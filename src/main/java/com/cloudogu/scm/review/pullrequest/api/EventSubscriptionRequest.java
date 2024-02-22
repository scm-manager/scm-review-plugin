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
package com.cloudogu.scm.review.pullrequest.api;

import lombok.Getter;
import sonia.scm.security.SessionId;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

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
