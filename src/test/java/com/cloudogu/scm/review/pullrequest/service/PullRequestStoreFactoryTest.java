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
package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.comment.service.CommentStoreFactory;
import com.cloudogu.scm.review.comment.service.CommentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestStoreFactoryTest {

  @Mock
  private DataStoreFactory dataStoreFactory;

  @Mock
  private DataStore<PullRequest> store;

  @InjectMocks
  private PullRequestStoreFactory storeFactory;

  @Test
  public void testCreate() {
    Repository repository = RepositoryTestData.createHeartOfGold("git");
    repository.setId("42");

    when(dataStoreFactory.getStore(any())).thenReturn((DataStore) store);
    when(dataStoreFactory.withType(any())).thenCallRealMethod();

    PullRequestStore store = storeFactory.create(repository);
    assertThat(store).isNotNull();
  }

  @Test
  public void shouldCreateCommentStore() {
    Repository repository = RepositoryTestData.createHeartOfGold("git");
    repository.setId("repo");

    when(dataStoreFactory.getStore(any())).thenReturn((DataStore) store);
    when(dataStoreFactory.withType(any())).thenCallRealMethod();

    PullRequestStore commentStore = storeFactory.create(repository);
    assertThat(commentStore).isNotNull();
  }
}
