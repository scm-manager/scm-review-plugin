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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

class MultipartFormDataInputHelper<DTO> {

  private final MultipartFormDataInput formData;

  MultipartFormDataInputHelper(MultipartFormDataInput formData) {
    this.formData = formData;
  }

  DTO extractJsonObject(Class<DTO> clazz, String formPartName) throws IOException {
    List<InputPart> jsonPart = formData.getFormDataMap().get(formPartName);
    if (jsonPart == null || jsonPart.isEmpty()) {
      return null;
    }

    String jsonContent = new String(jsonPart.get(0).getBody().readAllBytes(), StandardCharsets.UTF_8);
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper.readValue(jsonContent, clazz);
  }

  void processFiles(FileHandler handler) throws IOException {
    for (List<InputPart> inputParts : formData.getFormDataMap().values()) {
      for(InputPart inputPart : inputParts) {
        String fileHash = parseFileHash(inputPart.getHeaders());

        if(!Strings.isNullOrEmpty(fileHash)) {
          handler.handle(fileHash, inputPart.getBody());
        }
      }
    }
  }

  private String parseFileHash(MultivaluedMap<String, String> headers) {
    String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");
    for (String name : contentDispositionHeader) {
      if ((name.trim().startsWith("filename"))) {
        String[] tmp = name.split("=");
        return removeQuotes(tmp[1]);
      }
    }

    return null;
  }

  private String removeQuotes(String s) {
    if (s.startsWith("\"")) {
      s = s.substring(1);
    }
    if (s.endsWith("\"")) {
      return s.substring(0, s.length() - 1);
    } else {
      return s;
    }
  }

  @FunctionalInterface
  interface FileHandler {
    void handle(String fileHash, InputStream fileBody);
  }
}
