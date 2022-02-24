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
package com.cloudogu.scm.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.RepositoryHookEvent;
import sonia.scm.repository.api.HookMessageProvider;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderMessageSenderTest {

  @Mock
  private ScmConfiguration scmConfiguration;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryHookEvent event;
  @Mock
  private HookMessageProvider messageProvider;

  @InjectMocks
  private ProviderMessageSender sender;

  @BeforeEach
  void setUpMessageProvider() {
    when(event.getContext().getMessageProvider())
      .thenReturn(messageProvider);
  }

  @BeforeEach
  void mockBaseUrl() {
    when(scmConfiguration.getBaseUrl()).thenReturn("https://hog/scm");
  }

  @Test
  void shouldEncodeBranchInUrl() {
    sender.sendCreatePullRequestMessage("nice+branch");

    verify(messageProvider).sendMessage("https://hog/scm/repo/null/null/pull-requests/add/changesets/?source=nice%2Bbranch");
  }
}
