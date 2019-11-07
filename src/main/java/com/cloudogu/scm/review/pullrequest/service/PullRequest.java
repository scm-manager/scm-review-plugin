package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.XmlInstantAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "pull-request")
@XmlAccessorType(XmlAccessType.FIELD)
@Builder(toBuilder = true)
public class PullRequest {

  private String id;
  private String source;
  private String target;
  private String title;
  private String description;
  private String author;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant creationDate;
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  private Instant lastModified;
  private PullRequestStatus status;
  private Set<String> subscriber = new HashSet<>();
  private Map<String, Boolean> reviewer = new HashMap<>();

  public void addApprover(String recipient) {
    this.reviewer.put(recipient, true);
  }

  public void removeApprover(String recipient) {
    this.reviewer.put(recipient, false);
  }

  public Set<String> getSubscriber() {
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
