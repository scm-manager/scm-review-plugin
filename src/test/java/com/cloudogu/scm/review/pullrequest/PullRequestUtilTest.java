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
