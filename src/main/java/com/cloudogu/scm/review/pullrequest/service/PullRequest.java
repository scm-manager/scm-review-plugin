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

package com.cloudogu.scm.review.pullrequest.service;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.repository.Repository;
import sonia.scm.search.Indexed;
import sonia.scm.search.IndexedType;
import sonia.scm.store.Id;
import sonia.scm.store.QueryableType;

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
@EqualsAndHashCode
@IndexedType(repositoryScoped = true, namespaceScoped = true)
@QueryableType(Repository.class)
@SuppressWarnings("UnstableApiUsage")
public class PullRequest implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(PullRequest.class);

  static final int VERSION = 1;

  @Id
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
  private Instant closeDate;
  @Indexed
  private Instant creationDate;
  @Indexed
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
