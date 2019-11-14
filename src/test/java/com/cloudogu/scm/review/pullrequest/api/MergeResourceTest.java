package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.ExceptionMessageMapper;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.github.sdorra.shiro.SubjectAware;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.api.MergeDryRunCommandResult;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sonia.scm.repository.api.MergeStrategy.SQUASH;

@SubjectAware(
  configuration = "classpath:com/cloudogu/scm/review/shiro.ini",
  password = "secret"
)
@ExtendWith(MockitoExtension.class)
class MergeResourceTest {

  private final String MERGE_URL = "/" + MergeResource.MERGE_PATH_V2 + "/space/name/1";

  private Dispatcher dispatcher;
  private final MockHttpResponse response = new MockHttpResponse();

  @Mock
  private MergeService mergeService;

  @InjectMocks
  private MergeResource mergeResource;

  @BeforeEach
  void initDispatcher() {
    dispatcher = MockDispatcherFactory.createDispatcher();
    dispatcher.getProviderFactory().register(new ExceptionMessageMapper());
    dispatcher.getRegistry().addSingletonResource(mergeResource);
  }

  @Test
  void shouldMergeWithSquash() throws URISyntaxException, IOException {
    byte[] mergeCommitJson = loadJson("com/cloudogu/scm/review/mergeCommit.json");

    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "?strategy=SQUASH", mergeCommitJson);

    dispatcher.invoke(request, response);
    verify(mergeService).merge(eq(new NamespaceAndName("space", "name")), eq("1"), any(), eq(SQUASH));
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldHandleSuccessfulDryRun() throws IOException, URISyntaxException {
    when(mergeService.dryRun(any(), any())).thenReturn(new MergeDryRunCommandResult(true));

    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand.json");

    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "/dry-run", mergeCommandJson);

    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldHandleFailedDryRun() throws IOException, URISyntaxException {
    when(mergeService.dryRun(any(), any())).thenReturn(new MergeDryRunCommandResult(false));

    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand.json");
    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "/dry-run", mergeCommandJson);
    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(409);
  }

  @Test
  void shouldReturnBadRequestIfMergeCommandDtoInvalid() throws IOException, URISyntaxException {
    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand_invalid.json");
    MockHttpRequest request = createHttpPostRequest(MERGE_URL + "/dry-run", mergeCommandJson);
    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void shouldCreateSquashCommitMessage() throws IOException, URISyntaxException {
    when(mergeService.createDefaultCommitMessage(any(), any(), eq(SQUASH))).thenReturn("successful");
    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand.json");
    MockHttpRequest request = createHttpGetRequest(MERGE_URL + "/commit-message/?strategy=SQUASH", mergeCommandJson);
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

  private MockHttpRequest createHttpGetRequest(String url, byte[] content) throws URISyntaxException {
    return MockHttpRequest
      .get(url)
      .content(content)
      .contentType("application/vnd.scmm-mergeCommand+json");
  }

  private byte[] loadJson(String s) throws IOException {
    URL url = getResource(s);
    return toByteArray(url);
  }
}
