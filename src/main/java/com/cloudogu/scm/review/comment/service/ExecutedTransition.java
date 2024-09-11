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

package com.cloudogu.scm.review.comment.service;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "transition")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExecutedTransition<T extends Transition> implements Serializable {

  private String id;
  private T transition;
  private long date;
  private String user;

  public ExecutedTransition() {
  }

  public ExecutedTransition(String id, T transition, long date, String user) {
    this.id = id;
    this.transition = transition;
    this.date = date;
    this.user = user;
  }

  public String getId() {
    return id;
  }

  public T getTransition() {
    return transition;
  }

  public long getDate() {
    return date;
  }

  public String getUser() {
    return user;
  }
}
