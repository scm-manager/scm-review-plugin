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

package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.InlineContext;
import com.cloudogu.scm.review.comment.service.Location;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import sonia.scm.xml.XmlMapStringAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.cloudogu.scm.review.comment.service.CommentType.COMMENT;
import static java.util.Collections.unmodifiableList;

@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
class LegacyXmlComment extends LegacyXmlBasicComment {

  private Location location;
  private boolean systemComment;
  private CommentType type = COMMENT;
  private boolean outdated;
  private InlineContext context;
  private boolean emergencyMerged;
  @XmlJavaTypeAdapter(XmlMapStringAdapter.class)
  private Map<String, String> systemCommentParameters = Collections.emptyMap();

  private List<LegacyXmlReply> replies = new ArrayList<>();

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

  public List<LegacyXmlReply> getReplies() {
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

  public void setReplies(List<LegacyXmlReply> replies) {
    this.replies = replies;
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
