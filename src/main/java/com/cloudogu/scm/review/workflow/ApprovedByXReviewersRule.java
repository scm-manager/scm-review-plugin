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

package com.cloudogu.scm.review.workflow;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sonia.scm.plugin.Extension;

import jakarta.validation.constraints.Min;
import jakarta.xml.bind.annotation.XmlRootElement;
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

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  @XmlRootElement
  static class Configuration {
    @Min(1)
    private int numberOfReviewers;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  static class ErrorContext {
    int expected;
    int actual;
    int missing;
  }
}
