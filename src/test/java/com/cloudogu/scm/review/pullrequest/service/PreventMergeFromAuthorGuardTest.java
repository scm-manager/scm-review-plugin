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

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.config.service.ConfigService;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.user.User;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreventMergeFromAuthorGuardTest {

  private static final String CURRENT_USER = "dent";

  private static PullRequest PULL_REQUEST = TestData.createPullRequest();
  private static Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  Subject subject;
  @Mock
  ConfigService configService;

  @InjectMocks
  PreventMergeFromAuthorGuard guard;

  @BeforeEach
  void initSubject() {
    ThreadContext.bind(subject);
    lenient().when(subject.getPrincipals().oneByType(User.class)).thenReturn(new User(CURRENT_USER));
  }

  @Test
  void shouldNotCheckAnythingWhenDisabled() {
    when(configService.isPreventMergeFromAuthor(REPOSITORY)).thenReturn(false);

    Collection<MergeObstacle> obstacles = guard.getObstacles(REPOSITORY, PULL_REQUEST);

    assertThat(obstacles).isEmpty();
  }

  @Test
  void shouldPreventMergeFromAuthorWhenEnabled() {
    when(configService.isPreventMergeFromAuthor(REPOSITORY)).thenReturn(true);
    PULL_REQUEST.setAuthor(CURRENT_USER);

    Collection<MergeObstacle> obstacles = guard.getObstacles(REPOSITORY, PULL_REQUEST);

    assertThat(obstacles)
      .extracting("key")
      .containsExactly("scm-review-plugin.pullRequest.guard.mergeFromAuthorNotAllowed.obstacle");
  }

  @Test
  void shouldNotPreventMergeFromOtherUsersWhenEnabled() {
    when(configService.isPreventMergeFromAuthor(REPOSITORY)).thenReturn(true);
    PULL_REQUEST.setAuthor("trillian");

    Collection<MergeObstacle> obstacles = guard.getObstacles(REPOSITORY, PULL_REQUEST);

    assertThat(obstacles).isEmpty();
  }
}
