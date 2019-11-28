package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.PullRequestComments;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.RepositoryLocationResolver;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, TempDirectory.class})
class AddTypeToCommentUpdateStepTest {

  @Mock
  private RepositoryLocationResolver.RepositoryLocationResolverInstance<Path> resolverInstance;

  private AddTypeToCommentUpdateStep updateStep;

  @Mock
  private RepositoryLocationResolver resolver;
  private Path store;

  @BeforeEach
  void init(@TempDirectory.TempDir Path temp) throws IOException {
    when(resolver.forClass(Path.class)).thenReturn(resolverInstance);
    updateStep = new AddTypeToCommentUpdateStep(resolver);

    store = createStore(temp);

    doAnswer(ic -> {
      BiConsumer<String, Path> consumer = ic.getArgument(0);
      consumer.accept("repo", temp);
      return null;
    }).when(resolverInstance).forAllLocations(any());
  }

  @Test
  void shouldAddTypeToPullRequestComments() throws IOException {
    Path one = copyResource(store, "1");

    updateStep.doUpdate();

    PullRequestComments commentsOfOne = JAXB.unmarshal(one.toFile(), PullRequestComments.class);
    assertThat(commentsOfOne.getComments())
      .hasSize(4)
      .extracting(Comment::getType)
      .containsExactly(CommentType.COMMENT, CommentType.COMMENT, CommentType.COMMENT, CommentType.COMMENT);
  }

  @Test
  void shouldNotOverwriteExistingTypeInPullRequestComments() throws IOException {
    Path two = copyResource(store, "2");

    updateStep.doUpdate();

    PullRequestComments commentsOfTwo = JAXB.unmarshal(two.toFile(), PullRequestComments.class);
    assertThat(commentsOfTwo.getComments())
      .hasSize(3)
      .extracting(Comment::getType)
      .containsExactly(CommentType.COMMENT, CommentType.TASK_TODO, CommentType.TASK_DONE);
  }

  private Path createStore(Path temp) throws IOException {
    Path store = temp.resolve("store").resolve("data").resolve("pullRequestComment");
    Files.createDirectories(store);
    return store;
  }

  private Path copyResource(Path store, String id) throws IOException {
    String source = String.format("com/cloudogu/scm/review/update/comment-location-%s.xml", id);
    URL resource = Resources.getResource(source);
    Path target = store.resolve(id + ".xml");
    try (OutputStream outputStream = Files.newOutputStream(target)) {
      Resources.copy(resource, outputStream);
    }
    return target;
  }
}

