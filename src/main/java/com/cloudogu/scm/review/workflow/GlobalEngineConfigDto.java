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

import de.otto.edison.hal.Links;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlobalEngineConfigDto extends RepositoryEngineConfigDto {

  private boolean disableRepositoryConfiguration;

  public GlobalEngineConfigDto(Links links) {
    super(links);
  }

  public GlobalEngineConfigDto(Links links, List<AppliedRuleDto> rules, boolean enabled, boolean disableRepositoryConfiguration) {
    super(links, rules, enabled);
    this.disableRepositoryConfiguration = disableRepositoryConfiguration;
  }

}
