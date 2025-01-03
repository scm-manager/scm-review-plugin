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

package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.service.MergeCheckResult;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.MergeService.CommitDefaults;
import com.fasterxml.jackson.databind.JsonNode;
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
import sonia.scm.repository.api.MergePreventReason;
import sonia.scm.repository.api.MergePreventReasonType;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.User;
import sonia.scm.web.JsonMockHttpResponse;
import sonia.scm.web.RestDispatcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

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
import static sonia.scm.repository.api.MergeStrategy.MERGE_COMMIT;
import static sonia.scm.repository.api.MergeStrategy.REBASE;
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
    when(mergeService.checkMerge(any(), any())).thenReturn(new MergeCheckResult(false, emptyList(), null));

    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand.json");

    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "/merge-check", mergeCommandJson);

    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"hasConflicts\":false");
  }

  @Test
  void shouldHandleFailedDryRun_FileConflicts() throws IOException, URISyntaxException {
    when(mergeService.checkMerge(any(), any())).thenReturn(
      new MergeCheckResult(
        true,
        emptyList(),
        List.of(new MergePreventReason(MergePreventReasonType.FILE_CONFLICTS))
      )
    );

    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand.json");
    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "/merge-check", mergeCommandJson);
    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"hasConflicts\":true");
    assertThat(response.getContentAsString()).contains("\"mergePreventReasons\":[{\"type\":\"FILE_CONFLICTS\",\"affectedPaths\":[]}]");
  }

  @Test
  void shouldHandleFailedDryRun_ExternalMergeTool() throws IOException, URISyntaxException {
    when(mergeService.checkMerge(any(), any())).thenReturn(
      new MergeCheckResult(
        true,
        emptyList(),
        List.of(new MergePreventReason(MergePreventReasonType.EXTERNAL_MERGE_TOOL))
      )
    );

    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand.json");
    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "/merge-check", mergeCommandJson);
    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("\"hasConflicts\":true");
    assertThat(response.getContentAsString()).contains("\"mergePreventReasons\":[{\"type\":\"EXTERNAL_MERGE_TOOL\",\"affectedPaths\":[]}]");
  }

  @Test
  void shouldCreateSquashCommitMessage() throws IOException, URISyntaxException {
    when(mergeService.createCommitDefaults(any(), any(), eq(SQUASH)))
      .thenReturn(new CommitDefaults("successful", DisplayUser.from(new User("Arthur Dent"))));
    MockHttpRequest request = createHttpGetRequest(MERGE_URL + "/commit-message/?strategy=SQUASH");
    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("successful");
  }

  @Test
  void shouldGetSquashMergeStrategyInfoWithoutMail() throws URISyntaxException {
    when(mergeService.createCommitDefaults(any(), any(), eq(SQUASH)))
      .thenReturn(new CommitDefaults("happy days", DisplayUser.from(new User("Arthur Dent"))));
    when(mergeService.isCommitMessageDisabled(SQUASH)).thenReturn(true);
    when(mergeService.createMergeCommitMessageHint(SQUASH)).thenReturn(null);
    MockHttpRequest request = createHttpGetRequest(MERGE_URL + "/merge-strategy-info/?strategy=SQUASH");
    JsonMockHttpResponse response = new JsonMockHttpResponse();
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    JsonNode jsonResponse = response.getContentAsJson();
    assertThat(jsonResponse.get("commitMessageDisabled").asBoolean()).isTrue();
    assertThat(jsonResponse.get("defaultCommitMessage").asText()).isEqualTo("happy days");
    assertThat(jsonResponse.get("commitAuthor").asText()).isEqualTo("Arthur Dent");
    assertThat(jsonResponse.get("commitMessageHint")).isNull();
  }

  @Test
  void shouldGetSquashMergeStrategyInfoWithMail() throws URISyntaxException {
    when(mergeService.createCommitDefaults(any(), any(), eq(SQUASH)))
      .thenReturn(new CommitDefaults("happy days", DisplayUser.from(new User("dent", "Arthur Dent", "arthur@hitchhiker.com"))));
    MockHttpRequest request = createHttpGetRequest(MERGE_URL + "/merge-strategy-info/?strategy=SQUASH");
    JsonMockHttpResponse response = new JsonMockHttpResponse();
    dispatcher.invoke(request, response);
    JsonNode jsonResponse = response.getContentAsJson();
    assertThat(jsonResponse.get("commitAuthor").asText()).isEqualTo("Arthur Dent <arthur@hitchhiker.com>");
  }

  @Test
  void shouldNotSetCommitAuthorForNonSquash() throws URISyntaxException {
    when(mergeService.createCommitDefaults(any(), any(), eq(MERGE_COMMIT)))
      .thenReturn(new CommitDefaults("happy days", null));
    MockHttpRequest request = createHttpGetRequest(MERGE_URL + "/merge-strategy-info/?strategy=MERGE_COMMIT");
    JsonMockHttpResponse response = new JsonMockHttpResponse();
    dispatcher.invoke(request, response);
    JsonNode jsonResponse = response.getContentAsJson();
    assertThat(jsonResponse.get("commitAuthor")).isNull();
  }

  @Test
  void shouldGetRebaseMergeStrategyInfo() throws URISyntaxException, UnsupportedEncodingException {
    CommitDefaults commitDefaults = new CommitDefaults("happy days", DisplayUser.from(new User("Arthur Dent")));
    when(mergeService.createCommitDefaults(any(), any(), eq(REBASE))).thenReturn(commitDefaults);
    when(mergeService.isCommitMessageDisabled(REBASE)).thenReturn(true);
    when(mergeService.createMergeCommitMessageHint(REBASE)).thenReturn(null);
    MockHttpRequest request = createHttpGetRequest(MERGE_URL + "/merge-strategy-info/?strategy=REBASE");
    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("happy days").contains("true").doesNotContain("commitMessageHint");
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
