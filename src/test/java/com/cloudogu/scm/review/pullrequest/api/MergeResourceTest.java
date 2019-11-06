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

    MockHttpRequest request = createHttpRequest();

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void shouldReturnConflictsIfMergeNotSuccessful() throws URISyntaxException, IOException {
    ImmutableList<String> conflicts = ImmutableList.of("a", "b");
    when(mergeService.merge(any(), any(), any())).thenReturn(MergeCommandResult.failure(conflicts));

    MockHttpRequest request = createHttpRequest();

    dispatcher.invoke(request, response);
    assertThat(response.getStatus()).isEqualTo(409);
  }

  private MockHttpRequest createHttpRequest() throws IOException, URISyntaxException {
    byte[] mergeCommitJson = loadJson("com/cloudogu/scm/review/mergeCommit.json");

    return MockHttpRequest
      .post("/" + MergeResource.MERGE_PATH_V2 + "/space/name")
      .content(mergeCommitJson)
      .contentType("application/vnd.scmm-mergeCommand+json");
  }

  private byte[] loadJson(String s) throws IOException {
    URL url = getResource(s);
    return toByteArray(url);
  }
}
