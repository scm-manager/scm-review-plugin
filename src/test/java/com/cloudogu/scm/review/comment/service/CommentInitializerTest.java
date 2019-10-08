package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.NotFoundException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.DiffFile;
import sonia.scm.repository.api.DiffLine;
import sonia.scm.repository.api.DiffResult;
import sonia.scm.repository.api.DiffResultCommandBuilder;
import sonia.scm.repository.api.Hunk;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.OptionalInt;

import static com.cloudogu.scm.review.TestData.createPullRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
class CommentInitializerTest {

  static final String PRINCIPAL = "dent";

  private static final Repository REPOSITORY = createHeartOfGold();
  private static final PullRequest PULL_REQUEST = createPullRequest();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  Subject subject;

  @Mock
  Clock clock;

  @Mock
  RepositoryServiceFactory repositoryServiceFactory;
  @Mock
  RepositoryService repositoryService;
  @Mock(answer = Answers.RETURNS_SELF)
  DiffResultCommandBuilder diffResultCommandBuilder;

  @InjectMocks
  CommentInitializer initializer;

  @BeforeEach
  void initSubject() {
    ThreadContext.bind(subject);
    when(subject.getPrincipals().getPrimaryPrincipal()).thenReturn(PRINCIPAL);
  }

  @Nested
  class withoutDiffResult {

    @Test
    void shouldSetCurrentAuthor() {
      Comment comment = new Comment();

      initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

      assertThat(comment.getAuthor()).isEqualTo(PRINCIPAL);
    }

    @Test
    void shouldSetCurrentTime() {
      Instant now = Instant.now();
      when(clock.instant()).thenReturn(now);
      Comment comment = new Comment();

      initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

      assertThat(comment.getDate()).isEqualTo(now);
    }
  }

  @Nested
  class withAddedLinesOnly {

    @BeforeEach
    void initRepositoryService() {
      when(repositoryServiceFactory.create(REPOSITORY.getId())).thenReturn(repositoryService);
      when(repositoryService.isSupported(Command.DIFF_RESULT)).thenReturn(true);
    }

    @BeforeEach
    void mockDiffResult() throws IOException {
      when(repositoryService.getDiffResultCommand()).thenReturn(diffResultCommandBuilder);
      Hunk hunk = new MockedHunk.Builder()
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(2).get())
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(3).get())
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(4).get())
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(5).get())
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(6).get())
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(7).get())
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(8).get())
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(9).get())
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(10).get())
        .get();
      DiffFile diffFile = new MockedDiffFile.Builder().oldPath("newPath").newPath("newPath").addHunk(hunk).get();
      DiffResult diffResult = new MockedDiffResult.Builder().diffFile(diffFile).get();
      when(diffResultCommandBuilder.getDiffResult()).thenReturn(diffResult);
    }

    @Test
    void shouldSetContextForInlineCommentWithCommentInTheMiddle() {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", null, 6));

      initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

      assertThat(comment.getContext()).isNotNull();
      assertThat(comment.getContext().getLines())
        .hasSize(7)
        .extracting(DiffLine::getNewLineNumber)
        .extracting(OptionalInt::getAsInt)
        .contains(3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    void shouldSetContextForInlineCommentWithCommentAtTheEnd() {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", null, 10));

      initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

      assertThat(comment.getContext()).isNotNull();
      assertThat(comment.getContext().getLines())
        .hasSize(7)
        .extracting(DiffLine::getNewLineNumber)
        .extracting(OptionalInt::getAsInt)
        .contains(4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void shouldSetContextForInlineCommentWithCommentAtTheBeginning() {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", null, 2));

      initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

      assertThat(comment.getContext()).isNotNull();
      assertThat(comment.getContext().getLines())
        .hasSize(7)
        .extracting(DiffLine::getNewLineNumber)
        .extracting(OptionalInt::getAsInt)
        .contains(2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    void shouldThrowNotFoundForNotExistingFileName() {
      Comment comment = new Comment();
      comment.setLocation(new Location("notExistingPath", "irrelevant", null, 2));

      Assertions.assertThrows(NotFoundException.class, () -> initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId()));
    }

    @Test
    void shouldThrowNotFoundForNotExistingLineNumber() {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", null, 42));

      Assertions.assertThrows(NotFoundException.class, () -> initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId()));
    }

    @Test
    void shouldCallDiffResultWithSourceAndTarget() throws IOException {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", null, 2));

      initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

      verify(diffResultCommandBuilder).setRevision(PULL_REQUEST.getSource());
      verify(diffResultCommandBuilder).setAncestorChangeset(PULL_REQUEST.getTarget());
    }
  }

  @Nested
  class withDeletedLinesOnly {

    @BeforeEach
    void initRepositoryService() {
      when(repositoryServiceFactory.create(REPOSITORY.getId())).thenReturn(repositoryService);
      when(repositoryService.isSupported(Command.DIFF_RESULT)).thenReturn(true);
    }

    @BeforeEach
    void mockDiffResult() throws IOException {
      when(repositoryService.getDiffResultCommand()).thenReturn(diffResultCommandBuilder);
      Hunk hunk = new MockedHunk.Builder()
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(2).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(3).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(4).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(5).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(6).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(7).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(8).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(9).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(10).get())
        .get();
      DiffFile diffFile = new MockedDiffFile.Builder().oldPath("newPath").newPath("newPath").addHunk(hunk).get();
      DiffResult diffResult = new MockedDiffResult.Builder().diffFile(diffFile).get();
      when(diffResultCommandBuilder.getDiffResult()).thenReturn(diffResult);
    }

    @Test
    void shouldSetContextForInlineCommentWithCommentInTheMiddle() {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", 6, null));

      initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

      assertThat(comment.getContext()).isNotNull();
      assertThat(comment.getContext().getLines())
        .hasSize(7)
        .extracting(DiffLine::getOldLineNumber)
        .extracting(OptionalInt::getAsInt)
        .contains(3, 4, 5, 6, 7, 8, 9);
    }
  }

  @Nested
  class withSingleChangedLine {

    @BeforeEach
    void initRepositoryService() {
      when(repositoryServiceFactory.create(REPOSITORY.getId())).thenReturn(repositoryService);
      when(repositoryService.isSupported(Command.DIFF_RESULT)).thenReturn(true);
    }

    @BeforeEach
    void mockDiffResult() throws IOException {
      when(repositoryService.getDiffResultCommand()).thenReturn(diffResultCommandBuilder);
      Hunk hunk = new MockedHunk.Builder()
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(2).newLineNumber(4).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(3).newLineNumber(5).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(4).newLineNumber(6).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(5).get())
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(7).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(6).newLineNumber(8).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(7).newLineNumber(9).get())
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(8).newLineNumber(10).get())
        .get();
      DiffFile diffFile = new MockedDiffFile.Builder().oldPath("newPath").newPath("newPath").addHunk(hunk).get();
      DiffResult diffResult = new MockedDiffResult.Builder().diffFile(diffFile).get();
      when(diffResultCommandBuilder.getDiffResult()).thenReturn(diffResult);
    }

    @Test
    void shouldSetContextForInlineCommentWithCommentInTheMiddle() {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", 5, null));

      initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

      assertThat(comment.getContext()).isNotNull();
      assertThat(comment.getContext().getLines())
        .hasSize(7)
        .extracting(DiffLine::getOldLineNumber)
        .extracting(n -> n.orElse(-1))
        .contains(2, 3, 4, 5, -1, 6, 7);
      assertThat(comment.getContext().getLines())
        .hasSize(7)
        .extracting(DiffLine::getNewLineNumber)
        .extracting(n -> n.orElse(-1))
        .contains(4, 5, 6, -1, 7, 8, 9);
    }
  }

  @Nested
  class withNotEnoughLines {

    @BeforeEach
    void initRepositoryService() {
      when(repositoryServiceFactory.create(REPOSITORY.getId())).thenReturn(repositoryService);
      when(repositoryService.isSupported(Command.DIFF_RESULT)).thenReturn(true);
    }

    @BeforeEach
    void mockDiffResult() throws IOException {
      when(repositoryService.getDiffResultCommand()).thenReturn(diffResultCommandBuilder);
      Hunk hunk = new MockedHunk.Builder()
        .addDiffLine(new MockedDiffLine.Builder().newLineNumber(1).get())
        .get();
      DiffFile diffFile = new MockedDiffFile.Builder().oldPath("newPath").newPath("newPath").addHunk(hunk).get();
      DiffResult diffResult = new MockedDiffResult.Builder().diffFile(diffFile).get();
      when(diffResultCommandBuilder.getDiffResult()).thenReturn(diffResult);
    }

    @Test
    void shouldSetContextForInlineCommentWithCommentInTheMiddle() {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", null, 1));

      initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

      assertThat(comment.getContext()).isNotNull();
      assertThat(comment.getContext().getLines())
        .extracting(DiffLine::getNewLineNumber)
        .extracting(OptionalInt::getAsInt)
        .containsExactly(1);
    }
  }

  @Test
  void shouldNotCallUnsupportedCommand() {
    when(repositoryServiceFactory.create(REPOSITORY.getId())).thenReturn(repositoryService);
    when(repositoryService.isSupported(Command.DIFF_RESULT)).thenReturn(false);

    Comment comment = new Comment();
    comment.setLocation(new Location("newPath", "irrelevant", null, 1));

    initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

    verify(repositoryService, never()).getDiffResultCommand();
  }

  @Nested
  class withDeletedLines {

    @BeforeEach
    void initRepositoryService() {
      when(repositoryServiceFactory.create(REPOSITORY.getId())).thenReturn(repositoryService);
      when(repositoryService.isSupported(Command.DIFF_RESULT)).thenReturn(true);
    }

    @BeforeEach
    void mockDiffResult() throws IOException {
      when(repositoryService.getDiffResultCommand()).thenReturn(diffResultCommandBuilder);
      Hunk hunk = new MockedHunk.Builder()
        .addDiffLine(new MockedDiffLine.Builder().oldLineNumber(1).newLineNumber(0).get())
        .get();
      DiffFile diffFile = new MockedDiffFile.Builder().oldPath("oldPath").newPath("newPath").addHunk(hunk).get();
      DiffResult diffResult = new MockedDiffResult.Builder().diffFile(diffFile).get();
      when(diffResultCommandBuilder.getDiffResult()).thenReturn(diffResult);
    }

    @Test
    void shouldFindPathForInlineCommentWithOldContext() {
      Comment comment = new Comment();
      comment.setLocation(new Location("oldPath", "hunter", 1, null));

      initializer.initialize(comment, PULL_REQUEST, REPOSITORY.getId());

      assertThat(comment.getContext()).isNotNull();
      assertThat(comment.getContext().getLines())
        .hasSize(1)
        .extracting(DiffLine::getNewLineNumber)
        .extracting(OptionalInt::getAsInt)
        .contains(0);
      assertThat(comment.getContext().getLines())
        .hasSize(1)
        .extracting(DiffLine::getOldLineNumber)
        .extracting(OptionalInt::getAsInt)
        .contains(1);
    }
  }
}
