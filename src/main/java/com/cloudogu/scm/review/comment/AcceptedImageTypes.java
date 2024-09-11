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

package com.cloudogu.scm.review.comment;

import java.util.Arrays;

public class AcceptedImageTypes {
  public static final String ACCEPTED_IMAGE_TYPES = "image/png|image/jpeg|image/gif";
  public static final int MAX_ACCEPTED_IMAGE_TYPE_STRING_LENGTH = Arrays.stream(ACCEPTED_IMAGE_TYPES.split("\\|")).mapToInt(String::length).max().orElse(0);
}
