package com.cloudogu.scm.review.comment.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
class CommentInitializerTest {

  static final String PRINCIPAL = "dent";

  private static final Repository REPOSITORY = createHeartOfGold();

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
    void shouldSetCurrentAuthor() throws IOException {
      Comment comment = new Comment();

      initializer.initialize(comment, REPOSITORY.getId());

      assertThat(comment.getAuthor()).isEqualTo(PRINCIPAL);
    }

    @Test
    void shouldSetCurrentTime() throws IOException {
      Instant now = Instant.now();
      when(clock.instant()).thenReturn(now);
      Comment comment = new Comment();

      initializer.initialize(comment, REPOSITORY.getId());

      assertThat(comment.getDate()).isEqualTo(now);
    }
  }

  @Nested
  class withAddedLinesOnly {

    @BeforeEach
    void initRepositoryService() {
      when(repositoryServiceFactory.create(REPOSITORY.getId())).thenReturn(repositoryService);
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
    void shouldSetContextForInlineCommentWithCommentInTheMiddle() throws IOException {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", null, 6));

      initializer.initialize(comment, REPOSITORY.getId());

      assertThat(comment.getContext()).isNotNull();
      assertThat(comment.getContext().getChanges())
        .hasSize(7)
        .extracting(DiffLine::getNewLineNumber)
        .extracting(OptionalInt::getAsInt)
        .contains(3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    void shouldSetContextForInlineCommentWithCommentAtTheEnd() throws IOException {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", null, 10));

      initializer.initialize(comment, REPOSITORY.getId());

      assertThat(comment.getContext()).isNotNull();
      assertThat(comment.getContext().getChanges())
        .hasSize(7)
        .extracting(DiffLine::getNewLineNumber)
        .extracting(OptionalInt::getAsInt)
        .contains(4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void shouldSetContextForInlineCommentWithCommentAtTheBeginning() throws IOException {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", null, 2));

      initializer.initialize(comment, REPOSITORY.getId());

      assertThat(comment.getContext()).isNotNull();
      assertThat(comment.getContext().getChanges())
        .hasSize(7)
        .extracting(DiffLine::getNewLineNumber)
        .extracting(OptionalInt::getAsInt)
        .contains(2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    void shouldThrowNotFoundForNotExistingFileName() {
      Comment comment = new Comment();
      comment.setLocation(new Location("notExistingPath", "irrelevant", null, 2));

      Assertions.assertThrows(NotFoundException.class, () -> initializer.initialize(comment, REPOSITORY.getId()));
    }

    @Test
    void shouldThrowNotFoundForNotExistingLineNumber() {
      Comment comment = new Comment();
      comment.setLocation(new Location("newPath", "irrelevant", null, 42));

      Assertions.assertThrows(NotFoundException.class, () -> initializer.initialize(comment, REPOSITORY.getId()));
    }
  }
  
}
