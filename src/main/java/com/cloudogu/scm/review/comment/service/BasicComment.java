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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class BasicComment implements Cloneable, Serializable {

  private String id;
  private String comment;
  private String author;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant date;
  private Set<String> mentionUserIds;

  private List<ExecutedTransition> executedTransitions = new ArrayList<>();

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

  void addTransition(ExecutedTransition<?> transition) {
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
