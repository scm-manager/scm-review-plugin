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

import lombok.AllArgsConstructor;
import lombok.Getter;

public final class PullRequestUtil {
  private PullRequestUtil() {}

  public static PullRequestTitleAndDescription determineTitleAndDescription(String changesetDescription) {
    String[] split = changesetDescription.split("\\R", 2);
    String title = split[0];
    String description = "";

    if (split.length > 1) {
      description = split[1];
    }

    if (title.length() > 80) {
      description = "..." + title.substring(69).trim() + System.lineSeparator() + description;
      title = title.substring(0, 69).trim() + "...";
    }

    return new PullRequestTitleAndDescription(title.trim(), description.trim());
  }

  @AllArgsConstructor
  @Getter
  public static class PullRequestTitleAndDescription {
    private final String title;
    private final String description;
  }
}
