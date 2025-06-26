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

package com.cloudogu.scm.review.workflow;

import jakarta.xml.bind.JAXB;
import sonia.scm.store.ConfigurationStore;

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
