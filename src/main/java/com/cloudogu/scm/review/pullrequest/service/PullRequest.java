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
package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.XmlInstantAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.search.Indexed;
import sonia.scm.search.IndexedType;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "pull-request")
@XmlAccessorType(XmlAccessType.FIELD)
@Builder(toBuilder = true)
@ToString
@IndexedType(repositoryScoped = true, namespaceScoped = true)
@SuppressWarnings("UnstableApiUsage")
public class PullRequest implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(PullRequest.class);

  static final int VERSION = 1;

  @Indexed(type = Indexed.Type.STORED_ONLY)
  private String id;
  @Indexed(analyzer = Indexed.Analyzer.IDENTIFIER)
  private String source;
  @Indexed(analyzer = Indexed.Analyzer.IDENTIFIER)
  private String target;
  @Indexed(boost = 1.5f, defaultQuery = true)
  private String title;
  @Indexed(defaultQuery = true, highlighted = true)
  private String description;
  private String author;
  private String reviser;
  @Indexed
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant closeDate;
  @Indexed
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant creationDate;
  @Indexed
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant lastModified;
  @Indexed
  private PullRequestStatus status;
  private Set<String> subscriber = new HashSet<>();
  //TODO Identifier?
//  @Indexed(type = Indexed.Type.SEARCHABLE)
  private Set<String> labels = new HashSet<>();
  private Map<String, Boolean> reviewer = new HashMap<>();
  private String sourceRevision;
  private String targetRevision;
  private Set<ReviewMark> reviewMarks = new HashSet<>();
  @Indexed
  private String overrideMessage;
  @Indexed
  private boolean emergencyMerged;
  private List<String> ignoredMergeObstacles;
  private boolean shouldDeleteSourceBranch;

  public PullRequest(String id, String source, String target) {
    this.id = id;
    this.source = source;
    this.target = target;
  }

  public void setStatus(PullRequestStatus status) {
    if (this.status != null && status == null) {
      LOG.warn("refusing to set pull request status to null in #{} (was: {})", id, this.status, new IllegalStateException());
      return;
    }
    this.status = status;
  }

  public void addApprover(String recipient) {
    this.reviewer.put(recipient, true);
  }

  public void addReviewer(String recipient) {
    this.reviewer.putIfAbsent(recipient, false);
  }

  public void removeApprover(String recipient) {
    this.reviewer.put(recipient, false);
  }

  public Set<String> getSubscriber() {
    if (subscriber == null) {
      return emptySet();
    }
    return unmodifiableSet(subscriber);
  }

  public Map<String, Boolean> getReviewer() {
    return unmodifiableMap(reviewer);
  }

  public void addSubscriber(String recipient) {
    this.subscriber.add(recipient);
  }

  public void removeSubscriber(String recipient) {
    this.subscriber.remove(recipient);
  }

  public Set<ReviewMark> getReviewMarks() {
    return reviewMarks == null ? emptySet() : reviewMarks;
  }

  public void setLabels(Set<String> labels) {
    this.labels = labels == null ? new HashSet<>() : labels;
  }

  public Set<String> getLabels() {
    return labels == null ? emptySet() : labels;
  }

  public void addLabel(String label) {
    this.labels.add(label);
  }

  public void removeLabel(String label) {
    this.labels.remove(label);
  }

  public boolean isInProgress() {
    return status.isInProgress();
  }

  public boolean isClosed() {
    return status.isClosed();
  }

  public boolean isOpen() {
    return status == PullRequestStatus.OPEN;
  }

  public boolean isDraft() {
    return status == PullRequestStatus.DRAFT;
  }

  public boolean isRejected() {
    return status == PullRequestStatus.REJECTED;
  }

  public boolean isMerged() {
    return status == PullRequestStatus.MERGED;
  }
}
