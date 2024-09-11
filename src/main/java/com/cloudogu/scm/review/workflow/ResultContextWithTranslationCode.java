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
 *           "obstacle": "I never use translation keys.",
 *         }
 *       }
 *     }
 *   }
 * </pre>
 * <br>
 * This format is only required if the rule actually returns a translation code in the result context.<br>
 * If the translation code is null, the default method of forming the translation key is used.<br>
 * The translation code is never used for obstacles.<br>
 */
public interface ResultContextWithTranslationCode {
  String getTranslationCode();
}
