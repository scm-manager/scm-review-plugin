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
