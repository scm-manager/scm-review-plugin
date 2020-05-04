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

/**
 * Optional interface for rule results to provide type-safe
 * definition of custom translation keys.<br>
 * <br>
 * The key is added at the end of the default translation key.
 * This results in the requirement of a sub-object for success and failure
 * translation keys.<br>
 * <br>
 * <b>Example:</b>
 * <pre>
 *   {
 *     "workflow": {
 *       "rule": {
 *         "MyClassName": {
 *           "failed": {
 *             "MyErrorCode1": "This is the first type of error.",
 *             "MyErrorCode2": "This is the second type of error."
 *           },
 *           "success": {
 *             "MySuccessCode1": "This is the first type of success message.",
 *             "MySuccessCode2": "This is the second type of success message."
 *           },
 *           "obstacle": {
 *             "MyErrorCode1": "This is the first type of obstacle.",
 *             "MyErrorCode2": "This is the second type of obstacle."
 *           },
 *         }
 *       }
 *     }
 *   }
 * </pre>
 * <br>
 * This format is only required if the rule actually returns a translation code in the result context.<br>
 * If the translation code is null, the default method of forming the translation key is used.<br>
 * In the event of an error, the same translation code is used for both obstacle and failure message.
 */
public interface ResultContextWithTranslationCode {
  String getTranslationCode();
}
