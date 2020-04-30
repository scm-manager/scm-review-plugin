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

import sonia.scm.store.ConfigurationStore;

import javax.xml.bind.JAXB;
import java.io.File;

/**
 * Use this to test storage and retrieval to and from the file system in xml format.
 */
public class SimpleJaxbStore<T> implements ConfigurationStore<T> {

  private final File file;
  private final Class<T> clazz;
  private final T defaultValue;

  public SimpleJaxbStore(Class<T> clazz, File file, T defaultValue) {
    this.clazz = clazz;
    this.file = file;
    this.defaultValue = defaultValue;
  }

  @Override
  public T get() {
    if (!file.exists()) {
      return defaultValue;
    }
    return JAXB.unmarshal(file, clazz);
  }

  @Override
  public void set(T object) {
    JAXB.marshal(object, file);
  }
}
