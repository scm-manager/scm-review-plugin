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

import com.cloudogu.scm.review.TestData;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryHookEvent;
import sonia.scm.repository.RepositoryHookType;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.HookContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware("Trainer Red")
class PullRequestSuggestionServiceTest {

  private final Repository repository = RepositoryTestData.createHeartOfGold();
  private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

  private PullRequestSuggestionService suggestionService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private HookContext hookContext;

  @Mock
  private PullRequestService pullRequestService;

  @BeforeEach
  void setup() {
    suggestionService = new PullRequestSuggestionService(pullRequestService, clock);
  }

  @Nested
  class OnBranchPushed {

    @BeforeEach
    void setup() {
      when(pullRequestService.supportsPullRequests(repository)).thenReturn(true);
      when(hookContext.getBranchProvider().getCreatedOrModified()).thenReturn(
        List.of("feature")
      );
    }

    private PostReceiveRepositoryHookEvent createPostReceiveRepositoryHookEvent() {
      return new PostReceiveRepositoryHookEvent(
        new RepositoryHookEvent(
          hookContext,
          repository,
          RepositoryHookType.POST_RECEIVE
        )
      );
    }

    @Test
    void shouldIgnorePushBecauseRepositoryDoesNotSupportPullRequests() {
      when(pullRequestService.supportsPullRequests(repository)).thenReturn(false);
      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());
      assertThat(suggestionService.getPushEntries()).isEmpty();
    }

    @Test
    void shouldAddNewPushEntryForEachPushedBranch() {
      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(
          new PullRequestSuggestionService.PushEntry(
            repository.getId(), "feature", "Trainer Red", Instant.now(clock)
          )
        )
      );
    }

    @Test
    void shouldUpdatePushedAtForExistingPushEntry() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(),
        "feature",
        "Trainer Red",
        Instant.now(clock).minus(1, ChronoUnit.MILLIS)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(
          new PullRequestSuggestionService.PushEntry(
            repository.getId(), "feature", "Trainer Red", Instant.now(clock)
          )
        )
      );
    }

    @Test
    void shouldUpdatePushedAtForPushEntryOfCorrespondingRepositoryBranchAndPusher() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(),
        "feature",
        "Trainer Red",
        Instant.now(clock).minus(1, ChronoUnit.MILLIS)
      );
      PullRequestSuggestionService.PushEntry existingPushEntryOfDifferentRepository = new PullRequestSuggestionService.PushEntry(
        "different",
        "feature",
        "Trainer Red",
        Instant.now(clock).minus(1, ChronoUnit.MILLIS)
      );

      suggestionService.getPushEntries().add(existingPushEntryOfDifferentRepository);
      suggestionService.getPushEntries().add(existingPushEntry);

      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(
          new PullRequestSuggestionService.PushEntry(
            "different", "feature", "Trainer Red", Instant.now(clock).minus(1, ChronoUnit.MILLIS)
          ),
          new PullRequestSuggestionService.PushEntry(
            repository.getId(), "feature", "Trainer Red", Instant.now(clock)
          )
        )
      );
    }

    @Test
    void shouldAddPushEntryForSameRepositoryWithDuplicateBranchButDifferentPusher() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(), "feature", "Trainer Blue", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(
          existingPushEntry,
          new PullRequestSuggestionService.PushEntry(
            repository.getId(), "feature", "Trainer Red", Instant.now(clock)
          )
        )
      );
    }

    @Test
    void shouldAddPushEntryForSameRepositoryWithDuplicatePusherButDifferentBranch() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(), "different/branch", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(
          existingPushEntry,
          new PullRequestSuggestionService.PushEntry(
            repository.getId(), "feature", "Trainer Red", Instant.now(clock)
          )
        )
      );
    }

    @Test
    void shouldAddPushEntryForDuplicatePusherAndBranchButDifferentRepository() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        "1337", "feature", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(
          existingPushEntry,
          new PullRequestSuggestionService.PushEntry(
            repository.getId(), "feature", "Trainer Red", Instant.now(clock)
          )
        )
      );
    }

    @Test
    void shouldNotAddPushEntryBecauseRepositoryAlreadyHasPushEntryForBranchAndPusher() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(), "feature", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(existingPushEntry)
      );
    }

    @Test
    void shouldNotAddPushEntryBecausePushedBranchAlreadyHasOpenPullRequest() {
      PullRequest openPullRequest = new PullRequest("1", "feature", "develop");
      openPullRequest.setStatus(PullRequestStatus.OPEN);
      when(pullRequestService.getAll(repository.getNamespace(), repository.getName())).thenReturn(
        List.of(openPullRequest)
      );

      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());

      assertThat(suggestionService.getPushEntries()).isEmpty();
    }

    @Test
    void shouldNotAddPushEntryBecausePushedBranchAlreadyHasDraftPullRequest() {
      PullRequest draftPullRequest = new PullRequest("1", "feature", "develop");
      draftPullRequest.setStatus(PullRequestStatus.DRAFT);
      when(pullRequestService.getAll(repository.getNamespace(), repository.getName())).thenReturn(
        List.of(draftPullRequest)
      );

      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());

      assertThat(suggestionService.getPushEntries()).isEmpty();
    }

    @Test
    void shouldRemovePushEntriesOfRepositoryForDeletedBranches() {
      when(hookContext.getBranchProvider().getDeletedOrClosed()).thenReturn(
        List.of("feature")
      );

      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(), "feature", "Trainer Red", Instant.now(clock)
      );
      PullRequestSuggestionService.PushEntry existingPushEntryOtherRepository = new PullRequestSuggestionService.PushEntry(
        "other", "feature", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);
      suggestionService.getPushEntries().add(existingPushEntryOtherRepository);

      suggestionService.onBranchUpdated(createPostReceiveRepositoryHookEvent());

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(
          existingPushEntryOtherRepository
        )
      );
    }
  }

  @Nested
  class OnPullRequestCreated {

    @Test
    void shouldRemoveAllPushEntriesOfRepositoryContainingSourceBranch() {
      suggestionService.getPushEntries().add(
        new PullRequestSuggestionService.PushEntry(
          repository.getId(), "feature", "Trainer Red", Instant.now(clock)
        )
      );

      suggestionService.getPushEntries().add(
        new PullRequestSuggestionService.PushEntry(
          repository.getId(), "feature", "Trainer Blue", Instant.now(clock)
        )
      );

      PullRequest pr = TestData.createPullRequest();
      pr.setSource("feature");
      PullRequestEvent event = new PullRequestEvent(repository, pr, null, HandlerEventType.CREATE);

      suggestionService.onPullRequestCreated(event);

      assertThat(suggestionService.getPushEntries()).isEmpty();
    }

    @Test
    void shouldNotRemovePushEntriesOfOtherRepository() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        "1337", "feature", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      PullRequest pr = TestData.createPullRequest();
      pr.setSource("feature");
      PullRequestEvent event = new PullRequestEvent(repository, pr, null, HandlerEventType.CREATE);

      suggestionService.onPullRequestCreated(event);

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(existingPushEntry)
      );
    }

    @Test
    void shouldNotRemovePushEntriesOfOtherBranchThanSourceBranch() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(), "bugfix", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      PullRequest pr = TestData.createPullRequest();
      pr.setSource("feature");
      PullRequestEvent event = new PullRequestEvent(repository, pr, null, HandlerEventType.CREATE);

      suggestionService.onPullRequestCreated(event);

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(existingPushEntry)
      );
    }

    @ParameterizedTest
    @EnumSource(value = HandlerEventType.class, names = {"MODIFY", "DELETE", "BEFORE_CREATE", "BEFORE_MODIFY", "BEFORE_DELETE"})
    void shouldOnlyHandlePullRequestCreatedEvent(HandlerEventType eventType) {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(), "feature", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      PullRequest pr = TestData.createPullRequest();
      pr.setSource("feature");
      PullRequestEvent event = new PullRequestEvent(repository, pr, null, eventType);

      suggestionService.onPullRequestCreated(event);

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(existingPushEntry)
      );
    }
  }

  @Nested
  class RemovePushEntry {

    @Test
    void shouldNotRemovePushEntriesOfOtherRepositories() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        "1337", "feature", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      suggestionService.removePushEntry(repository.getId(), "feature", "Trainer Red");

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(existingPushEntry)
      );
    }

    @Test
    void shouldNotRemovePushEntriesOfOtherBranches() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(), "bugfix", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      suggestionService.removePushEntry(repository.getId(), "feature", "Trainer Red");

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(existingPushEntry)
      );
    }

    @Test
    void shouldNotRemovePushEntriesOfOtherUsers() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(), "feature", "Trainer Blue", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      suggestionService.removePushEntry(repository.getId(), "feature", "Trainer Red");

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(existingPushEntry)
      );
    }

    @Test
    void shouldRemovePushEntriesOfRepositoryBranchAndUser() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(), "feature", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      suggestionService.removePushEntry(repository.getId(), "feature", "Trainer Red");

      assertThat(suggestionService.getPushEntries()).isEmpty();
    }
  }

  @Nested
  class GetPushEntries {

    @Test
    void shouldNotGetPushEntriesOfDifferentRepository() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        "1337", "feature", "Trainer Red", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      Collection<PullRequestSuggestionService.PushEntry> entries = suggestionService.getPushEntries(
        repository.getId(), "Trainer Red"
      );

      assertThat(entries).isEmpty();
    }

    @Test
    void shouldNotGetPushEntriesOfDifferentUser() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(), "feature", "Trainer Blue", Instant.now(clock)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      Collection<PullRequestSuggestionService.PushEntry> entries = suggestionService.getPushEntries(
        repository.getId(), "Trainer Red"
      );

      assertThat(entries).isEmpty();
    }

    @Test
    void shouldNotGetExpiredPushEntriesOfRepositoryAndUser() {
      PullRequestSuggestionService.PushEntry existingPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(),
        "feature",
        "Trainer Red",
        Instant.now(clock).minus(2, ChronoUnit.HOURS)
      );
      suggestionService.getPushEntries().add(existingPushEntry);

      Collection<PullRequestSuggestionService.PushEntry> entries = suggestionService.getPushEntries(
        repository.getId(), "Trainer Red"
      );

      assertThat(entries).isEmpty();
    }

    @Test
    void shouldGetPushEntriesOfRepositoryAndUser() {
      PullRequestSuggestionService.PushEntry pushEntryJustInTime = new PullRequestSuggestionService.PushEntry(
        repository.getId(),
        "feature",
        "Trainer Red",
        Instant.now(clock).minus(2, ChronoUnit.HOURS).plus(1, ChronoUnit.MILLIS)
      );
      suggestionService.getPushEntries().add(pushEntryJustInTime);

      PullRequestSuggestionService.PushEntry anotherPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(),
        "bugfix",
        "Trainer Red",
        Instant.now(clock)
      );
      suggestionService.getPushEntries().add(anotherPushEntry);

      Collection<PullRequestSuggestionService.PushEntry> entries = suggestionService.getPushEntries(
        repository.getId(), "Trainer Red"
      );

      assertThat(entries).usingRecursiveComparison().isEqualTo(
        List.of(pushEntryJustInTime, anotherPushEntry)
      );
    }

    @Test
    void shouldCleanUpAllExpiredPushEntries() {
      PullRequestSuggestionService.PushEntry expiredPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(),
        "feature",
        "Trainer Red",
        Instant.now(clock).minus(2, ChronoUnit.HOURS)
      );
      PullRequestSuggestionService.PushEntry expiredPushEntryDifferentRepo = new PullRequestSuggestionService.PushEntry(
        "1337",
        "feature",
        "Trainer Red",
        Instant.now(clock).minus(2, ChronoUnit.HOURS)
      );
      PullRequestSuggestionService.PushEntry expiredPushEntryDifferentUser = new PullRequestSuggestionService.PushEntry(
        repository.getId(),
        "feature",
        "Trainer Blue",
        Instant.now(clock).minus(2, ChronoUnit.HOURS)
      );
      PullRequestSuggestionService.PushEntry nonExpiredPushEntry = new PullRequestSuggestionService.PushEntry(
        repository.getId(),
        "feature",
        "Trainer Red",
        Instant.now(clock).minus(2, ChronoUnit.HOURS).plus(1, ChronoUnit.MILLIS)
      );


      suggestionService.getPushEntries().addAll(
        List.of(
          expiredPushEntry, expiredPushEntryDifferentRepo, expiredPushEntryDifferentUser, nonExpiredPushEntry
        )
      );

      suggestionService.getPushEntries(
        repository.getId(), "Trainer Red"
      );

      assertThat(suggestionService.getPushEntries()).usingRecursiveComparison().isEqualTo(
        List.of(nonExpiredPushEntry)
      );
    }
  }
}
