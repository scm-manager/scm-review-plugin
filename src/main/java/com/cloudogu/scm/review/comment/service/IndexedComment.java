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

import com.cloudogu.scm.review.XmlInstantAdapter;
import lombok.Getter;
import sonia.scm.search.Indexed;
import sonia.scm.search.IndexedType;
import java.io.Serializable;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

@IndexedType(repositoryScoped = true, namespaceScoped = true)
@Getter
@SuppressWarnings({"UnstableApiUsage"})
public class IndexedComment implements Serializable {

  static final int VERSION = 1;

  @Indexed(type = Indexed.Type.STORED_ONLY)
  private final String pullRequestId;
  @Indexed(type = Indexed.Type.STORED_ONLY)
  private final String id;
  @Indexed(highlighted = true, defaultQuery = true)
  private final String comment;
  @Indexed(type = Indexed.Type.SEARCHABLE)
  private final String author;
  @Indexed
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private final Instant date;
  @Indexed
  private final CommentType type;
  @Indexed
  private final boolean systemComment;
  @Indexed
  private final boolean emergencyMerged;

  @SuppressWarnings("java:S107")
  private IndexedComment(String pullRequestId, String id, String comment, String author, Instant date, CommentType type, boolean systemComment, boolean emergencyMerged) {
    this.pullRequestId = pullRequestId;
    this.id = id;
    this.comment = comment;
    this.author = author;
    this.date = date;
    this.type = type;
    this.systemComment = systemComment;
    this.emergencyMerged = emergencyMerged;
  }

  static IndexedComment transform(String pullRequestId, Comment comment) {
    return new IndexedComment(
      pullRequestId, comment.getId(),
      comment.getComment(),
      comment.getAuthor(),
      comment.getDate(),
      comment.getType(),
      comment.isSystemComment(),
      comment.isEmergencyMerged()
    );
  }

  static IndexedComment transform(String pullRequestId, Reply reply) {
    return new IndexedComment(
      pullRequestId, reply.getId(),
      reply.getComment(),
      reply.getAuthor(),
      reply.getDate(),
      CommentType.COMMENT,
      reply.isSystemReply(),
      false
    );
  }
}
