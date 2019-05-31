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
