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

package com.cloudogu.scm.review.comment.api;

import org.jboss.resteasy.mock.MockHttpRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.UUID;

public class MultipartUploadHelper {

  public static void multipartRequest(MockHttpRequest request, Map<String, InputStream> files, String jsonContent) throws IOException {
    String boundary = UUID.randomUUID().toString();
    request.contentType("multipart/form-data; boundary=" + boundary);

    //Make sure this is deleted in afterTest()
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (OutputStreamWriter formWriter = new OutputStreamWriter(buffer)) {
      formWriter.append("--").append(boundary);

      for (Map.Entry<String, InputStream> entry : files.entrySet()) {
        formWriter.append("\n");
        formWriter.append(String.format("Content-Disposition: form-data; name=\"file%s\"; filename=\"%s\"",
          entry.getKey(), entry.getKey())).append("\n");
        formWriter.append("Content-Type: application/octet-stream").append("\n\n");

        InputStream stream = entry.getValue();
        int b = stream.read();
        while (b >= 0) {
          formWriter.write(b);
          b = stream.read();
        }
        stream.close();
        formWriter.append("\n").append("--").append(boundary);
      }

      if (jsonContent != null) {
        formWriter.append("\n");
        formWriter.append("Content-Disposition: form-data; name=\"comment\"").append("\n\n");
        formWriter.append(jsonContent);
        formWriter.append("--").append(boundary);
      }

      formWriter.append("--");
      formWriter.flush();
    }
    request.setInputStream(new ByteArrayInputStream(buffer.toByteArray()));
  }
}
