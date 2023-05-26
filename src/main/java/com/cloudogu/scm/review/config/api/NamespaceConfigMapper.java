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
package com.cloudogu.scm.review.config.api;

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.NamespacePullRequestConfig;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.LinkBuilder;

import javax.ws.rs.core.UriInfo;
import java.util.List;

@Mapper
public abstract class NamespaceConfigMapper {

  public abstract NamespacePullRequestConfigDto map(NamespacePullRequestConfig config, @Context String namespace, @Context UriInfo uriInfo);

  public abstract NamespacePullRequestConfig map(NamespacePullRequestConfigDto configDto);

  abstract List<BasePullRequestConfig.ProtectionBypass> map(List<BasePullRequestConfigDto.ProtectionBypassDto> bypassDtos);
  abstract BasePullRequestConfig.ProtectionBypass map(BasePullRequestConfigDto.ProtectionBypassDto bypassDto);

  abstract BasePullRequestConfigDto.ProtectionBypassDto map(BasePullRequestConfig.ProtectionBypass bypass);
  abstract List<BasePullRequestConfigDto.ProtectionBypassDto> mapToDto(List<BasePullRequestConfig.ProtectionBypass> bypasses);

  @ObjectFactory
  NamespacePullRequestConfigDto createForNamespace(@Context String namespace, @Context UriInfo uriInfo) {
    LinkBuilder linkBuilder = new LinkBuilder(uriInfo::getBaseUri, NamespaceConfigResource.class);
    Links.Builder halLinks = new Links.Builder();

    halLinks.self(linkBuilder.method("getNamespaceConfig").parameters(namespace).href());

    if (PermissionCheck.mayConfigure(namespace)) {
      halLinks.single(Link.link("update", linkBuilder.method("setNamespaceConfig").parameters(namespace).href()));
    }
    return new NamespacePullRequestConfigDto(halLinks.build());
  }
}
