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

package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.pullrequest.service.PullRequestChange;
import de.otto.edison.hal.HalRepresentation;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sonia.scm.xml.XmlInstantAdapter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
@NoArgsConstructor
public class PullRequestChangeDto extends HalRepresentation {
  private String prId;

  private String username;
  private String displayName;
  private String mail;

  private String changedAt;

  private String previousValue;

  private String currentValue;

  private String property;

  private Map<String, String> additionalInfo = new HashMap<>();

  public static PullRequestChangeDto mapToDto(PullRequestChange change) {
    PullRequestChangeDto result = new PullRequestChangeDto();

    result.setPrId(change.getPrId());
    result.setUsername(change.getUsername());
    result.setDisplayName(change.getDisplayName());
    result.setMail(change.getMail());
    result.setChangedAt(change.getChangedAt().toString());
    result.setPreviousValue(change.getPreviousValue());
    result.setCurrentValue(change.getCurrentValue());
    result.setProperty(change.getProperty());
    result.setAdditionalInfo(change.getAdditionalInfo());

    return result;
  }
}
