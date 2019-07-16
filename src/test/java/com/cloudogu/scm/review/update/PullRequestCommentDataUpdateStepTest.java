package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.PullRequestComments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.xml.XmlRepositoryDAO;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;
import sonia.scm.store.InMemoryDataStore;
import sonia.scm.store.InMemoryDataStoreFactory;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.RepositoryTestData.createHeartOfGold;

@ExtendWith(MockitoExtension.class)
class PullRequestCommentDataUpdateStepTest {

  @Mock
  XmlRepositoryDAO repositoryDAO;

  private PullRequestCommentDataUpdateStep updateStep;
  private DataStoreFactory dataStoreFactory = new InMemoryDataStoreFactory(new InMemoryDataStore());
  private DataStore<PullRequestComments> dataStore;
  private Repository repository = createHeartOfGold();

  private final static String STORE_NAME = "pullRequestComment";

  @BeforeEach
  void init() {
    dataStore = dataStoreFactory.withType(PullRequestComments.class).withName(STORE_NAME).forRepository(repository).build();
    updateStep = new PullRequestCommentDataUpdateStep(repositoryDAO, dataStoreFactory);
  }

  @BeforeEach
  void mockRepositories() {
    when(repositoryDAO.getAll()).thenReturn(Collections.singletonList(repository));
  }

  @Test
  void shouldUpdatePullRequestCommentData() {
    Comment comment1 = new Comment();
    comment1.setComment("comment1");
    comment1.setAuthor("trillian");
    comment1.setType(CommentType.COMMENT);
    Comment comment2 = new Comment();
    comment2.setAuthor("edi");
    comment2.setComment("comment2");
    comment2.setType(null);
    PullRequestComments comments = new PullRequestComments();
    comments.setComments(Arrays.asList(comment1, comment2));
    dataStore.put(comments);

    updateStep.doUpdate();

    assertThat(dataStore.getAll().values().iterator().next().getComments().size()).isEqualTo(2);
    assertThat(dataStore.getAll().values().iterator().next().getComments().get(0).getType()).isEqualTo(CommentType.COMMENT);
    assertThat(dataStore.getAll().values().iterator().next().getComments().get(0).getAuthor()).isEqualTo("trillian");
    assertThat(dataStore.getAll().values().iterator().next().getComments().get(0).getComment()).isEqualTo("comment1");
    assertThat(dataStore.getAll().values().iterator().next().getComments().get(1).getType()).isEqualTo(CommentType.COMMENT);
    assertThat(dataStore.getAll().values().iterator().next().getComments().get(1).getAuthor()).isEqualTo("edi");
    assertThat(dataStore.getAll().values().iterator().next().getComments().get(1).getComment()).isEqualTo("comment2");
  }

  @Test
  void shouldSkipIfNoDataAvailable() {
    updateStep.doUpdate();

    assertThat(dataStore.getAll().size()).isEqualTo(0);
  }
}

