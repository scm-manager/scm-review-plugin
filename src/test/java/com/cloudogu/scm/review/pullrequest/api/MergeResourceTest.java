package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.ExceptionMessageMapper;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.github.sdorra.shiro.SubjectAware;
import com.google.common.collect.ImmutableList;
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
import sonia.scm.api.v2.resources.MergeResultToDtoMapper;
import sonia.scm.repository.api.MergeCommandResult;
import sonia.scm.repository.api.MergeDryRunCommandResult;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SubjectAware(
  configuration = "classpath:com/cloudogu/scm/review/shiro.ini",
  password = "secret"
)
@ExtendWith(MockitoExtension.class)
class MergeResourceTest {

  private final String MERGE_URL = "/" + MergeResource.MERGE_PATH_V2 + "/space/name";

  private Dispatcher dispatcher;
  private final MockHttpResponse response = new MockHttpResponse();

  @Mock
  private MergeService mergeService;
  @Mock
  private MergeResultToDtoMapper mergeResultToDtoMapper;

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
    when(mergeService.merge(any(), any(), any())).thenReturn(MergeCommandResult.success());
    byte[] mergeCommitJson = loadJson("com/cloudogu/scm/review/mergeCommit.json");

    MockHttpRequest request = createHttpRequest(MERGE_URL, mergeCommitJson);

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldReturnConflictsIfMergeNotSuccessful() throws URISyntaxException, IOException {
    ImmutableList<String> conflicts = ImmutableList.of("a", "b");
    when(mergeService.merge(any(), any(), any())).thenReturn(MergeCommandResult.failure(conflicts));
    byte[] mergeCommitJson = loadJson("com/cloudogu/scm/review/mergeCommit.json");

    MockHttpRequest request = createHttpRequest(MERGE_URL, mergeCommitJson);

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(409);
  }

  @Test
  void shouldHandleSuccessfulDryRun() throws IOException, URISyntaxException {
    when(mergeService.dryRun(any(), any())).thenReturn(new MergeDryRunCommandResult(true));

    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand.json");

    MockHttpRequest request = createHttpRequest(MERGE_URL + "/dry-run", mergeCommandJson);

    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldHandleFailedDryRun() throws IOException, URISyntaxException {
    when(mergeService.dryRun(any(), any())).thenReturn(new MergeDryRunCommandResult(false));

    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand.json");
    MockHttpRequest request = createHttpRequest(MERGE_URL + "/dry-run", mergeCommandJson);
    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(409);
  }

  @Test
  void shouldReturnBadRequestIfMergeCommandDtoInvalid() throws IOException, URISyntaxException {
    byte[] mergeCommandJson = loadJson("com/cloudogu/scm/review/mergeCommand_invalid.json");
    MockHttpRequest request = createHttpRequest(MERGE_URL + "/dry-run", mergeCommandJson);
    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);

    assertThat(response.getStatus()).isEqualTo(400);
  }

  private MockHttpRequest createHttpRequest(String url, byte[] content) throws IOException, URISyntaxException {
    return MockHttpRequest
      .post(url)
      .content(content)
      .contentType("application/vnd.scmm-mergeCommand+json");
  }

  private byte[] loadJson(String s) throws IOException {
    URL url = getResource(s);
    return toByteArray(url);
  }
}
