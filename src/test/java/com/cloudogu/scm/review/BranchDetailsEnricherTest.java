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

import com.cloudogu.scm.review.pullrequest.dto.PullRequestDto;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.google.common.collect.ImmutableList;
import de.otto.edison.hal.HalRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.repository.BranchDetails;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@SuppressWarnings("unchecked")
class BranchDetailsEnricherTest {

  private final Repository repository = RepositoryTestData.create42Puzzle();

  @Mock
  private PullRequestMapper mapper;

  @Mock
  private PullRequestService service;

  @Mock
  private HalAppender appender;

  @InjectMocks
  private BranchDetailsEnricher enricher;

  @Nested
  class ForSupportedRepositoryType {

    @BeforeEach
    void supportPullRequests() {
      when(service.supportsPullRequests(repository)).thenReturn(true);
    }

    @Test
    void shouldEnrichWithEmbedded() {
      PullRequest pr = new PullRequest("1", "main", "develop");
      pr.setStatus(PullRequestStatus.OPEN);
      PullRequestDto dto = new PullRequestDto();
      dto.setId("1");
      dto.setSource("main");
      when(service.getAll(repository.getNamespace(), repository.getName())).thenReturn(ImmutableList.of(pr));
      when(mapper.map(pr)).thenReturn(dto);

      enricher.enrich(HalEnricherContext.of(repository, new BranchDetails("main")), appender);

      verify(appender).appendEmbedded(eq("pullRequests"), (List<HalRepresentation>) argThat(prs -> {
        List<HalRepresentation> list = (List<HalRepresentation>) prs;
        assertThat(list).hasSize(1);
        assertThat(((PullRequestDto) list.get(0)).getId()).isEqualTo("1");
        assertThat(((PullRequestDto) list.get(0)).getSource()).isEqualTo("main");
        return true;
      }));
    }

    @Test
    void shouldNotEnrichIfNotSourceBranch() {
      PullRequest pr = new PullRequest("1", "develop", "main");
      when(service.getAll(repository.getNamespace(), repository.getName())).thenReturn(ImmutableList.of(pr));

      enricher.enrich(HalEnricherContext.of(repository, new BranchDetails("main")), appender);

      verify(appender).appendEmbedded(eq("pullRequests"), (List<HalRepresentation>) argThat(hrs -> {
        assertThat(((List<HalRepresentation>) hrs)).isEmpty();
        return true;
      }));
    }

    @Test
    void shouldNotEnrichIfPrNotOpen() {
      PullRequest pr = new PullRequest("1", "main", "develop");
      pr.setStatus(PullRequestStatus.MERGED);
      PullRequestDto dto = new PullRequestDto();
      dto.setId("1");
      dto.setSource("main");
      when(service.getAll(repository.getNamespace(), repository.getName())).thenReturn(ImmutableList.of(pr));

      enricher.enrich(HalEnricherContext.of(repository, new BranchDetails("main")), appender);

      verify(appender).appendEmbedded(eq("pullRequests"), (List<HalRepresentation>) argThat(hrs -> {
        assertThat(((List<HalRepresentation>) hrs)).isEmpty();
        return true;
      }));
    }
  }

  @Test
  void shouldNotEnrichIfPullRequestsAreNotSupported() {
    when(service.supportsPullRequests(repository)).thenReturn(false);
    lenient().doThrow(RuntimeException.class).when(service).getAll(repository.getNamespace(), repository.getName());

    enricher.enrich(HalEnricherContext.of(repository, new BranchDetails("main")), appender);

    verify(appender, never()).appendEmbedded(anyString(), (List<HalRepresentation>) any(List.class));
  }

}
