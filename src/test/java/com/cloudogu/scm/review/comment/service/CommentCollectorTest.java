package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentCollectorTest {

  @Mock
  private CommentService commentService;

  @InjectMocks
  private CommentCollector collector;

  @Test
  void shouldCollectNonOutdatedComments() {
    Repository repository = RepositoryTestData.createHeartOfGold();
    PullRequest pullRequest = TestData.createPullRequest();

    Comment one = Comment.createSystemComment("hello");
    Comment two = Comment.createSystemComment("hello");
    two.setOutdated(true);
    List<Comment> prcs = ImmutableList.of(one, two);
    when(commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId())).thenReturn(prcs);

    Stream<Comment> comments = collector.collectNonOutdated(repository, pullRequest);
    assertThat(comments).containsOnly(one);
  }
}
