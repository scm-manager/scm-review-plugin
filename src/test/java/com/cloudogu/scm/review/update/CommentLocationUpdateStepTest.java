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
package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.Location;
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
class CommentLocationUpdateStepTest {

  @Mock
  private RepositoryLocationResolver resolver;

  @Mock
  private RepositoryLocationResolver.RepositoryLocationResolverInstance<Path> resolverInstance;

  private CommentLocationUpdateStep updateStep;

  @BeforeEach
  void setupUpdateStep(){
    when(resolver.forClass(Path.class)).thenReturn(resolverInstance);
    updateStep = new CommentLocationUpdateStep(resolver);
  }

  @Test
  void shouldUpdateLocation(@TempDirectory.TempDir Path temp) throws Exception {
    Path store = createStore(temp);

    doAnswer(ic -> {
      BiConsumer<String, Path> consumer = ic.getArgument(0);
      consumer.accept("repo", temp);
      return null;
    }).when(resolverInstance).forAllLocations(any());

    Path one = copyResource(store, "1");
    Path two = copyResource(store, "2");

    updateStep.doUpdate();

    PullRequestComments commentsOfOne = JAXB.unmarshal(one.toFile(), PullRequestComments.class);
    assertThat(commentsOfOne.getComments())
      .hasSize(4)
      .extracting(Comment::getLocation)
      .containsOnly(
        null,
        null,
        new Location("README.md", "@@ -1,3 +1,41 @@", null, 25),
        new Location("README.md", "@@ -1,3 +1,41 @@", null, 39)
      );

    PullRequestComments commentsOfTwo = JAXB.unmarshal(two.toFile(), PullRequestComments.class);
    assertThat(commentsOfTwo.getComments())
      .hasSize(3)
      .extracting(Comment::getLocation)
      .containsOnly(
        new Location("README.md", "@@ -1,3 +1,41 @@", null, 39),
        new Location("README.md", "@@ -1,3 +1,41 @@", 39, null),
        new Location("README.md", "@@ -1,3 +1,41 @@", 39, 39)
      );
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
