package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.api.MentionMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestMergedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestRejectedEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.HandlerEventType;
import sonia.scm.NotFoundException;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

import static com.cloudogu.scm.review.comment.service.Comment.createComment;
import static com.cloudogu.scm.review.comment.service.CommentTransition.MAKE_TASK;
import static com.cloudogu.scm.review.comment.service.CommentTransition.SET_DONE;
import static com.cloudogu.scm.review.comment.service.CommentType.TASK_DONE;
import static com.cloudogu.scm.review.comment.service.Reply.createReply;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.Collections.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware(
  configuration = "classpath:com/cloudogu/scm/review/shiro.ini",
  password = "secret"
)
@RunWith(MockitoJUnitRunner.class)
public class CommentServiceTest {

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
  }

  @Rule
  public ShiroRule shiroRule = new ShiroRule();

  @Mock
  private RepositoryResolver repositoryResolver;

  @Mock
  private CommentStoreFactory storeFactory;

  @Mock
  private CommentStore store;

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

  @Captor
  private ArgumentCaptor<Comment> rootCommentCaptor;
  @Captor
  private ArgumentCaptor<BasicCommentEvent> eventCaptor;

  private CommentService commentService;

  @Before
  public void init() {
    when(storeFactory.create(any())).thenReturn(store);
    doNothing().when(eventBus).post(eventCaptor.capture());

    when(repositoryResolver.resolve(REPOSITORY.getNamespaceAndName())).thenReturn(REPOSITORY);
    commentService = new CommentService(repositoryResolver, pullRequestService, storeFactory, keyGenerator, eventBus, commentInitializer, mentionMapper);

    lenient().when(store.getAll(PULL_REQUEST_ID)).thenReturn(singletonList(EXISTING_COMMENT));

    lenient().doAnswer(invocation -> {
      BasicComment comment = invocation.getArgument(0);
      comment.setDate(NOW);
      comment.setAuthor(author);
      return null;
    }).when(commentInitializer).initialize(any(), any(), any());
  }

  @Test
  public void shouldGetComment() {
    Comment comment = commentService.get(NAMESPACE, NAME, PULL_REQUEST_ID, COMMENT_ID);

    assertThat(comment).isEqualTo(EXISTING_COMMENT);
  }

  @Test(expected = NotFoundException.class)
  public void shouldFailGetIfNoCommentExists() {
    commentService.get(NAMESPACE, NAME, PULL_REQUEST_ID, "2");
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldAddComment() {
    when(store.add(eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");

    Comment comment = createComment("2", "2. comment", author, new Location());
    commentService.add(NAMESPACE, NAME, PULL_REQUEST_ID, comment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getAuthor()).isEqualTo("author");
    assertThat(storedComment.getDate()).isEqualTo(NOW);
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldExtractMentionsOnAddComment() {
    when(store.add(eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("trillian"));

    Comment comment = createComment("2", "2. comment @[trillian]", author, new Location());
    commentService.add(NAMESPACE, NAME, PULL_REQUEST_ID, comment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getMentionUserIds()).contains("trillian");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldTriggerMentionEventIfNewMentionAdded() {
    when(store.add(eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("trillian"));

    Comment comment = createComment("2", "2. comment @[trillian]", author, new Location());
    commentService.add(NAMESPACE, NAME, PULL_REQUEST_ID, comment);

    assertThat(eventCaptor.getAllValues().size()).isEqualTo(2);
    assertTrue(eventCaptor.getAllValues().stream().anyMatch(event -> event instanceof MentionEvent));
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldPostEventForNewRootComment() {
    when(store.add(eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");

    Comment comment = createComment("2", "2. comment", author, new Location());
    commentService.add(NAMESPACE, NAME, PULL_REQUEST_ID, comment);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.CREATE);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian")
  public void shouldFailIfUserHasNoPermissionToCreateComment() {
    Comment comment = createComment("2", "2. comment", author, new Location());

    commentService.add(REPOSITORY.getNamespace(), REPOSITORY.getName(), PULL_REQUEST_ID, comment);
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldAddReplyToParentComment() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());

    Reply reply = createReply("new reply", "1. comment", author);

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getReplies()).hasSize(2);
    assertThat(storedComment.getReplies()).contains(reply);
    assertThat(storedComment.getReplies().get(1).getId()).isNotEqualTo("new reply");
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldExtractMentionsOnCreateReply() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("dent"));
    Reply reply = createReply("new reply", "1. comment @[dent]", author);

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getReplies()).hasSize(2);
    assertThat(storedComment.getReplies()).contains(reply);
    assertThat(storedComment.getReplies().get(1).getId()).isNotEqualTo("new reply @[dent]");
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldTriggerMentionEventIfNewMentionAddedOnCreateReply() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("dent"));
    Reply reply = createReply("new reply", "1. comment @[dent]", author);

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);

    assertThat(eventCaptor.getAllValues().size()).isEqualTo(2);
    assertTrue(eventCaptor.getAllValues().stream().anyMatch(event -> event instanceof MentionEvent));
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldPostEventForNewReply() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());

    Reply reply = createReply("1", "1. comment", author);

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.CREATE);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian")
  public void shouldFailIfUserHasNoPermissionTocreateReply() {
    Reply reply = createReply("1", "1. comment", author);

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldModifyRootCommentText() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setComment("new comment");
    changedRootComment.setMentionUserIds(EMPTY_SET);

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getComment()).isEqualTo("new comment");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldExtractMentionsOnModifyRootCommentText() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("trillian"));
    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setComment("new comment @[trillian]");
    changedRootComment.setMentionUserIds(EMPTY_SET);

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getComment()).isEqualTo("new comment @[trillian]");
    assertThat(storedComment.getMentionUserIds()).contains("trillian");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldTriggerMentionEventIfNewMentionAddedOnModify() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("trillian"));
    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setComment("new comment @[trillian]");
    changedRootComment.setMentionUserIds(EMPTY_SET);

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment);

    assertThat(eventCaptor.getAllValues().size()).isEqualTo(2);
    assertTrue(eventCaptor.getAllValues().stream().anyMatch(event -> event instanceof MentionEvent));
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldModifyRootCommentDoneFlag() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    EXISTING_COMMENT.setType(CommentType.TASK_TODO);

    commentService.transform(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), SET_DONE);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getType()).isEqualTo(TASK_DONE);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldTriggerModifyEventForRootComment() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    EXISTING_COMMENT.setType(CommentType.TASK_TODO);

    commentService.transform(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), SET_DONE);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.MODIFY);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldKeepAuthorAndDateWhenModifyingRootComment() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setAuthor("new author");
    changedRootComment.setDate(ofEpochMilli(123));
    changedRootComment.setMentionUserIds(EMPTY_SET);

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getAuthor()).isEqualTo(EXISTING_COMMENT.getAuthor());
    assertThat(storedComment.getDate()).isEqualTo(EXISTING_COMMENT.getDate());
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldModifyCommentWhenUserHasNoModifyPermissionButCommentIsHerOwn() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    EXISTING_COMMENT.setAuthor("createCommentUser");
    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setMentionUserIds(EMPTY_SET);

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "createCommentUser")
  public void shouldFailModifyingRootCommentWhenUserHasNoPermission() {
    Comment changedRootComment = EXISTING_COMMENT.clone();

    commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId(), changedRootComment);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldFailModifyWhenCommentDoesNotExist() {
    Comment changedRootComment = EXISTING_COMMENT.clone();
    changedRootComment.setComment("new comment");

    Assertions.assertThrows(NotFoundException.class, () -> commentService.modifyComment(NAMESPACE, NAME, PULL_REQUEST_ID, "no such id", changedRootComment));

    verify(store, never()).update(any(), any());
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldDeleteRootComment() {
    EXISTING_COMMENT.setReplies(emptyList());

    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId());

    verify(store).delete(PULL_REQUEST_ID, EXISTING_COMMENT.getId());
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldTriggerDeleteEvent() {
    EXISTING_COMMENT.setReplies(emptyList());

    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId());

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.DELETE);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian")
  public void shouldFailWhenDeletingRootCommentWithoutPermission() {
    EXISTING_COMMENT.setReplies(emptyList());

    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId());
  }

  @Test
  public void shouldNotFailWhenDeletingNotExistingRootComment() {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, "no such id");

    verify(store, never()).delete(any(), any());
  }

  @Test(expected = ScmConstraintViolationException.class)
  @SubjectAware(username = "dent")
  public void shouldNotDeleteIfReplyExists() {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId());
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldModifyReplyText() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    Reply changedReply = EXISTING_REPLY.clone();
    changedReply.setComment("new comment");

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getReplies()).hasSize(1);
    assertThat(storedComment.getReplies().get(0).getComment()).isEqualTo("new comment");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldExtractMentionsOnModifyReplyText() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("trillian", "dent"));

    Reply changedReply = EXISTING_REPLY.clone();
    changedReply.setComment("new comment @[trillian] @[dent]");

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getReplies()).hasSize(1);
    assertThat(storedComment.getReplies().get(0).getComment()).isEqualTo("new comment @[trillian] @[dent]");
    assertThat(storedComment.getReplies().get(0).getMentionUserIds()).contains("trillian", "dent");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldTriggerMentionEventOnModifyReplyText() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    when(mentionMapper.extractMentionsFromComment(anyString())).thenReturn(ImmutableSet.of("trillian", "dent"));

    Reply changedReply = EXISTING_REPLY.clone();
    changedReply.setComment("new comment @[trillian] @[dent]");

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    assertThat(eventCaptor.getAllValues().size()).isEqualTo(2);
    assertTrue(eventCaptor.getAllValues().stream().anyMatch(event -> event instanceof MentionEvent));
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldTriggerModifyEventForReplies() {
    Reply changedReply = EXISTING_REPLY.clone();
    changedReply.setComment("new comment");

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.MODIFY);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldModifyReplyDoneFlag() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    Reply changedReply = EXISTING_REPLY.clone();

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getReplies()).hasSize(1);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "createCommentUser")
  public void shouldFailModifyingReplyWhenUserHasNoPermission() {
    Reply changedReply = EXISTING_REPLY.clone();

    commentService.modifyReply(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldDeleteExistingReply() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());

    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId());

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getReplies()).isEmpty();
  }

  @Test(expected = ScmConstraintViolationException.class)
  @SubjectAware(username = "dent")
  public void shouldFailDeletingSystemComment() {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_COMMENT.getId());
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldTriggerDeleteEventForReply() {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId());

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.DELETE);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldNotTriggerEventForDeletingNotExistingComment() {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, "no such comment");

    assertThat(eventCaptor.getAllValues()).isEmpty();
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian")
  public void shouldFailDeletingExistingReplyWithoutPermission() {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId());
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldAddChangedStatusComment() {
    when(store.add(eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");

    commentService.addStatusChangedComment(REPOSITORY, PULL_REQUEST_ID, SystemCommentType.MERGED);
    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getComment()).isEqualTo("merged");
    assertThat(storedComment.isSystemComment()).isTrue();
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetAllComments() {
    Collection<Comment> all = commentService.getAll(NAMESPACE, NAME, PULL_REQUEST_ID);

    assertThat(all).containsExactly(EXISTING_COMMENT);
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldGiveTransitionsForAuthorizedUser() {
    Collection<CommentTransition> commentTransitions = commentService.possibleTransitions(NAMESPACE, NAME, PULL_REQUEST_ID, COMMENT_ID);

    assertThat(commentTransitions).containsExactly(MAKE_TASK);
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldGiveNoTransitionsForUnauthorizedUser() {
    Collection<CommentTransition> commentTransitions = commentService.possibleTransitions(NAMESPACE, NAME, PULL_REQUEST_ID, COMMENT_ID);

    assertThat(commentTransitions).isEmpty();
  }

  @Test
  @SubjectAware(username = "trillian")
  public void shouldMarkCommentAsOutdated() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());

    Comment comment = EXISTING_COMMENT.clone();
    commentService.markAsOutdated(NAMESPACE, NAME, PULL_REQUEST_ID, comment.getId());

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.isOutdated()).isTrue();
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldNotMarkAlreadyOutdatedComments() {
    Comment comment = EXISTING_COMMENT.clone();
    comment.setOutdated(true);

    when(store.getAll(PULL_REQUEST_ID)).thenReturn(singletonList(comment));

    commentService.markAsOutdated(NAMESPACE, NAME, PULL_REQUEST_ID, comment.getId());

    verify(store, never()).update(any(), any());
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldAddCommentOnMergeEvent() {
    when(store.add(eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");

    commentService.addCommentOnMerge(new PullRequestMergedEvent(REPOSITORY, mockPullRequest()));

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getComment()).isEqualTo("merged");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldAddCommentOnRejectEventByUser() {
    when(store.add(eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");

    commentService.addCommentOnReject(new PullRequestRejectedEvent(REPOSITORY, mockPullRequest(), PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER));

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getComment()).isEqualTo("rejected");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldAddCommentOnRejectEventByDeletedBranch() {
    when(store.add(eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");

    commentService.addCommentOnReject(new PullRequestRejectedEvent(REPOSITORY, mockPullRequest(), PullRequestRejectedEvent.RejectionCause.BRANCH_DELETED));

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    Comment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getComment()).isEqualTo("sourceDeleted");
  }

  private PullRequest mockPullRequest() {
    PullRequest mock = mock(PullRequest.class);
    when(mock.getId()).thenReturn(PULL_REQUEST_ID);
    return mock;
  }
}
