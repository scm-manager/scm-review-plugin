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

package com.cloudogu.scm.review.comment.api;

import com.google.inject.Provider;
import com.google.inject.util.Providers;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sonia.scm.api.v2.resources.ScmPathInfoStore;

import static java.net.URI.create;

class CommentPathBuilderTest {

  private Provider<ScmPathInfoStore> pathInfoStore;
  private CommentPathBuilder commentPathBuilder;

  @BeforeEach
  void init() {
    ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    scmPathInfoStore.set(() ->
      create("/api/"));
    pathInfoStore = Providers.of(scmPathInfoStore);
    commentPathBuilder = new CommentPathBuilder(pathInfoStore);
  }

  @Test
  void shouldCreateSelfLink() {
    String commentSelfUri = commentPathBuilder.createCommentSelfUri("space", "name", "prId", "commentId");
    Assertions.assertThat(commentSelfUri).isEqualTo("/api/v2/pull-requests/space/name/prId/comments/commentId");
  }
}
