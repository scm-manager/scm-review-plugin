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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;
import sonia.scm.plugin.Extension;

import javax.validation.constraints.Min;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

@Extension
public class ApprovedByXReviewersRule implements Rule {

  @Override
  public Optional<Class<?>> getConfigurationType() {
    return Optional.of(Configuration.class);
  }

  @Override
  public Result validate(Context context) {
    final PullRequest pullRequest = context.getPullRequest();
    Configuration configuration = context.getConfiguration(Configuration.class);
    long numberOfReviewers = pullRequest.getReviewer().values().stream().filter(b -> b).count();
    return numberOfReviewers >= configuration.numberOfReviewers ? success() : failed(
      new ErrorContext(configuration.numberOfReviewers, (int) numberOfReviewers, (int) (configuration.numberOfReviewers - numberOfReviewers))
    );
  }

  @Data
  @AllArgsConstructor
  @XmlRootElement
  static class Configuration {
    @Min(1)
    private int numberOfReviewers;
  }

  @Value
  @XmlRootElement
  static class ErrorContext {
    int expected;
    int actual;
    int missing;
  }
}
