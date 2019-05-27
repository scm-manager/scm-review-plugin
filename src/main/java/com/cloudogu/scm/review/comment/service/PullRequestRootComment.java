package com.cloudogu.scm.review.comment.service;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
public class PullRequestRootComment extends PullRequestComment implements Cloneable {

  public static PullRequestRootComment createSystemComment(String key) {
    PullRequestRootComment comment = new PullRequestRootComment();
    comment.setDate(Instant.now());
    comment.setSystemComment(true);
    comment.setComment(key);
    return comment;
  }

  public static PullRequestRootComment createComment(String id, String text, String author, Location location) {
    PullRequestRootComment comment = new PullRequestRootComment();
    comment.setId(id);
    comment.setComment(text);
    comment.setAuthor(author);
    comment.setLocation(location);
    comment.setDate(Instant.now());
    return comment;
  }

  private Location location;

  private List<PullRequestComment> replies = new ArrayList<>();

  @Override
  public PullRequestRootComment clone() {
    return (PullRequestRootComment) super.clone();
  }

  public Location getLocation() {
    return location;
  }

  public List<PullRequestComment> getResponses() {
    return Collections.unmodifiableList(replies);
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public void addReply(PullRequestComment comment) {
    this.replies.add(comment);
  }

  public void setReplies(List<PullRequestComment> replies) {
    this.replies = replies;
  }

  public void removeResponse(PullRequestComment response) {
    this.replies.remove(response);
  }
}
