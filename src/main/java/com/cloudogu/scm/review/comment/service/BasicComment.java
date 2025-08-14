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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode
@ToString
public abstract class BasicComment implements Cloneable, Serializable {

  private String id;
  private String comment;
  private String author;
  private Instant date;
  private Set<String> mentionUserIds;

  private List<ExecutedTransition> executedTransitions = new ArrayList<>();

  @Getter
  @Setter
  private Set<String> assignedImages = new HashSet<>();

  public String getId() {
    return id;
  }

  public String getComment() {
    return comment;
  }

  public String getAuthor() {
    return author;
  }

  public Instant getDate() {
    return date;
  }

  public List<ExecutedTransition> getExecutedTransitions() {
    return unmodifiableList(executedTransitions);
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setDate(Instant date) {
    this.date = date;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void addTextTransition(ExecutedTransition<TextTransition> transition) {
    this.executedTransitions.add(transition);
  }

  public void addExecutedTransition(ExecutedTransition transition) {
    this.executedTransitions.add(transition);
  }

  public Set<String> getMentionUserIds() {
    if (mentionUserIds == null) {
      return emptySet();
    }
    return unmodifiableSet(mentionUserIds);
  }

  public void setMentionUserIds(Set<String> mentionUserIds) {
    this.mentionUserIds = mentionUserIds;
  }

  public BasicComment clone() {
    try {
      return (BasicComment) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new RuntimeException(ex);
    }
  }
}
