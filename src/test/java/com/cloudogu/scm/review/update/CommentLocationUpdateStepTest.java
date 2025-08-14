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

import com.cloudogu.scm.review.comment.service.Location;
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
class CommentLocationUpdateStepTest {

  @Mock
  private RepositoryLocationResolver resolver;

  @Mock
  private RepositoryLocationResolver.RepositoryLocationResolverInstance<Path> resolverInstance;

  private CommentLocationUpdateStep updateStep;

  @BeforeEach
  void setupUpdateStep() {
    when(resolver.forClass(Path.class)).thenReturn(resolverInstance);
    updateStep = new CommentLocationUpdateStep(resolver);
  }

  @Test
  void shouldUpdateLocation(@TempDir Path temp) throws Exception {
    Path store = createStore(temp);

    doAnswer(ic -> {
      BiConsumer<String, Path> consumer = ic.getArgument(0);
      consumer.accept("repo", temp);
      return null;
    }).when(resolverInstance).forAllLocations(any());

    Path one = copyResource(store, "1");
    Path two = copyResource(store, "2");

    updateStep.doUpdate();

    LegacyXmlPullRequestComments commentsOfOne = JAXB.unmarshal(one.toFile(), LegacyXmlPullRequestComments.class);
    assertThat(commentsOfOne.getComments())
      .hasSize(4)
      .extracting(LegacyXmlComment::getLocation)
      .containsOnly(
        null,
        null,
        new Location("README.md", "@@ -1,3 +1,41 @@", null, 25),
        new Location("README.md", "@@ -1,3 +1,41 @@", null, 39)
      );

    LegacyXmlPullRequestComments commentsOfTwo = JAXB.unmarshal(two.toFile(), LegacyXmlPullRequestComments.class);
    assertThat(commentsOfTwo.getComments())
      .hasSize(3)
      .extracting(LegacyXmlComment::getLocation)
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
