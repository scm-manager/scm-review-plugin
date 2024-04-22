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
