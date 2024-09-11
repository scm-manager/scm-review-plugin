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

package com.cloudogu.scm.review;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Instant;

public class XmlInstantAdapter extends XmlAdapter<Long, Instant> {
  @Override
  public Long marshal(Instant v) {
    return v == null ? null : v.toEpochMilli();
  }

  @Override
  public Instant unmarshal(Long v) {
    return v == null ? null : Instant.ofEpochMilli(v);
  }
}
