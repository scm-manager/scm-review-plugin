/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.comment.service.CommentIndexer.ReindexRepository;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.search.Id;
import sonia.scm.search.Index;
import sonia.scm.search.IndexLog;
import sonia.scm.search.IndexLogStore;
import sonia.scm.search.SearchEngine;
import sonia.scm.search.SerializableIndexTask;

import javax.servlet.ServletContextEvent;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"UnstableApiUsage", "unchecked"})
class CommentIndexerTest {

  private final Repository repository = new Repository("1", "git", "hitchhiker", "42");

  @Mock
  private SearchEngine searchEngine;
  @Mock
  private SearchEngine.ForType<IndexedComment> forType;

  @InjectMocks
  private CommentIndexer indexer;

  @BeforeEach
  void mockSearchEngine() {
    lenient().when(searchEngine.forType(IndexedComment.class)).thenReturn(forType);
  }

  @Test
  void shouldIndexCommentForCommentEvent() {
    PullRequest pr = createPullRequest();
    Comment comment = Comment.createComment("1", "first one", "trillian", new Location());
    indexer.handleEvent(new CommentEvent(repository, pr, comment, comment, HandlerEventType.CREATE));

    verify(forType, times(1)).update(any(SerializableIndexTask.class));
  }

  @Test
  void shouldDeleteCommentFromIndex() {
    PullRequest pr = createPullRequest();
    Comment comment = Comment.createComment("1", "first one", "trillian", new Location());
    indexer.handleEvent(new CommentEvent(repository, pr, null, comment, HandlerEventType.DELETE));

    verify(forType, times(1)).update(any(SerializableIndexTask.class));
  }

  @Test
  void shouldIndexCommentForReplyEvent() {
    PullRequest pr = createPullRequest();
    Comment comment = Comment.createComment("1", "first one", "trillian", new Location());
    Reply reply = Reply.createReply("1", "first reply", "trillian");
    indexer.handleEvent(new ReplyEvent(repository, pr, reply, reply, comment, HandlerEventType.CREATE));

    verify(forType, times(1)).update(any(SerializableIndexTask.class));
  }

  @Test
  void shouldDeleteReplyFromIndexForReplyEvent() {
    PullRequest pr = createPullRequest();
    Comment comment = Comment.createComment("1", "first one", "trillian", new Location());
    Reply reply = Reply.createReply("1", "first one", "trillian");
    indexer.handleEvent(new ReplyEvent(repository, pr, null, reply, comment, HandlerEventType.DELETE));

    verify(forType, times(1)).update(any(SerializableIndexTask.class));
  }

  @Test
  void shouldReindexAllPullRequestOnStartup() {
    ServletContextEvent event = mock(ServletContextEvent.class);
    indexer.contextInitialized(event);

    verify(forType).update(CommentIndexer.ReindexAll.class);
  }

  @Nested
  class ReindexAllTests {
    @Mock
    private RepositoryManager repositoryManager;
    @Mock
    private PullRequestService pullRequestService;
    @Mock
    private CommentService commentService;
    @Mock
    private IndexLogStore indexLogStore;
    @Mock
    private IndexLogStore.ForIndex forIndex;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Index<IndexedComment> index;

    @InjectMocks
    private CommentIndexer.ReindexAll reindexAll;

    @BeforeEach
    void mockLogStore() {
      when(indexLogStore.defaultIndex()).thenReturn(forIndex);
    }

    @Test
    void shouldNotReindexAllIfVersionHasNotChanged() {
      when(forIndex.get(IndexedComment.class)).thenReturn(Optional.of(new IndexLog(IndexedComment.VERSION)));

      reindexAll.update(index);

      verify(index, never()).delete();
    }

    @Test
    void shouldNotReindexRepositoryIfDoesNotSupportPullRequests() {
      when(pullRequestService.supportsPullRequests(repository)).thenReturn(false);
      when(forIndex.get(IndexedComment.class)).thenReturn(Optional.of(new IndexLog(42)));

      when(repositoryManager.getAll()).thenReturn(ImmutableList.of(repository));

      reindexAll.update(index);

      verify(index.delete()).all();
      verify(index, never()).store(
        any(Id.class),
        anyString(),
        any(IndexedComment.class)
      );
    }

    @Test
    void shouldReindexAllIfLogStoreIsEmpty() {
      when(forIndex.get(IndexedComment.class)).thenReturn(Optional.empty());

      reindexAll.update(index);

      verify(index.delete()).all();
    }

    @Test
    void shouldReindexAllIfLogStoreVersionDiffers() {
      when(pullRequestService.supportsPullRequests(repository)).thenReturn(true);
      when(forIndex.get(IndexedComment.class)).thenReturn(Optional.of(new IndexLog(42)));

      PullRequest pullRequest = createPullRequest();
      Comment comment = Comment.createComment("1", "first one", "trillian", new Location());
      when(repositoryManager.getAll()).thenReturn(ImmutableList.of(repository));
      when(pullRequestService.getAll(repository.getNamespace(), repository.getName())).thenReturn(ImmutableList.of(pullRequest));
      when(commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId())).thenReturn(ImmutableList.of(comment));

      reindexAll.update(index);

      verify(index.delete()).all();
      verify(index).store(
        eq(Id.of(IndexedComment.class, comment.getId()).and(PullRequest.class, pullRequest.getId()).and(Repository.class, repository.getId())),
        eq("repository:readPullRequest:" + pullRequest.getId()),
        argThat(indexedComment -> {
          assertThat(indexedComment.getId()).isEqualTo(comment.getId());
          assertThat(indexedComment.getComment()).isEqualTo(comment.getComment());
          return true;
        })
      );
    }
  }

  @Nested
  class ReindexRepositoryTests {

    @Mock
    private PullRequestService pullRequestService;
    @Mock
    private CommentService commentService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Index<IndexedComment> index;

    @Test
    void shouldReindexRepository() {
      ReindexRepository reindexRepository = new ReindexRepository(repository);
      reindexRepository.setCommentService(commentService);
      reindexRepository.setPullRequestService(pullRequestService);

      when(pullRequestService.supportsPullRequests(repository)).thenReturn(true);

      PullRequest pullRequest = createPullRequest();
      when(pullRequestService.getAll(repository.getNamespace(), repository.getName())).thenReturn(ImmutableList.of(pullRequest));
      Comment comment = Comment.createComment("1", "first one", "trillian", new Location());
      when(commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId())).thenReturn(ImmutableList.of(comment));

      reindexRepository.update(index);

      verify(index.delete()).by(repository);

      verify(index).store(
        eq(Id.of(IndexedComment.class, comment.getId()).and(PullRequest.class, pullRequest.getId()).and(Repository.class, repository.getId())),
        eq("repository:readPullRequest:" + pullRequest.getId()),
        argThat(indexedComment -> {
          assertThat(indexedComment.getId()).isEqualTo(comment.getId());
          assertThat(indexedComment.getComment()).isEqualTo(comment.getComment());
          return true;
        })
      );
    }
  }

  private PullRequest createPullRequest() {
    return new PullRequest("1", "source", "target");
  }
}
