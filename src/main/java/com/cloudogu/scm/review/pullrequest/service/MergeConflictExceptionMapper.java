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

package com.cloudogu.scm.review.pullrequest.service;

import org.slf4j.MDC;
import sonia.scm.ContextEntry;
import sonia.scm.web.VndMediaType;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

@Provider
public class MergeConflictExceptionMapper implements ExceptionMapper<MergeConflictException> {

  @Override
  public Response toResponse(MergeConflictException exception) {
    return Response.status(409).entity(new Object() {
      public String getTransactionId() {
        return MDC.get("transaction_id");
      }

      public String getErrorCode() {
        return exception.getCode();
      }

      public List<ContextEntry> getContext() {
        return exception.getContext();
      }

      public String getMessage() {
        return exception.getMessage();
      }
    }).type(VndMediaType.ERROR_TYPE).build();
  }
}
