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
package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.service.MergeCheckResult;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.github.sdorra.shiro.SubjectAware;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.web.RestDispatcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.api.MergeStrategy.FAST_FORWARD_IF_POSSIBLE;
import static sonia.scm.repository.api.MergeStrategy.SQUASH;

@SubjectAware(
  configuration = "classpath:com/cloudogu/scm/review/shiro.ini",
  password = "secret"
)
@ExtendWith(MockitoExtension.class)
class MergeResourceTest {

  private final String MERGE_URL = "/" + MergeResource.MERGE_PATH_V2 + "/space/name/1";

  private RestDispatcher dispatcher;
  private final MockHttpResponse response = new MockHttpResponse();

  @Mock
  private MergeService mergeService;

  @InjectMocks
  private MergeResource mergeResource;

  @BeforeEach
  void initDispatcher() {
    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(mergeResource);
  }

  @Test
  void shouldMergeWithSquash() throws URISyntaxException, IOException {
    byte[] mergeCommitJson = loadJson("com/cloudogu/scm/review/mergeCommit.json");

    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "?strategy=SQUASH", mergeCommitJson);

    dispatcher.invoke(request, response);
    verify(mergeService).merge(eq(new NamespaceAndName("space", "name")), eq("1"), any(), eq(SQUASH), anyBoolean());
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldEmergencyMergeWithFastForward() throws URISyntaxException, IOException {
    byte[] mergeCommitJson = loadJson("com/cloudogu/scm/review/mergeCommit.json");

    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "/emergency" + "?strategy=FAST_FORWARD_IF_POSSIBLE", mergeCommitJson);

    dispatcher.invoke(request, response);
    verify(mergeService).merge(eq(new NamespaceAndName("space", "name")), eq("1"), any(), eq(FAST_FORWARD_IF_POSSIBLE), anyBoolean());
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldHandleSuccessfulDryRun() throws IOException, URISyntaxException {
    when(mergeService.checkMerge(any(), any())).thenReturn(new MergeCheckResult(false, emptyList()));

    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand.json");

    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "/merge-check", mergeCommandJson);

    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"hasConflicts\":false");
  }

  @Test
  void shouldHandleFailedDryRun() throws IOException, URISyntaxException {
    when(mergeService.checkMerge(any(), any())).thenReturn(new MergeCheckResult(true, emptyList()));

    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand.json");
    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "/merge-check", mergeCommandJson);
    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"hasConflicts\":true");
  }

  @Test
  void shouldCreateSquashCommitMessage() throws IOException, URISyntaxException {
    when(mergeService.createDefaultCommitMessage(any(), any(), eq(SQUASH))).thenReturn("successful");
    MockHttpRequest request = createHttpGetRequest(MERGE_URL + "/commit-message/?strategy=SQUASH");
    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("successful");
  }

  private MockHttpRequest createHttpPostRequest(String url, byte[] content) throws URISyntaxException {
    return MockHttpRequest
      .post(url)
      .content(content)
      .contentType("application/vnd.scmm-mergeCommand+json");
  }

  private MockHttpRequest createHttpGetRequest(String url) throws URISyntaxException {
    return MockHttpRequest
      .get(url);
  }

  private byte[] loadJson(String s) throws IOException {
    URL url = getResource(s);
    return toByteArray(url);
  }
}
