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

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import sonia.scm.repository.Repository;
import sonia.scm.store.QueryableType;
import sonia.scm.xml.XmlMapStringAdapter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.cloudogu.scm.review.comment.service.CommentType.COMMENT;
import static java.util.Collections.unmodifiableList;

@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
@QueryableType({Repository.class, PullRequest.class})
@EqualsAndHashCode(callSuper = true)
@ToString
public class Comment extends BasicComment {

  public static Comment createSystemComment(String key, Map<String, String> systemCommentParameters) {
    Comment systemComment = createSystemComment(key);
    systemComment.setSystemCommentParameters(systemCommentParameters);
    return systemComment;
  }

  public static Comment createSystemComment(String key) {
    Comment comment = new Comment();
    comment.setDate(Instant.now());
    comment.setSystemComment(true);
    comment.setComment(key);
    return comment;
  }

  public static Comment createComment(String id, String text, String author, Location location) {
    Comment comment = new Comment();
    comment.setId(id);
    comment.setComment(text);
    comment.setAuthor(author);
    comment.setLocation(location);
    comment.setDate(Instant.now());

    return comment;
  }

  private Location location;
  private boolean systemComment;
  private CommentType type = COMMENT;
  private boolean outdated;
  private InlineContext context;
  private boolean emergencyMerged;
  @XmlJavaTypeAdapter(XmlMapStringAdapter.class)
  private Map<String, String> systemCommentParameters = Collections.emptyMap();

  private List<Reply> replies = new ArrayList<>();

  @Override
  public Comment clone() {
    return (Comment) super.clone();
  }

  public CommentType getType() {
    return type;
  }

  public Location getLocation() {
    return location;
  }

  public boolean isSystemComment() {
    return systemComment;
  }

  public boolean isOutdated() {
    return outdated;
  }

  public List<Reply> getReplies() {
    return unmodifiableList(replies);
  }

  public Map<String, String> getSystemCommentParameters() {
    return Collections.unmodifiableMap(systemCommentParameters);
  }

  public void setType(CommentType type) {
    this.type = type;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public void setOutdated(boolean outdated) {
    this.outdated = outdated;
  }

  public void setSystemCommentParameters(Map<String, String> systemCommentParameters) {
    this.systemCommentParameters = systemCommentParameters;
  }

  void setSystemComment(boolean systemComment) {
    this.systemComment = systemComment;
  }

  public void addReply(Reply reply) {
    this.replies.add(reply);
  }

  public void setReplies(List<Reply> replies) {
    this.replies = replies;
  }

  public void removeReply(BasicComment reply) {
    this.replies.remove(reply);
  }

  public InlineContext getContext() {
    return this.context;
  }

  public void setContext(InlineContext context) {
    this.context = context;
  }

  public boolean isEmergencyMerged() {
    return emergencyMerged;
  }

  public void setEmergencyMerged(boolean emergencyMerged) {
    this.emergencyMerged = emergencyMerged;
  }
}
