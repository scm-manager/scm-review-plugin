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
public class PullRequestRootComment extends PullRequestComment {

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
  private boolean systemComment;
  private boolean done;

  private List<Reply> replies = new ArrayList<>();

  @Override
  public PullRequestRootComment clone() {
    return (PullRequestRootComment) super.clone();
  }

  public Location getLocation() {
    return location;
  }

  public boolean isSystemComment() {
    return systemComment;
  }

  public boolean isDone() {
    return done;
  }

  public List<Reply> getReplies() {
    return Collections.unmodifiableList(replies);
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public void setDone(boolean done) {
    this.done = done;
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

  public void removeReply(PullRequestComment reply) {
    this.replies.remove(reply);
  }
}
