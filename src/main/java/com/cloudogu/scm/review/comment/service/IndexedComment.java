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
package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.XmlInstantAdapter;
import lombok.Getter;
import sonia.scm.search.Indexed;
import sonia.scm.search.IndexedType;
import java.io.Serializable;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

@IndexedType
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
  public IndexedComment(String pullRequestId, String id, String comment, String author, Instant date, CommentType type, boolean systemComment, boolean emergencyMerged) {
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
