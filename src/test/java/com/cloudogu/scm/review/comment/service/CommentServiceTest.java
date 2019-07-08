package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
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

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

import static com.cloudogu.scm.review.comment.service.PullRequestRootComment.createComment;
import static com.cloudogu.scm.review.comment.service.Reply.createReply;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
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
  private final PullRequestRootComment EXISTING_ROOT_COMMENT = createComment(COMMENT_ID, "1. comment", "author", new Location());
  private final Reply EXISTING_REPLY = createReply("resp_1", "1. reply", "author");

  {
    EXISTING_ROOT_COMMENT.addReply(EXISTING_REPLY);
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
  private Clock clock;

  @Captor
  private ArgumentCaptor<PullRequestRootComment> rootCommentCaptor;
  @Captor
  private ArgumentCaptor<CommentEvent> eventCaptor;

  private CommentService commentService;

  @Before
  public void init() {
    when(storeFactory.create(any())).thenReturn(store);
    when(clock.instant()).thenReturn(NOW);
    doNothing().when(eventBus).post(eventCaptor.capture());

    when(repositoryResolver.resolve(REPOSITORY.getNamespaceAndName())).thenReturn(REPOSITORY);
    commentService = new CommentService(repositoryResolver, pullRequestService, storeFactory, keyGenerator, eventBus, clock);

    lenient().when(store.getAll(PULL_REQUEST_ID)).thenReturn(singletonList(EXISTING_ROOT_COMMENT));
  }

  @Test
  public void shouldGetComment() {
    PullRequestRootComment comment = commentService.get(NAMESPACE, NAME, PULL_REQUEST_ID, COMMENT_ID);

    assertThat(comment).isEqualTo(EXISTING_ROOT_COMMENT);
  }

  @Test(expected = NotFoundException.class)
  public void shouldFailGetIfNoCommentExists() {
    commentService.get(NAMESPACE, NAME, PULL_REQUEST_ID, "2");
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldAddComment() {
    when(store.add(eq(REPOSITORY), eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");

    PullRequestRootComment comment = createComment("2", "2. comment", "author", new Location());
    commentService.add(NAMESPACE, NAME, PULL_REQUEST_ID, comment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    PullRequestRootComment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getAuthor()).isEqualTo("createCommentUser");
    assertThat(storedComment.getDate()).isEqualTo(NOW);
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldPostEventForNewRootComment() {
    when(store.add(eq(REPOSITORY), eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");

    PullRequestRootComment comment = createComment("2", "2. comment", "author", new Location());
    commentService.add(NAMESPACE, NAME, PULL_REQUEST_ID, comment);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.CREATE);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian")
  public void shouldFailIfUserHasNoPermissionToCreateComment() {
    PullRequestRootComment comment = createComment("2", "2. comment", "author", new Location());

    commentService.add(REPOSITORY.getNamespace(), REPOSITORY.getName(), PULL_REQUEST_ID, comment);
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldAddReplyToParentComment() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());

    Reply reply = createReply("new reply", "1. comment", "author");

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    PullRequestRootComment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getReplies()).hasSize(2);
    assertThat(storedComment.getReplies()).contains(reply);
    assertThat(storedComment.getReplies().get(1).getAuthor()).isEqualTo("createCommentUser");
    assertThat(storedComment.getReplies().get(1).getDate()).isEqualTo(NOW);
    assertThat(storedComment.getReplies().get(1).getId()).isNotEqualTo("new reply");
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldPostEventForNewReply() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());

    Reply reply = createReply("1", "1. comment", "author");

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.CREATE);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian")
  public void shouldFailIfUserHasNoPermissionTocreateReply() {
    Reply reply = createReply("1", "1. comment", "author");

    commentService.reply(NAMESPACE, NAME, PULL_REQUEST_ID, "1", reply);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldModifyRootCommentText() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    PullRequestRootComment changedRootComment = EXISTING_ROOT_COMMENT.clone();
    changedRootComment.setComment("new comment");

    commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId(), changedRootComment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    PullRequestRootComment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getComment()).isEqualTo("new comment");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldModifyRootCommentDoneFlag() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    PullRequestRootComment changedRootComment = EXISTING_ROOT_COMMENT.clone();
    changedRootComment.setDone(true);

    commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId(), changedRootComment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    PullRequestRootComment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.isDone()).isTrue();
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldTriggerModifyEventForRootComment() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    PullRequestRootComment changedRootComment = EXISTING_ROOT_COMMENT.clone();
    changedRootComment.setDone(true);

    commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId(), changedRootComment);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.MODIFY);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldKeepAuthorAndDateWhenModifyingRootComment() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    PullRequestRootComment changedRootComment = EXISTING_ROOT_COMMENT.clone();
    changedRootComment.setAuthor("new author");
    changedRootComment.setDate(ofEpochMilli(123));

    commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId(), changedRootComment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    PullRequestRootComment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getAuthor()).isEqualTo(EXISTING_ROOT_COMMENT.getAuthor());
    assertThat(storedComment.getDate()).isEqualTo(EXISTING_ROOT_COMMENT.getDate());
  }

  @Test
  @SubjectAware(username = "createCommentUser")
  public void shouldModifyCommentWhenUserHasNoModifyPermissionButCommentIsHerOwn() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    EXISTING_ROOT_COMMENT.setAuthor("createCommentUser");
    PullRequestRootComment changedRootComment = EXISTING_ROOT_COMMENT.clone();

    commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId(), changedRootComment);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "createCommentUser")
  public void shouldFailModifyingRootCommentWhenUserHasNoPermission() {
    PullRequestRootComment changedRootComment = EXISTING_ROOT_COMMENT.clone();

    commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId(), changedRootComment);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldFailModifyWhenCommentDoesNotExist() {
    PullRequestRootComment changedRootComment = EXISTING_ROOT_COMMENT.clone();
    changedRootComment.setComment("new comment");

    Assertions.assertThrows(NotFoundException.class, () -> commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, "no such id", changedRootComment));

    verify(store, never()).update(any(), any());
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldDeleteRootComment() {
    EXISTING_ROOT_COMMENT.setReplies(emptyList());

    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId());

    verify(store).delete(PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId());
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldTriggerDeleteEvent() {
    EXISTING_ROOT_COMMENT.setReplies(emptyList());

    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId());

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.DELETE);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "trillian")
  public void shouldFailWhenDeletingRootCommentWithoutPermission() {
    EXISTING_ROOT_COMMENT.setReplies(emptyList());

    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId());
  }

  @Test
  public void shouldNotFailWhenDeletingNotExistingRootComment() {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, "no such id");

    verify(store, never()).delete(any(), any());
  }

  @Test(expected = ScmConstraintViolationException.class)
  @SubjectAware(username = "dent")
  public void shouldNotDeleteIfReplyExists() {
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_ROOT_COMMENT.getId());
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldModifyReplyText() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    Reply changedReply = EXISTING_REPLY.clone();
    changedReply.setComment("new comment");

    commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    PullRequestRootComment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getReplies()).hasSize(1);
    assertThat(storedComment.getReplies().get(0).getComment()).isEqualTo("new comment");
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldTriggerModifyEventForReplies() {
    Reply changedReply = EXISTING_REPLY.clone();
    changedReply.setComment("new comment");

    commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    assertThat(eventCaptor.getAllValues()).hasSize(1);
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(HandlerEventType.MODIFY);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldModifyReplyDoneFlag() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());
    Reply changedReply = EXISTING_REPLY.clone();

    commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    PullRequestRootComment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getReplies()).hasSize(1);
  }

  @Test(expected = UnauthorizedException.class)
  @SubjectAware(username = "createCommentUser")
  public void shouldFailModifyingReplyWhenUserHasNoPermission() {
    Reply changedReply = EXISTING_REPLY.clone();

    commentService.modify(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId(), changedReply);
  }

  @Test
  @SubjectAware(username = "dent")
  public void shouldDeleteExistingReply() {
    doNothing().when(store).update(eq(PULL_REQUEST_ID), rootCommentCaptor.capture());

    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, EXISTING_REPLY.getId());

    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    PullRequestRootComment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getReplies()).isEmpty();
  }

  @Test(expected = ScmConstraintViolationException.class)
  @SubjectAware(username = "dent")
  public void shouldFailDeletingSystemComment() {
    PullRequestComment systemComment = EXISTING_ROOT_COMMENT;
    commentService.delete(NAMESPACE, NAME, PULL_REQUEST_ID, systemComment.getId());
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
    when(store.add(eq(REPOSITORY), eq(PULL_REQUEST_ID), rootCommentCaptor.capture())).thenReturn("newId");

    commentService.addStatusChangedComment(REPOSITORY, PULL_REQUEST_ID, SystemCommentType.MERGED);
    assertThat(rootCommentCaptor.getAllValues()).hasSize(1);
    PullRequestRootComment storedComment = rootCommentCaptor.getValue();
    assertThat(storedComment.getAuthor()).isEqualTo("trillian");
    assertThat(storedComment.getDate()).isEqualTo(NOW);
    assertThat(storedComment.getComment()).isEqualTo("merged");
    assertThat(storedComment.isSystemComment()).isTrue();
  }

  @Test
  @SubjectAware(username = "rr")
  public void shouldGetAllComments() {
    Collection<PullRequestRootComment> all = commentService.getAll(REPOSITORY.getNamespace(), REPOSITORY.getName(), PULL_REQUEST_ID);

    assertThat(all).containsExactly(EXISTING_ROOT_COMMENT);
  }
}
