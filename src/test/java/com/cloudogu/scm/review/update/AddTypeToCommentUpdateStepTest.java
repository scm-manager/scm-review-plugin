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

package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.comment.service.CommentType;
import com.google.common.io.Resources;
import jakarta.xml.bind.JAXB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.RepositoryLocationResolver;

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

@ExtendWith(MockitoExtension.class)
class AddTypeToCommentUpdateStepTest {

  @Mock
  private RepositoryLocationResolver.RepositoryLocationResolverInstance<Path> resolverInstance;

  private AddTypeToCommentUpdateStep updateStep;

  @Mock
  private RepositoryLocationResolver resolver;
  private Path store;

  @BeforeEach
  void init(@TempDir Path temp) throws IOException {
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

    LegacyXmlPullRequestComments commentsOfOne = JAXB.unmarshal(one.toFile(), LegacyXmlPullRequestComments.class);
    assertThat(commentsOfOne.getComments())
      .hasSize(4)
      .extracting(LegacyXmlComment::getType)
      .containsExactly(CommentType.COMMENT, CommentType.COMMENT, CommentType.COMMENT, CommentType.COMMENT);
  }

  @Test
  void shouldNotAddTypeToCommentTextContentNode() throws IOException {
    Path one = copyResource(store, "1");

    updateStep.doUpdate();

    LegacyXmlPullRequestComments commentsOfOne = JAXB.unmarshal(one.toFile(), LegacyXmlPullRequestComments.class);
    assertThat(commentsOfOne.getComments().get(0).getComment()).isEqualTo("Don't panic!");
  }

  @Test
  void shouldNotOverwriteExistingTypeInPullRequestComments() throws IOException {
    Path two = copyResource(store, "2");

    updateStep.doUpdate();

    LegacyXmlPullRequestComments commentsOfTwo = JAXB.unmarshal(two.toFile(), LegacyXmlPullRequestComments.class);
    assertThat(commentsOfTwo.getComments())
      .hasSize(3)
      .extracting(LegacyXmlComment::getType)
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

