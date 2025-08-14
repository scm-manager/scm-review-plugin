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

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.api.MentionMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEmergencyMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestReopenedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatusChangedEvent;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.authz.UnauthorizedException;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.NotFoundException;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStoreExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.cloudogu.scm.review.comment.service.Comment.createComment;
import static com.cloudogu.scm.review.comment.service.CommentTransition.MAKE_TASK;
import static com.cloudogu.scm.review.comment.service.CommentTransition.SET_DONE;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_DONE;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_TODO;
import static com.cloudogu.scm.review.comment.service.Reply.createReply;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.Collections.EMPTY_SET;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class, QueryableStoreExtension.class})
@QueryableStoreExtension.QueryableTypes(Comment.class)
@SubjectAware("trillian")
class CommentServiceTest {

  private final String NAMESPACE = "space";
  private final String NAME = "name";
  private final Repository REPOSITORY = new Repository("repo_ID", "git", NAMESPACE, NAME);
  private final String PULL_REQUEST_ID = "pr_id";
  private final Instant NOW = now().truncatedTo(ChronoUnit.SECONDS);
  private final String COMMENT_ID = "1";
  private final String author = "author";
  private final Comment EXISTING_COMMENT = createComment(COMMENT_ID, "1. comment", author, new Location());
  private final Reply EXISTING_REPLY = createReply("resp_1", "1. reply", author);

  {
    EXISTING_COMMENT.addReply(EXISTING_REPLY);
    // nanoseconds are not supported by the json mapper
    EXISTING_COMMENT.setDate(Instant.now().truncatedTo(ChronoUnit.MILLIS));
    EXISTING_REPLY.setDate(Instant.now().truncatedTo(ChronoUnit.MILLIS));
  }

  @Mock
  private RepositoryResolver repositoryResolver;

  @Mock
  private PullRequestService pullRequestService;

  @Mock
  private KeyGenerator keyGenerator;

  @Mock
  private ScmEventBus eventBus;

  @Mock
  private CommentInitializer commentInitializer;

  @Mock
  private MentionMapper mentionMapper;

  private CommentStoreBuilder storeBuilder;

  @Captor
  private ArgumentCaptor<Comment> rootCommentCaptor;
  @Captor
  private ArgumentCaptor<BasicCommentEvent> eventCaptor;

  private CommentService commentService;

  private int lastCommentKeyId = 1;

  @BeforeEach
  void init(CommentStoreFactory storeFactory) {
    storeBuilder = new CommentStoreBuilder(storeFactory, keyGenerator);
    lenient().doAnswer(invocationOnMock -> Integer.toString(++lastCommentKeyId))
      .when(keyGenerator).createKey();

    lenient().doNothing().when(eventBus).post(eventCaptor.capture());

    lenient().when(repositoryResolver.resolve(REPOSITORY.getNamespaceAndName())).thenReturn(REPOSITORY);
    commentService = new CommentService(repositoryResolver, pullRequestService, storeBuilder, keyGenerator, eventBus, commentInitializer, mentionMapper);

    try (QueryableMutableStore<Comment> store = storeFactory.getMutable(REPOSITORY.getId(), PULL_REQUEST_ID)) {
      store.put("1", EXISTING_COMMENT);
    }

    lenient().doAnswer(invocation -> {
      BasicComment comment = invocation.getArgument(0);
      comment.setDate(NOW);
      comment.setAuthor(author);
      return null;
    }).when(commentInitializer).initialize(any(), any(), any());
  }

  @Test
  void shouldGetComment() {
    Comment comment = commentService.get(NAMESPACE, NAME, PULL_REQUEST_ID, COMMENT_ID);

    assertThat(comment).isEqualTo(EXISTING_COMMENT);
  }

  @Test
  void shouldFailGetIfNoCommentExists() {
    assertThrows(
      NotFoundException.class,
      () -> commentService.get(NAMESPACE, NAME, PULL_REQUEST_ID, "2")
    );
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldAddComment(CommentStoreFactory storeFactory) {
    Comment comment = createComment("2", "2. comment", author, new Location());
    commentService.add(NAMESPACE, NAME, PULL_REQUEST_ID, comment);

    Comment storedComment = readCommentFromStore(storeFactory, Integer.toString(lastCommentKeyId));
    assertThat(storedComment.getAuthor()).isEqualTo("author");
    assertThat(storedComment.getDate()).isEqualTo(NOW);
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldExtractMentionsOnAddComment(CommentStoreFactory storeFactory) {
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("trillian"));

    Comment comment = createComment("2", "2. comment @[trillian]", author, new Location());
    commentService.add(NAMESPACE, NAME, PULL_REQUEST_ID, comment);

    Comment storedComment = readCommentFromStore(storeFactory, Integer.toString(lastCommentKeyId));
    assertThat(storedComment.getMentionUserIds()).contains("trillian");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldTriggerMentionEventIfNewMentionAddedOnCreateComment(CommentStoreFactory storeFactory) {
    when(mentionMapper.extractMentionsFromComment("2. comment @[dent]")).thenReturn(ImmutableSet.of("dent"));

    Comment comment = createComment("2", "2. comment @[dent]", author, new Location());

    String parsedCommentText = "2. comment @Arthur Dent";
    Comment parsedComment = createComment("2", parsedCommentText, author, new Location());
    when(mentionMapper.parseMentionsUserIdsToDisplayNames(comment)).thenReturn(parsedComment);

    commentService.add(NAMESPACE, NAME, PULL_REQUEST_ID, comment);

    assertThat(eventCaptor.getAllValues()).hasSize(2);
    assertMentionEventFiredAndMentionsParsedToDisplayNames(parsedCommentText);
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldPostEventForNewRootComment() {
    Comment comment = createComment("2", "2. comment", author, new Location());
    commentService.add(NAMESPACE, NAME, PULL_REQUEST_ID, comment);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.CREATE);
  }

  @Test
  @SubjectAware(permissions = "repository:read:*")
  void shouldFailIfUserHasNoPermissionToCreateComment() {
    Comment comment = createComment("2", "2. comment", author, new Location());

    assertThrows(
      UnauthorizedException.class,
      () -> commentService.add(REPOSITORY.getNamespace(), REPOSITORY.getName(), PULL_REQUEST_ID, comment)
    );
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldAddReplyToParentComment(CommentStoreFactory storeFactory) {
    Reply reply = createReply("new reply", "1. comment", author);

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getReplies()).hasSize(2);
    assertThat(storedComment.getReplies()).contains(reply);
    assertThat(storedComment.getReplies().get(1).getId()).isNotEqualTo("new reply");
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldExtractMentionsOnCreateReply(CommentStoreFactory storeFactory) {
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("dent"));
    Reply reply = createReply("new reply", "1. comment @[dent]", author);

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getReplies()).hasSize(2);
    assertThat(storedComment.getReplies()).contains(reply);
    assertThat(storedComment.getReplies().get(1).getId()).isNotEqualTo("new reply @[dent]");
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldTriggerMentionEventIfNewMentionAddedOnCreateReply() {
    when(mentionMapper.extractMentionsFromComment("1. comment @[dent]")).thenReturn(ImmutableSet.of("dent"));

    Reply reply = createReply("new reply", "1. comment @[dent]", author);

    String parsedCommentText = "2. comment @Arthur Dent";
    Reply parsedReply = createReply("2", parsedCommentText, author);
    when(mentionMapper.parseMentionsUserIdsToDisplayNames(reply)).thenReturn(parsedReply);

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);

    assertThat(eventCaptor.getAllValues()).hasSize(2);
    assertMentionEventFiredAndMentionsParsedToDisplayNames(parsedCommentText);
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldPostEventForNewReply() {
    Reply reply = createReply("1", "1. comment", author);

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.CREATE);
  }

  @Test
  @SubjectAware(permissions = "repository:read:*")
  void shouldFailIfUserHasNoPermissionTocreateReply() {
    Reply reply = createReply("1", "1. comment", author);

    assertThrows(
      UnauthorizedException.class,
      () -> commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply)
    );
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldModifyRootCommentText(CommentStoreFactory storeFactory) {
    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setComment("new comment");
    changedRootComment.setMentionUserIds(EMPTY_SET);

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment);

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getComment()).isEqualTo("new comment");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldExtractMentionsOnModifyRootCommentText(CommentStoreFactory storeFactory) {
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("trillian"));
    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setComment("new comment @[trillian]");
    changedRootComment.setMentionUserIds(EMPTY_SET);

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment);

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getComment()).isEqualTo("new comment @[trillian]");
    assertThat(storedComment.getMentionUserIds()).contains("trillian");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldTriggerMentionEventIfNewMentionAddedOnModify() {
    when(mentionMapper.extractMentionsFromComment("new comment @[dent]")).thenReturn(ImmutableSet.of("dent"));

    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setComment("new comment @[dent]");
    changedRootComment.setMentionUserIds(EMPTY_SET);

    String parsedCommentText = "2. comment @Arthur Dent";
    Comment parsedComment = EXISTING_COMMENT.clone();
    parsedComment.setComment(parsedCommentText);
    when(mentionMapper.parseMentionsUserIdsToDisplayNames(any(Comment.class))).thenReturn(parsedComment);

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, changedRootComment.getId(), changedRootComment);

    assertThat(eventCaptor.getAllValues()).hasSize(2);
    assertMentionEventFiredAndMentionsParsedToDisplayNames(parsedCommentText);
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldModifyRootCommentDoneFlag(CommentStoreFactory storeFactory) {
    EXISTING_COMMENT.setType(CommentType.TASK_TODO);
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    commentService.transform(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), SET_DONE);

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getType()).isEqualTo(TASK_DONE);
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldTriggerModifyEventForRootComment(CommentStoreFactory storeFactory) {
    EXISTING_COMMENT.setType(CommentType.TASK_TODO);
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    commentService.transform(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), SET_DONE);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.MODIFY);
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldKeepAuthorAndDateWhenModifyingRootComment(CommentStoreFactory storeFactory) {
    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setAuthor("new author");
    changedRootComment.setDate(ofEpochMilli(123));
    changedRootComment.setMentionUserIds(EMPTY_SET);
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment);

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getAuthor()).isEqualTo(EXISTING_COMMENT.getAuthor());
    assertThat(storedComment.getDate()).isEqualTo(EXISTING_COMMENT.getDate().truncatedTo(ChronoUnit.MILLIS));
  }

  @Test
  @SubjectAware(value = "createCommentUser", permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldModifyCommentWhenUserHasNoModifyPermissionButCommentIsHerOwn(CommentStoreFactory storeFactory) {
    EXISTING_COMMENT.setAuthor("createCommentUser");
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setComment("new content");

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment);

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getComment()).isEqualTo("new content");
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldFailModifyingRootCommentWhenUserHasNoPermission() {
    Comment changedRootComment = EXISTING_COMMENT.clone();

    assertThrows(
      UnauthorizedException.class,
      () -> commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment)
    );
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldFailModifyWhenCommentDoesNotExist(CommentStoreFactory storeFactory) {
    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setComment("new comment");

    assertThrows(NotFoundException.class, () -> commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, "no such id", changedRootComment));

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getComment()).isEqualTo("1. comment");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldDeleteRootComment(CommentStoreFactory storeFactory) {
    EXISTING_COMMENT.setReplies(emptyList());
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId());

    assertThat(getAllCommentsFromStore(storeFactory)).isEmpty();
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldTriggerDeleteEvent(CommentStoreFactory storeFactory) {
    EXISTING_COMMENT.setReplies(emptyList());
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId());

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.DELETE);
  }

  @Test
  @SubjectAware(permissions = "repository:read:*")
  void shouldFailWhenDeletingRootCommentWithoutPermission(CommentStoreFactory storeFactory) {
    EXISTING_COMMENT.setReplies(emptyList());
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    assertThrows(
      UnauthorizedException.class,
      () -> commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId())
    );
  }

  @Test
  void shouldNotFailWhenDeletingNotExistingRootComment(CommentStoreFactory storeFactory) {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, "no such id");

    assertThat(getAllCommentsFromStore(storeFactory)).hasSize(1);
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldNotDeleteIfReplyExists(CommentStoreFactory storeFactory) {
    assertThrows(
      ScmConstraintViolationException.class,
      () -> commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId())
    );

    assertThat(getAllCommentsFromStore(storeFactory)).hasSize(1);
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldNotDeleteIfReplyIsSystemReply(CommentStoreFactory storeFactory) {
    EXISTING_REPLY.setSystemReply(true);
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    assertThrows(
      ScmConstraintViolationException.class,
      () -> commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId())
    );
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldModifyReplyText(CommentStoreFactory storeFactory) {
    Reply changedReply = EXISTING_REPLY.clone();
    changedReply.setComment("new comment");

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getReplies()).hasSize(1);
    assertThat(storedComment.getReplies().get(0).getComment()).isEqualTo("new comment");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldExtractMentionsOnModifyReplyText(CommentStoreFactory storeFactory) {
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("trillian", "dent"));

    Reply changedReply = EXISTING_REPLY.clone();
    changedReply.setComment("new comment @[trillian] @[dent]");

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getReplies()).hasSize(1);
    assertThat(storedComment.getReplies().get(0).getComment()).isEqualTo("new comment @[trillian] @[dent]");
    assertThat(storedComment.getReplies().get(0).getMentionUserIds()).contains("trillian", "dent");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldTriggerMentionEventOnModifyReplyText() {
    when(mentionMapper.extractMentionsFromComment("new comment @[dent]")).thenReturn(ImmutableSet.of("dent"));

    Reply changedReply = EXISTING_REPLY.clone();
    changedReply.setMentionUserIds(ImmutableSet.of("dent"));
    changedReply.setComment("new comment @[dent]");

    String parsedCommentText = "2. comment @Arthur Dent";
    Reply parsedReply = createReply("2", parsedCommentText, author);
    when(mentionMapper.parseMentionsUserIdsToDisplayNames(any(Reply.class))).thenReturn(parsedReply);

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    assertThat(eventCaptor.getAllValues()).hasSize(2);
    assertMentionEventFiredAndMentionsParsedToDisplayNames(parsedCommentText);
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldTriggerModifyEventForReplies() {
    Reply changedReply = EXISTING_REPLY.clone();
    changedReply.setComment("new comment");

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.MODIFY);
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldModifyReplyDoneFlag(CommentStoreFactory storeFactory) {
    Reply changedReply = EXISTING_REPLY.clone();

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getReplies()).hasSize(1);
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldFailModifyingReplyWhenUserHasNoPermission() {
    Reply changedReply = EXISTING_REPLY.clone();

    assertThrows(
      UnauthorizedException.class,
      () -> commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply)
    );
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldDeleteExistingReply(CommentStoreFactory storeFactory) {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId());

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.getReplies()).isEmpty();
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldFailDeletingSystemComment() {
    assertThrows(
      ScmConstraintViolationException.class,
      () -> commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId())
    );
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldTriggerDeleteEventForReply() {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId());

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.DELETE);
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldNotTriggerEventForDeletingNotExistingComment() {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, "no such comment");

    assertThat(eventCaptor.getAllValues()).isEmpty();
  }

  @Test
  @SubjectAware(permissions = "repository:read:*")
  void shouldFailDeletingExistingReplyWithoutPermission() {
    assertThrows(
      UnauthorizedException.class,
      () -> commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId())
    );
  }

  @Test
  @SubjectAware(permissions = "repository:read:*")
  void shouldAddChangedStatusComment(CommentStoreFactory storeFactory) {
    commentService.addCommentOnMerge(new PullRequestMergedEvent(REPOSITORY, mockPullRequest()));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("merged");
    assertThat(storedComment.isSystemComment()).isTrue();
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest:*")
  void shouldGetAllComments() {
    Collection<Comment> all = commentService.getAll(NAMESPACE, NAME, PULL_REQUEST_ID);

    assertThat(all).containsExactly(EXISTING_COMMENT);
  }

  @Test
  @SubjectAware(permissions = "repository:read,readPullRequest,commentPullRequest:repo_ID")
  void shouldGiveTransitionsForAuthorizedUser() {
    Collection<CommentTransition> commentTransitions = commentService.possibleTransitions(NAMESPACE, NAME, PULL_REQUEST_ID, COMMENT_ID);

    assertThat(commentTransitions).containsExactly(MAKE_TASK);
  }

  @Test
  @SubjectAware(permissions = "repository:read:*")
  void shouldGiveNoTransitionsForUnauthorizedUser() {
    Collection<CommentTransition> commentTransitions = commentService.possibleTransitions(NAMESPACE, NAME, PULL_REQUEST_ID, COMMENT_ID);

    assertThat(commentTransitions).isEmpty();
  }

  @Test
  @SubjectAware(permissions = "repository:read:*")
  void shouldMarkCommentAsOutdated(CommentStoreFactory storeFactory) {
    Comment comment = EXISTING_COMMENT.clone();
    commentService.markAsOutdated(NAMESPACE, NAME, PULL_REQUEST_ID, comment.getId());

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.isOutdated()).isTrue();
  }

  @Test
  @SubjectAware(permissions = "repository:read:*")
  void shouldNotMarkTaskTodoAsOutdated(CommentStoreFactory storeFactory) {
    EXISTING_COMMENT.setType(TASK_TODO);
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    commentService.markAsOutdated(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId());

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.isOutdated()).isFalse();
  }

  @Test
  @SubjectAware(permissions = "repository:read:*")
  void shouldNotMarkTaskDoneAsOutdated(CommentStoreFactory storeFactory) {
    EXISTING_COMMENT.setType(TASK_DONE);
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    commentService.markAsOutdated(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId());

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.isOutdated()).isFalse();
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldNotMarkAlreadyOutdatedComments(CommentStoreFactory storeFactory) {
    EXISTING_COMMENT.setOutdated(true);
    putCommentInStore(storeFactory, "1", EXISTING_COMMENT);

    commentService.markAsOutdated(NAMESPACE, NAME, PULL_REQUEST_ID, "1");

    Comment storedComment = readCommentFromStore(storeFactory, "1");
    assertThat(storedComment.isOutdated()).isTrue();
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldAddCommentOnMergeEvent(CommentStoreFactory storeFactory) {
    commentService.addCommentOnMerge(new PullRequestMergedEvent(REPOSITORY, mockPullRequest()));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("merged");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldAddCommentOnReopenEvent(CommentStoreFactory storeFactory) {
    commentService.addCommentOnReopen(new PullRequestReopenedEvent(REPOSITORY, mockPullRequest()));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("reopened");
  }

  @Test
  void shouldAddCommentOnEmergencyMergeEvent(CommentStoreFactory storeFactory) {
    String overrideMessage = "really urgent";
    PullRequest pullRequest = mockPullRequest();
    when(pullRequest.getOverrideMessage()).thenReturn(overrideMessage);

    commentService.addCommentOnEmergencyMerge(new PullRequestEmergencyMergedEvent(REPOSITORY, pullRequest));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo(overrideMessage);
    assertThat(storedComment.isEmergencyMerged()).isTrue();
    assertThat(storedComment.getAuthor()).isEqualTo("author");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldAddCommentOnRejectEventByUserWithComment(CommentStoreFactory storeFactory) {
    commentService.addCommentOnReject(new PullRequestRejectedEvent(REPOSITORY, mockPullRequest(), PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER, "comment"));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("rejected");
    assertThat(storedComment.getReplies()).hasSize(1);
    assertThat(storedComment.getReplies().get(0).getComment()).isEqualTo("comment");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldAddCommentOnRejectEventByUserWithoutComment(CommentStoreFactory storeFactory) {
    commentService.addCommentOnReject(new PullRequestRejectedEvent(REPOSITORY, mockPullRequest(), PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER, null));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("rejected");
    assertThat(storedComment.getReplies()).isEmpty();
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldAddCommentOnRejectEventByUserWithEmptyComment(CommentStoreFactory storeFactory) {
    commentService.addCommentOnReject(new PullRequestRejectedEvent(REPOSITORY, mockPullRequest(), PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER, ""));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("rejected");
    assertThat(storedComment.getReplies()).isEmpty();
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldAddCommentOnRejectEventByDeletedSourceBranch(CommentStoreFactory storeFactory) {
    PullRequest pullRequest = mockPullRequest();
    commentService.addCommentOnReject(new PullRequestRejectedEvent(REPOSITORY, pullRequest, PullRequestRejectedEvent.RejectionCause.SOURCE_BRANCH_DELETED));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("sourceDeleted");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldAddCommentOnRejectEventByDeletedTargetBranch(CommentStoreFactory storeFactory) {
    PullRequest pullRequest = mockPullRequest();

    commentService.addCommentOnReject(new PullRequestRejectedEvent(REPOSITORY, pullRequest, PullRequestRejectedEvent.RejectionCause.TARGET_BRANCH_DELETED));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("targetDeleted");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldAddCommentOnStatusToDraftEvent(CommentStoreFactory storeFactory) {
    PullRequest pullRequest = mockPullRequest();

    commentService.addCommentOnStatusChanged(new PullRequestStatusChangedEvent(REPOSITORY, pullRequest, PullRequestStatus.DRAFT));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("statusToDraft");
  }

  @Test
  @SubjectAware(permissions = "*")
  void shouldAddCommentOnStatusToOpenEvent(CommentStoreFactory storeFactory) {
    PullRequest pullRequest = mockPullRequest();

    commentService.addCommentOnStatusChanged(new PullRequestStatusChangedEvent(REPOSITORY, pullRequest, PullRequestStatus.OPEN));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("statusToOpen");
  }

  @Test
  @SubjectAware(permissions = "repository:read:*")
  void shouldAddSystemCommentForTargetBranchChange(CommentStoreFactory storeFactory) {
    commentService.addCommentOnTargetBranchChange(
      new PullRequestEvent(
        REPOSITORY,
        new PullRequest(PULL_REQUEST_ID, "feature", "develop"),
        new PullRequest(PULL_REQUEST_ID, "feature", "master"),
        HandlerEventType.MODIFY));

    Comment storedComment = readCommentFromStore(storeFactory, "2");
    assertThat(storedComment.getComment()).isEqualTo("targetChanged");
    assertThat(storedComment.isSystemComment()).isTrue();
    assertThat(storedComment.getSystemCommentParameters()).containsAllEntriesOf(Map.of("oldTarget", "master", "newTarget", "develop"));
  }

  private PullRequest mockPullRequest() {
    PullRequest mock = mock(PullRequest.class);
    when(mock.getId()).thenReturn(PULL_REQUEST_ID);
    return mock;
  }

  private void assertMentionEventFiredAndMentionsParsedToDisplayNames(String expected) {
    Optional<BasicCommentEvent> mentionEvent = eventCaptor.getAllValues().stream().filter(event -> event instanceof MentionEvent).findFirst();
    assertThat(mentionEvent).isPresent();
    assertThat(mentionEvent.get().getItem().getComment()).isEqualTo(expected);
  }
  
  private Comment readCommentFromStore(CommentStoreFactory storeFactory, String commentId) {
    try (QueryableMutableStore<Comment> store = storeFactory.getMutable(REPOSITORY.getId(), PULL_REQUEST_ID)) {
      return store.get(commentId);
    }
  }

  private void putCommentInStore(CommentStoreFactory storeFactory, String id, Comment comment) {
    try (QueryableMutableStore<Comment> store = storeFactory.getMutable(REPOSITORY.getId(), PULL_REQUEST_ID)) {
      store.put(id, comment);
    }
  }

  private Map<String, Comment> getAllCommentsFromStore(CommentStoreFactory storeFactory) {
    try (QueryableMutableStore<Comment> store = storeFactory.getMutable(REPOSITORY.getId(), PULL_REQUEST_ID)) {
      return store.getAll();
    }
  }
}
