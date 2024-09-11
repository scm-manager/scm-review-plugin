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

package com.cloudogu.scm.review.pullrequest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PullRequestUtilTest {

  @Test
  void shouldCutOffLongTitle() {
    PullRequestUtil.PullRequestTitleAndDescription titleAndDescription = PullRequestUtil.determineTitleAndDescription("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.\nMore Text");
    assertThat(titleAndDescription.getTitle()).hasSizeLessThanOrEqualTo(72);
    assertThat(titleAndDescription.getTitle().charAt(69)).isNotEqualTo(' ');
    assertThat(titleAndDescription.getDescription()).startsWith("...");
    assertThat(titleAndDescription.getTitle().charAt(3)).isNotEqualTo(' ');
    assertThat(titleAndDescription.getDescription()).endsWith("More Text");
  }

  @Test
  void shouldNotCutOffBarelyTooLongTitle() {
    PullRequestUtil.PullRequestTitleAndDescription titleAndDescription = PullRequestUtil.determineTitleAndDescription("Lorem ipsum dolor sit amet, consetetur sadipscing elitr. Happy days are rainy.");
    assertThat(titleAndDescription.getTitle()).isEqualTo("Lorem ipsum dolor sit amet, consetetur sadipscing elitr. Happy days are rainy.");
    assertThat(titleAndDescription.getDescription()).isEmpty();
  }

  @Test
  void shouldRemoveNewLineBeforeDescription() {
    PullRequestUtil.PullRequestTitleAndDescription titleAndDescription = PullRequestUtil.determineTitleAndDescription("Lorem ipsum dolor sit amet, consetetur sadipscing elitr.\n\nHappy days are rainy.");
    assertThat(titleAndDescription.getTitle()).isEqualTo("Lorem ipsum dolor sit amet, consetetur sadipscing elitr.");
    assertThat(titleAndDescription.getDescription()).isEqualTo("Happy days are rainy.");
  }

}
