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
package com.cloudogu.scm.review.pullrequest.landingpage;

import com.cloudogu.scm.landingpage.mydata.MyData;
import com.cloudogu.scm.landingpage.mydata.MyDataProvider;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import org.apache.shiro.SecurityUtils;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Extension
@Requires("scm-landingpage-plugin")
public class MyPullRequests implements MyDataProvider {

  private final OpenPullRequestProvider pullRequestProvider;
  private final PullRequestMapper mapper;

  @Inject
  public MyPullRequests(OpenPullRequestProvider pullRequestProvider, PullRequestMapper mapper) {
    this.pullRequestProvider = pullRequestProvider;
    this.mapper = mapper;
  }

  @Override
  public Iterable<MyData> getData() {
    String subject = SecurityUtils.getSubject().getPrincipal().toString();
    Collection<MyData> result = new ArrayList<>();
    pullRequestProvider.findOpenAndDraftPullRequests((repository, stream) -> stream
        .filter(pr -> pr.getAuthor().equals(subject))
        .forEach(pr -> result.add(new MyPullRequestData(repository, pr, mapper)))
    );
    return result;
  }

  @Override
  public Optional<String> getType() {
    return Optional.of(MyPullRequestData.class.getSimpleName());
  }
}
