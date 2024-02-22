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

package com.cloudogu.scm.review.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.xml.bind.JAXB;
import java.io.StringReader;
import java.io.StringWriter;

public class RuleConfigurationTestUtil {

  static <T> T toAndFromJson(Class<T> clazz, T input) throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    final String json = objectMapper.writeValueAsString(input);
    return objectMapper.readValue(json, clazz);
  }

  static <T> T toAndFromXml(Class<T> clazz, T input) {
    final StringWriter xmlWriter = new StringWriter();
    JAXB.marshal(input, xmlWriter);
    final StringReader xmlReader = new StringReader(xmlWriter.toString());
    return JAXB.unmarshal(xmlReader, clazz);
  }

  static <T> T toAndFromJsonAndXml(Class<T> clazz, T input) throws JsonProcessingException {
    return toAndFromXml(clazz, toAndFromJson(clazz, input));
  }
}
