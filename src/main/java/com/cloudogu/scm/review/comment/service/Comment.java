package com.cloudogu.scm.review.comment.service;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.cloudogu.scm.review.comment.service.CommentType.COMMENT;
import static java.util.Collections.unmodifiableList;

@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
public class Comment extends BasicComment {

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

  public boolean isOutdated() { return outdated; }

  public List<Reply> getReplies() {
    return unmodifiableList(replies);
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

  public void addCommentTransition(ExecutedTransition<CommentTransition> transition) {
    super.addTransition(transition);
  }
}
