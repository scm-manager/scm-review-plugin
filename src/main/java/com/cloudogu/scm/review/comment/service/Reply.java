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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Reply extends BasicComment {

  private boolean systemReply = false;

  public boolean isSystemReply() {
    return systemReply;
  }

  public void setSystemReply(boolean systemReply) {
    this.systemReply = systemReply;
  }

  public static Reply createReply(String id, String text, String author) {
    Reply comment = new Reply();
    comment.setId(id);
    comment.setComment(text);
    comment.setAuthor(author);
    comment.setDate(Instant.now());
    return comment;
  }

  public static Reply createNewReply(String text) {
    Reply comment = new Reply();
    comment.setComment(text);
    comment.setDate(Instant.now());
    return comment;
  }

  @Override
  public Reply clone() {
    return (Reply) super.clone();
  }
}
