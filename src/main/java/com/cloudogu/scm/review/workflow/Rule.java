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

import sonia.scm.plugin.ExtensionPoint;

import java.util.Optional;

/**
 * Each {@link Rule} class implementation defines a type of workflow rule.<br>
 * <br>
 * Rules applied to your repositories are represented by {@link AppliedRule}s<br>
 * to support multiple {@link Rule}s of the same type with distinct configuration.
 */
@ExtensionPoint
public interface Rule {

  /**
   * Whether this {@link Rule} allows multiple {@link AppliedRule}s to be created or only one.
   */
  default boolean isApplicableMultipleTimes() {
    return false;
  }

  /**
   * If a {@link Rule} is configurable, {@link #getConfigurationType()} describes the type of configuration.
   * The returned class has to be a serializable {@link jakarta.xml.bind.annotation.XmlRootElement}.
   */
  default Optional<Class<?>> getConfigurationType() {
    return Optional.empty();
  }

  /**
   * Implement this method to describe the concrete logic of your {@link Rule}.
   *
   * If the {@link Rule} is configurable, the {@link AppliedRule}'s configuration is passed as part of the context.
   */
  Result validate(Context context);

  default Result success() {
    return Result.success(getClass());
  }

  default Result failed() {
    return Result.failed(getClass());
  }

  default Result failed(Object context) {
    return Result.failed(getClass(), context);
  }

}
