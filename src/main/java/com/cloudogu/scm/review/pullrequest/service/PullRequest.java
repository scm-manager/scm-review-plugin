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
import sonia.scm.search.Indexed;
import sonia.scm.search.IndexedType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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
@IndexedType
@SuppressWarnings("UnstableApiUsage")
public class PullRequest implements Serializable {

  static final int VERSION = 1;

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
  //TODO enum support
  private PullRequestStatus status;
  private Set<String> subscriber = new HashSet<>();
  private Map<String, Boolean> reviewer = new HashMap<>();
  private String sourceRevision;
  private String targetRevision;
  private Set<ReviewMark> reviewMarks = new HashSet<>();
  @Indexed
  private String overrideMessage;
  @Indexed
  private boolean emergencyMerged;
  private List<String> ignoredMergeObstacles;

  public PullRequest(String id, String source, String target) {
    this.id = id;
    this.source = source;
    this.target = target;
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
}
