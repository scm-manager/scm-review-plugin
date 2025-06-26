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

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.github.sdorra.jse.ShiroExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Modifications;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryHookEvent;
import sonia.scm.repository.RepositoryHookType;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.HookContext;
import sonia.scm.store.QueryableStoreExtension;
import sonia.scm.user.User;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class, QueryableStoreExtension.class})
@QueryableStoreExtension.QueryableTypes(PullRequestChange.class)
class PullRequestChangeServiceTest {

  private final Repository repository = RepositoryTestData.create42Puzzle();
  private final NamespaceAndName namespaceAndName = new NamespaceAndName(repository.getNamespace(), repository.getName());

  @Mock
  private RepositoryResolver repositoryResolver;

  @Mock
  private PullRequestService pullRequestService;

  @Mock
  private Subject subject;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private HookContext hookContext;

  private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

  private PullRequestChangeService changeService;

  private final User trainerRed = new User("Trainer Red", "Trainer Red Display", "trainer@red.com");

  @BeforeEach
  void setUp(PullRequestChangeStoreFactory prChangeStoreFactory) {
    ThreadContext.bind(subject);
    changeService = new PullRequestChangeService(repositoryResolver, prChangeStoreFactory, clock, pullRequestService);

    lenient().when(repositoryResolver.resolve(namespaceAndName)).thenReturn(repository);

    PrincipalCollection collection = mock(PrincipalCollection.class);
    lenient().when(subject.getPrincipals()).thenReturn(collection);
    lenient().when(collection.oneByType(User.class)).thenReturn(trainerRed);
  }

  @AfterEach
  void unbindSubject() {
    ThreadContext.unbindSubject();
  }

  @Test
  void shouldGetAllAddedChanges() {
    String pullRequestId1 = "1";
    String pullRequestId2 = "2";

    List<PullRequestChange> changes = List.of(
      new PullRequestChange(
        pullRequestId1,
        "username",
        "displayName",
        "email@email.com",
        Instant.now(),
        "previous",
        "current",
        "property",
        Map.of("key1", "value1")
      ),
      new PullRequestChange(
        pullRequestId1,
        "username2",
        "displayName2",
        "email2@email.com",
        Instant.now(),
        "previous2",
        "current2",
        "property2",
        Map.of("key2", "value2")
      ),
      new PullRequestChange(
        pullRequestId2,
        "username3",
        "displayName3",
        "email3@email.com",
        Instant.now(),
        "previous3",
        "current3",
        "property3",
        Map.of("key3", "value3")
      )
    );

    changes.forEach(change -> changeService.addPullRequestChange(
        namespaceAndName,
        change
      )
    );

    List<PullRequestChange> changesOfPr1 = changeService.getAllChangesOfPullRequest(namespaceAndName, pullRequestId1);
    List<PullRequestChange> changesOfPr2 = changeService.getAllChangesOfPullRequest(namespaceAndName, pullRequestId2);

    assertThat(changesOfPr1).usingRecursiveComparison().isEqualTo(List.of(changes.get(0), changes.get(1)));
    assertThat(changesOfPr2).usingRecursiveComparison().isEqualTo(List.of(changes.get(2)));
  }

  @Test
  void shouldAddAllChanges() {
    String pullRequestId1 = "1";

    List<PullRequestChange> changes = List.of(
      new PullRequestChange(
        pullRequestId1,
        "username",
        "displayName",
        "email@email.com",
        Instant.now(),
        "previous",
        "current",
        "property",
        Map.of("key1", "value1")
      ),
      new PullRequestChange(
        pullRequestId1,
        "username2",
        "displayName2",
        "email2@email.com",
        Instant.now(),
        "previous2",
        "current2",
        "property2",
        Map.of("key2", "value2")
      ),
      new PullRequestChange(
        pullRequestId1,
        "username3",
        "displayName3",
        "email3@email.com",
        Instant.now(),
        "previous3",
        "current3",
        "property3",
        Map.of("key3", "value3")
      )
    );

    changeService.addAllPullRequestChanges(namespaceAndName, pullRequestId1, changes);

    List<PullRequestChange> changesOfPr1 = changeService.getAllChangesOfPullRequest(namespaceAndName, pullRequestId1);

    assertThat(changesOfPr1).usingRecursiveComparison().isEqualTo(changes);
  }

  @Nested
  class OnPullRequestModifiedTests {


    @Test
    void shouldNotTrackChangesBecauseEventIsNotModification() {
      PullRequest pr = TestData.createPullRequest();
      PullRequestEvent event = new PullRequestEvent(repository, pr, null, HandlerEventType.CREATE);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).hasSize(0);
    }

    @Test
    void shouldTrackSourceBranchChange() {
      PullRequest oldPr = TestData.createPullRequest();
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setSource("feature/test");

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              oldPr.getSource(),
              changedPr.getSource(),
              "SOURCE_BRANCH",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackTargetBranchChange() {
      PullRequest oldPr = TestData.createPullRequest();
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setTarget("feature/test");

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              oldPr.getTarget(),
              changedPr.getTarget(),
              "TARGET_BRANCH",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackTitleChange() {
      PullRequest oldPr = TestData.createPullRequest();
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setTitle("Different Title");

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              oldPr.getTitle(),
              changedPr.getTitle(),
              "TITLE",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackDescriptionChange() {
      PullRequest oldPr = TestData.createPullRequest();
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setDescription("Different Description");

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              oldPr.getDescription(),
              changedPr.getDescription(),
              "DESCRIPTION",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackDeleteSourceBranchAfterMergeChange() {
      PullRequest oldPr = TestData.createPullRequest();
      oldPr.setShouldDeleteSourceBranch(true);
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setShouldDeleteSourceBranch(false);

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              "true",
              "false",
              "DELETE_SOURCE_BRANCH_AFTER_MERGE",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackPrStatusChange() {
      PullRequest oldPr = TestData.createPullRequest();
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setStatus(PullRequestStatus.REJECTED);

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              oldPr.getStatus().name(),
              changedPr.getStatus().name(),
              "PR_STATUS",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackAddedLabelChange() {
      PullRequest oldPr = TestData.createPullRequest();
      oldPr.setLabels(Set.of("feature"));
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setLabels(Set.of("feature", "documentation"));

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              null,
              "documentation",
              "LABELS",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackRemovedLabelChange() {
      PullRequest oldPr = TestData.createPullRequest();
      oldPr.setLabels(Set.of("feature", "documentation"));
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setLabels(Set.of("feature"));

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              "documentation",
              null,
              "LABELS",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackAddedReviewerChange() {
      PullRequest oldPr = TestData.createPullRequest();
      oldPr.setReviewer(Map.of("Trainer Blue", true));
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setReviewer(Map.of("Trainer Blue", true, "Trainer Red", true));

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              null,
              "PullRequestChangeService.Reviewer(username=Trainer Red, approved=true)",
              "REVIEWER",
              Map.of("currentUsername", "Trainer Red", "currentApproved", "true")
            )
          )
        );
    }

    @Test
    void shouldTrackRemovedReviewerChange() {
      PullRequest oldPr = TestData.createPullRequest();
      oldPr.setReviewer(Map.of("Trainer Blue", true, "Trainer Red", true));
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setReviewer(Map.of("Trainer Blue", true));

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              "PullRequestChangeService.Reviewer(username=Trainer Red, approved=true)",
              null,
              "REVIEWER",
              Map.of("previousUsername", "Trainer Red", "previousApproved", "true")
            )
          )
        );
    }

    @Test
    void shouldTrackReviewerApprovalChange() {
      PullRequest oldPr = TestData.createPullRequest();
      oldPr.setReviewer(Map.of("Trainer Red", true));
      PullRequest changedPr = TestData.createPullRequest();
      changedPr.setReviewer(Map.of("Trainer Red", false));

      PullRequestEvent event = new PullRequestEvent(repository, changedPr, oldPr, HandlerEventType.MODIFY);
      changeService.onPullRequestModified(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, changedPr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              changedPr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              "PullRequestChangeService.Reviewer(username=Trainer Red, approved=true)",
              "PullRequestChangeService.Reviewer(username=Trainer Red, approved=false)",
              "REVIEWER",
              Map.of(
                "previousUsername", "Trainer Red", "previousApproved", "true",
                "currentUsername", "Trainer Red", "currentApproved", "false"
              )
            )
          )
        );
    }
  }

  @Nested
  class OnReplyEventTests {

    @Test
    void shouldTrackAddedReplyChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment rootComment = TestData.createComment();
      Reply reply = Reply.createNewReply("new Reply");

      ReplyEvent event = new ReplyEvent(repository, pr, reply, null, rootComment, HandlerEventType.CREATE);
      changeService.onReplyEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              null,
              reply.getComment(),
              "REPLY",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackDeletedReplyChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment rootComment = TestData.createComment();
      Reply deletedReply = Reply.createNewReply("deleted Reply");

      ReplyEvent event = new ReplyEvent(repository, pr, null, deletedReply, rootComment, HandlerEventType.DELETE);
      changeService.onReplyEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              deletedReply.getComment(),
              null,
              "REPLY",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackEditReplyChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment rootComment = TestData.createComment();
      Reply oldReply = Reply.createNewReply("old Reply");
      Reply newReply = Reply.createNewReply("new Reply");

      ReplyEvent event = new ReplyEvent(repository, pr, newReply, oldReply, rootComment, HandlerEventType.MODIFY);
      changeService.onReplyEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              oldReply.getComment(),
              newReply.getComment(),
              "REPLY",
              null
            )
          )
        );
    }
  }

  @Nested
  class OnCommentEventTests {

    @Test
    void shouldTrackAddedTaskChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment newComment = TestData.createComment();
      newComment.setType(CommentType.TASK_TODO);

      CommentEvent event = new CommentEvent(repository, pr, newComment, null, HandlerEventType.CREATE);
      changeService.onCommentEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              null,
              newComment.getComment(),
              "TASK",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackDeletedTaskChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment deletedComment = TestData.createComment();
      deletedComment.setType(CommentType.TASK_TODO);

      CommentEvent event = new CommentEvent(repository, pr, null, deletedComment, HandlerEventType.DELETE);
      changeService.onCommentEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              deletedComment.getComment(),
              null,
              "TASK",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackEditTaskChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment oldComment = TestData.createComment();
      oldComment.setType(CommentType.TASK_TODO);
      Comment changedComment = TestData.createComment();
      changedComment.setType(CommentType.TASK_TODO);
      changedComment.setComment("changed comment");

      CommentEvent event = new CommentEvent(repository, pr, changedComment, oldComment, HandlerEventType.MODIFY);
      changeService.onCommentEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              oldComment.getComment(),
              changedComment.getComment(),
              "TASK",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackTaskToCommentChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment oldComment = TestData.createComment();
      oldComment.setType(CommentType.TASK_TODO);
      Comment changedComment = TestData.createComment();
      changedComment.setType(CommentType.COMMENT);

      CommentEvent event = new CommentEvent(repository, pr, changedComment, oldComment, HandlerEventType.MODIFY);
      changeService.onCommentEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              oldComment.getType().name(),
              changedComment.getType().name(),
              "COMMENT_TYPE",
              Map.of(
                "comment", changedComment.getComment()
              )
            )
          )
        );
    }

    @Test
    void shouldTrackAddedCommentChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment newComment = TestData.createComment();

      CommentEvent event = new CommentEvent(repository, pr, newComment, null, HandlerEventType.CREATE);
      changeService.onCommentEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              null,
              newComment.getComment(),
              "COMMENT",
              null
            )
          )
        );
    }

    @Test
    void shouldIgnoreAddedSystemCommentChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment newComment = TestData.createSystemComment();

      CommentEvent event = new CommentEvent(repository, pr, newComment, null, HandlerEventType.CREATE);
      changeService.onCommentEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).hasSize(0);
    }

    @Test
    void shouldTrackDeletedCommentChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment deletedComment = TestData.createComment();

      CommentEvent event = new CommentEvent(repository, pr, null, deletedComment, HandlerEventType.DELETE);
      changeService.onCommentEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              deletedComment.getComment(),
              null,
              "COMMENT",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackEditCommentChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment oldComment = TestData.createComment();
      Comment changedComment = TestData.createComment();
      changedComment.setComment("changed comment");

      CommentEvent event = new CommentEvent(repository, pr, changedComment, oldComment, HandlerEventType.MODIFY);
      changeService.onCommentEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              oldComment.getComment(),
              changedComment.getComment(),
              "COMMENT",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackCommentToTaskChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment oldComment = TestData.createComment();
      Comment changedComment = TestData.createComment();
      changedComment.setType(CommentType.TASK_TODO);

      CommentEvent event = new CommentEvent(repository, pr, changedComment, oldComment, HandlerEventType.MODIFY);
      changeService.onCommentEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              oldComment.getType().name(),
              changedComment.getType().name(),
              "COMMENT_TYPE",
              Map.of(
                "comment", changedComment.getComment()
              )
            )
          )
        );
    }

    @Test
    void shouldIgnoreSystemCommentToTaskChange() {
      PullRequest pr = TestData.createPullRequest();
      Comment oldComment = TestData.createSystemComment();
      oldComment.setType(CommentType.COMMENT);
      Comment taskComment = TestData.createSystemComment();
      taskComment.setType(CommentType.TASK_TODO);

      CommentEvent event = new CommentEvent(repository, pr, taskComment, oldComment, HandlerEventType.CREATE);
      changeService.onCommentEvent(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).hasSize(0);
    }
  }

  @Nested
  class OnSubscribedEventTests {

    @Test
    void shouldTrackAddedSubscriberChange() {
      PullRequest pr = TestData.createPullRequest();

      PullRequestSubscribedEvent event = new PullRequestSubscribedEvent(
        repository, pr, new User("Trainer Red"), PullRequestSubscribedEvent.EventType.SUBSCRIBED
      );
      changeService.onSubscribed(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              null,
              "Trainer Red",
              "SUBSCRIBER",
              null
            )
          )
        );
    }

    @Test
    void shouldTrackRemovedSubscriberChange() {
      PullRequest pr = TestData.createPullRequest();

      PullRequestSubscribedEvent event = new PullRequestSubscribedEvent(
        repository, pr, new User("Trainer Red"), PullRequestSubscribedEvent.EventType.UNSUBSCRIBED
      );
      changeService.onSubscribed(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              "Trainer Red",
              null,
              "SUBSCRIBER",
              null
            )
          )
        );
    }
  }

  @Nested
  class OnReviewMarkEventTests {

    @Test
    void shouldTrackAddedReviewMarksChange() {
      ReviewMark newMark = new ReviewMark("fileA", "Trainer Red");
      PullRequest pr = TestData.createPullRequest();

      PullRequestReviewMarkEvent event = new PullRequestReviewMarkEvent(
        repository, pr, newMark, PullRequestReviewMarkEvent.EventType.ADDED
      );
      changeService.onReviewMark(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              null,
              new ReviewMark("fileA", "Trainer Red").toString(),
              "REVIEW_MARKS",
              Map.of("file", "fileA", "user", "Trainer Red")
            )
          )
        );
    }

    @Test
    void shouldTrackRemovedReviewMarksChange() {
      ReviewMark newMark = new ReviewMark("fileA", "Trainer Red");
      PullRequest pr = TestData.createPullRequest();

      PullRequestReviewMarkEvent event = new PullRequestReviewMarkEvent(
        repository, pr, newMark, PullRequestReviewMarkEvent.EventType.REMOVED
      );
      changeService.onReviewMark(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              new ReviewMark("fileA", "Trainer Red").toString(),
              null,
              "REVIEW_MARKS",
              Map.of("file", "fileA", "user", "Trainer Red")
            )
          )
        );
    }
  }

  @Nested
  class OnPullRequestReopened {

    @Test
    void shouldTrackReopenedPullRequest() {
      PullRequest pr = TestData.createPullRequest();

      PullRequestReopenedEvent event = new PullRequestReopenedEvent(repository, pr);
      changeService.onPullRequestReopened(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              PullRequestStatus.REJECTED.name(),
              PullRequestStatus.OPEN.name(),
              "PR_STATUS",
              null
            )
          )
        );
    }
  }

  @Nested
  class OnPullRequestRejected {

    @Test
    void shouldTrackPullRequestRejectionWithMessage() {
      PullRequest pr = TestData.createPullRequest();

      PullRequestRejectedEvent event = new PullRequestRejectedEvent(
        repository, pr, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER, "Rejection Message", PullRequestStatus.OPEN
      );
      changeService.onPullRequestRejected(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              PullRequestStatus.OPEN.name(),
              PullRequestStatus.REJECTED.name(),
              "PR_STATUS",
              Map.of(
                "rejectionCause", event.getCause().name(),
                "rejectionMessage", event.getMessage()
              )
            )
          )
        );
    }

    @Test
    void shouldTrackPullRequestRejectionWithoutMessage() {
      PullRequest pr = TestData.createPullRequest();

      PullRequestRejectedEvent event = new PullRequestRejectedEvent(
        repository, pr, PullRequestRejectedEvent.RejectionCause.REJECTED_BY_USER, null, PullRequestStatus.OPEN
      );
      changeService.onPullRequestRejected(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              PullRequestStatus.OPEN.name(),
              PullRequestStatus.REJECTED.name(),
              "PR_STATUS",
              Map.of(
                "rejectionCause", event.getCause().name()
              )
            )
          )
        );
    }
  }

  @Nested
  class OnPostPushTests {

    @Test
    void shouldTrackChangedRevisionOfSourceBranchForEachPr() {
      String featurePrevRevision = "1234567890";
      String featureCurrentRevision = "0987654321";
      PullRequest featurePr1 = TestData.createPullRequest("1");
      featurePr1.setSource("feature");
      PullRequest featurePr2 = TestData.createPullRequest("2");
      featurePr2.setSource("feature");

      String supportPrevRevision = "abcdef";
      String supportCurrentRevision = "fedcba";
      PullRequest supportPr1 = TestData.createPullRequest("3");
      featurePr1.setSource("support");
      PullRequest supportPr2 = TestData.createPullRequest("4");
      featurePr2.setSource("support");

      when(hookContext.getBranchProvider().getCreatedOrModified())
        .thenReturn(List.of(featurePr1.getSource(), supportPr1.getSource()));
      when(hookContext.getModificationsProvider().getModifications(featurePr1.getSource()))
        .thenReturn(new Modifications(featurePrevRevision, featureCurrentRevision, List.of()));
      when(hookContext.getModificationsProvider().getModifications(supportPr1.getSource()))
        .thenReturn(new Modifications(supportPrevRevision, supportCurrentRevision, List.of()));
      when(pullRequestService.getAll(repository.getNamespace(), repository.getName()))
        .thenReturn(List.of(featurePr1, featurePr2, supportPr1, supportPr2));
      when(pullRequestService.supportsPullRequests(repository)).thenReturn(true);

      PostReceiveRepositoryHookEvent event = new PostReceiveRepositoryHookEvent(
        new RepositoryHookEvent(
          hookContext,
          repository,
          RepositoryHookType.POST_RECEIVE)
      );
      changeService.onPostPush(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, featurePr1.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              featurePr1.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              featurePrevRevision,
              featureCurrentRevision,
              "SOURCE_BRANCH_REVISION",
              null
            )
          )
        );

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, featurePr2.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              featurePr2.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              featurePrevRevision,
              featureCurrentRevision,
              "SOURCE_BRANCH_REVISION",
              null
            )
          )
        );

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, supportPr1.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              supportPr1.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              supportPrevRevision,
              supportCurrentRevision,
              "SOURCE_BRANCH_REVISION",
              null
            )
          )
        );

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, supportPr2.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              supportPr2.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              supportPrevRevision,
              supportCurrentRevision,
              "SOURCE_BRANCH_REVISION",
              null
            )
          )
        );
    }
  }

  @Nested
  class OnApprovalEventTests {

    @Test
    void shouldTrackAddedApprovalChange() {
      PullRequest pr = TestData.createPullRequest();

      PullRequestApprovalEvent event = new PullRequestApprovalEvent(
        repository, pr, new User("Trainer Red"), false, PullRequestApprovalEvent.ApprovalCause.APPROVED
      );
      changeService.onApproval(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              "PullRequestChangeService.Reviewer(username=Trainer Red, approved=false)",
              "PullRequestChangeService.Reviewer(username=Trainer Red, approved=true)",
              "REVIEWER",
              Map.of(
                "previousUsername", "Trainer Red", "previousApproved", "false",
                "currentUsername", "Trainer Red", "currentApproved", "true"
              )
            )
          )
        );
    }

    @Test
    void shouldTrackRemovedApprovalChange() {
      PullRequest pr = TestData.createPullRequest();

      PullRequestApprovalEvent event = new PullRequestApprovalEvent(
        repository, pr, new User("Trainer Red"), false, PullRequestApprovalEvent.ApprovalCause.APPROVAL_REMOVED
      );
      changeService.onApproval(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              "PullRequestChangeService.Reviewer(username=Trainer Red, approved=true)",
              "PullRequestChangeService.Reviewer(username=Trainer Red, approved=false)",
              "REVIEWER",
              Map.of(
                "previousUsername", "Trainer Red", "previousApproved", "true",
                "currentUsername", "Trainer Red", "currentApproved", "false"
              )
            )
          )
        );
    }

    @Test
    void shouldTrackAddedReviewerByApprovalChange() {
      PullRequest pr = TestData.createPullRequest();

      PullRequestApprovalEvent event = new PullRequestApprovalEvent(
        repository, pr, new User("Trainer Red"), true, PullRequestApprovalEvent.ApprovalCause.APPROVED
      );
      changeService.onApproval(event);

      assertThat(changeService.getAllChangesOfPullRequest(namespaceAndName, pr.getId())).usingRecursiveComparison()
        .isEqualTo(
          List.of(
            new PullRequestChange(
              pr.getId(),
              trainerRed.getId(),
              trainerRed.getDisplayName(),
              trainerRed.getMail(),
              Instant.now(clock),
              null,
              "PullRequestChangeService.Reviewer(username=Trainer Red, approved=true)",
              "REVIEWER",
              Map.of(
                "currentUsername", "Trainer Red", "currentApproved", "true"
              )
            )
          )
        );
    }
  }
}
