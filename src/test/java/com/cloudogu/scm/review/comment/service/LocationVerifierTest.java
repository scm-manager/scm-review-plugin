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

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.DiffFile;
import sonia.scm.repository.api.DiffLine;
import sonia.scm.repository.api.DiffResult;
import sonia.scm.repository.api.DiffResultCommandBuilder;
import sonia.scm.repository.api.Hunk;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;
import java.util.List;
import java.util.OptionalInt;

import static com.cloudogu.scm.review.comment.service.Comment.createComment;
import static java.util.Collections.emptyIterator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationVerifierTest {

  private static final String NAMESPACE = "space";
  private static final String NAME = "name";
  private static final Repository REPOSITORY = new Repository("repo_ID", "git", NAMESPACE, NAME);
  private static final PullRequest PULL_REQUEST = new PullRequest("42", "feature", "main");

  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;
  @InjectMocks
  private LocationVerifier locationVerifier;

  @Test
  void shouldNotVerifyCommentWithoutLocation() {
    Comment comment = createComment("2", "comment", null, null);
    locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY);

    verifyNoInteractions(repositoryServiceFactory);
  }

  @Nested
  class WithVerification {

    @BeforeEach
    void mockDiff() throws IOException {
      RepositoryService repositoryService = mock(RepositoryService.class);
      when(repositoryServiceFactory.create(REPOSITORY)).thenReturn(repositoryService);
      DiffResultCommandBuilder diffResultCommandBuilder = mock(DiffResultCommandBuilder.class, RETURNS_SELF);
      when(repositoryService.getDiffResultCommand()).thenReturn(diffResultCommandBuilder);
      DiffResult diffResult = mock(DiffResult.class);
      when(diffResultCommandBuilder.getDiffResult()).thenReturn(diffResult);
      DiffFile diffFile = mock(DiffFile.class);
      lenient().when(diffFile.getNewPath()).thenReturn("README.md");
      lenient().when(diffFile.getOldPath()).thenReturn("README");
      Hunk hunk1 = mock(Hunk.class);
      Hunk hunk2 = mock(Hunk.class);
      lenient().when(diffFile.iterator()).thenReturn(List.of(hunk1, hunk2).iterator());
      lenient().when(hunk1.getRawHeader()).thenReturn("@@ -1,5 +1,3 @@");
      lenient().when(hunk1.getNewStart()).thenReturn(1);
      lenient().when(hunk1.getNewLineCount()).thenReturn(3);
      lenient().when(hunk1.getOldStart()).thenReturn(1);
      lenient().when(hunk1.getOldLineCount()).thenReturn(5);
      lenient().when(hunk2.getRawHeader()).thenReturn("@@ -10,5 +10,3 @@");
      lenient().when(hunk2.getNewStart()).thenReturn(10);
      lenient().when(hunk2.getNewLineCount()).thenReturn(3);
      lenient().when(hunk2.getOldStart()).thenReturn(10);
      lenient().when(hunk2.getOldLineCount()).thenReturn(5);
      lenient().when(hunk1.iterator())
        .thenReturn(List.of(
          new MockDiffLine(1, 1),
          new MockDiffLine(2, null),
          new MockDiffLine(null, 2),
          new MockDiffLine(3, null),
          new MockDiffLine(4, null),
          (DiffLine) new MockDiffLine(5, 3)
        ).iterator());
      lenient().when(hunk2.iterator())
        .thenReturn(emptyIterator());
      DiffFile deletedFile = mock(DiffFile.class);
      lenient().when(deletedFile.getNewPath()).thenReturn("/dev/null");
      lenient().when(deletedFile.getOldPath()).thenReturn("deleted");
      Hunk deletedHunk = mock(Hunk.class);
      lenient().when(deletedFile.iterator()).thenReturn(List.of(deletedHunk).iterator());
      lenient().when(deletedHunk.getRawHeader()).thenReturn("@@ -1,2 +0,0 @@");
      lenient().when(deletedHunk.getNewStart()).thenReturn(0);
      lenient().when(deletedHunk.getNewLineCount()).thenReturn(0);
      lenient().when(deletedHunk.getOldStart()).thenReturn(1);
      lenient().when(deletedHunk.getOldLineCount()).thenReturn(2);
      lenient().when(deletedHunk.iterator())
        .thenReturn(List.of(
          new MockDiffLine(1, null),
          (DiffLine) new MockDiffLine(2, null)
        ).iterator());
      when(diffResult.iterator()).thenReturn(List.of(diffFile, deletedFile).iterator());
    }

    @Test
    void shouldFindHunkForCommentWithFirstOldLine() {
      Comment comment = createComment("2", "comment", null, new Location("README.md", null, 1, 1));
      locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY);

      assertThat(comment.getLocation().getHunk()).isEqualTo("@@ -1,5 +1,3 @@");
    }

    @Test
    void shouldFailWithIncorrectHunk() {
      Comment comment = createComment("2", "comment", null, new Location("README.md", "@@ -1,5 +1,4 @@", 1, 1));
      assertThrows(
        LocationForCommentNotFoundException.class,
        () -> locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY)
      );
    }

    @Test
    void shouldFailWithIncorrectFile() {
      Comment comment = createComment("2", "comment", null, new Location("READNIX", null, 1, 1));
      assertThrows(
        LocationForCommentNotFoundException.class,
        () -> locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY)
      );
    }

    @Test
    void shouldFindHunkForCommentWithLastOldLine() {
      Comment comment = createComment("2", "comment", null, new Location("README.md", null, 5, null));
      locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY);

      assertThat(comment.getLocation().getHunk()).isEqualTo("@@ -1,5 +1,3 @@");
    }

    @Test
    void shouldFindHunkForCommentWithFirstNewLine() {
      Comment comment = createComment("2", "comment", null, new Location("README.md", null, null, 1));
      locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY);

      assertThat(comment.getLocation().getHunk()).isEqualTo("@@ -1,5 +1,3 @@");
    }

    @Test
    void shouldFindHunkForCommentWithLastNewLine() {
      Comment comment = createComment("2", "comment", null, new Location("README.md", null, 5, 3));
      locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY);

      assertThat(comment.getLocation().getHunk()).isEqualTo("@@ -1,5 +1,3 @@");
    }

    @Test
    void shouldFixOldLineForComment() {
      Comment comment = createComment("2", "comment", null, new Location("README.md", null, null, 3));
      locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY);

      assertThat(comment.getLocation().getOldLineNumber()).isEqualTo(5);
    }

    @Test
    void shouldFixNewLineForComment() {
      Comment comment = createComment("2", "comment", null, new Location("README.md", null, 5, null));
      locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY);

      assertThat(comment.getLocation().getNewLineNumber()).isEqualTo(3);
    }

    @Test
    void shouldFailWithInconsistentOldLineForComment() {
      Comment comment = createComment("2", "comment", null, new Location("README.md", null, 8, 3));
      assertThrows(
        LocationForCommentNotFoundException.class,
        () -> locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY)
      );
    }

    @Test
    void shouldFailWithInconsistentNewLineForComment() {
      Comment comment = createComment("2", "comment", null, new Location("README.md", null, 1, 2));
      assertThrows(
        LocationForCommentNotFoundException.class,
        () -> locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY)
      );
    }

    @Test
    void shouldFindHunkForCommentInSecondHunk() {
      Comment comment = createComment("12", "comment", null, new Location("README.md", null, 14, 12));
      locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY);

      assertThat(comment.getLocation().getHunk()).isEqualTo("@@ -10,5 +10,3 @@");
    }

    @Test
    void shouldFindHunkForCommentInDeletedTile() {
      Comment comment = createComment("12", "comment", null, new Location("deleted", null, 2, null));
      locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY);

      assertThat(comment.getLocation().getHunk()).isEqualTo("@@ -1,2 +0,0 @@");
    }

    @Test
    void shouldFailToFindHunkForCommentWithWrongNewFile() {
      Comment comment = createComment("2", "comment", null, new Location("README.txt", null, null, 1));
      assertThrows(
        LocationForCommentNotFoundException.class,
        () -> locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY)
      );
    }

    @Test
    void shouldFailToFindHunkForCommentWithWrongOldFile() {
      Comment comment = createComment("2", "comment", null, new Location("README.txt", null, 1, null));
      assertThrows(
        LocationForCommentNotFoundException.class,
        () -> locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY)
      );
    }

    @Test
    void shouldFailToFindHunkForCommentWithWrongNewLineNumber() {
      Comment comment = createComment("2", "comment", null, new Location("README.md", null, null, 4));
      assertThrows(
        LocationForCommentNotFoundException.class,
        () -> locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY)
      );
    }

    @Test
    void shouldFailToFindHunkForCommentWithWrongOldLineNumber() {
      Comment comment = createComment("2", "comment", null, new Location("README.txt", null, 6, null));
      assertThrows(
        LocationForCommentNotFoundException.class,
        () -> locationVerifier.verifyLocation(comment, PULL_REQUEST, REPOSITORY)
      );
    }

    private static class MockDiffLine implements DiffLine {

      private final Integer oldLineNumber;
      private final Integer newLineNumber;

      private MockDiffLine(Integer oldLineNumber, Integer newLineNumber) {
        this.oldLineNumber = oldLineNumber;
        this.newLineNumber = newLineNumber;
      }

      @Override
      public OptionalInt getOldLineNumber() {
        return oldLineNumber == null ? OptionalInt.empty() : OptionalInt.of(oldLineNumber);
      }

      @Override
      public OptionalInt getNewLineNumber() {
        return newLineNumber == null ? OptionalInt.empty() : OptionalInt.of(newLineNumber);
      }

      @Override
      public String getContent() {
        return "line";
      }
    }
  }
}
