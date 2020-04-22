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
package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.api.CommentMapper;
import com.cloudogu.scm.review.comment.api.ExecutedTransitionMapper;
import com.cloudogu.scm.review.comment.api.ReplyMapper;
import com.cloudogu.scm.review.config.api.GlobalConfigMapper;
import com.cloudogu.scm.review.config.api.RepositoryConfigMapper;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.DefaultPullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.workflow.RepositoryEngineConfigMapper;
import com.google.inject.AbstractModule;
import org.mapstruct.factory.Mappers;
import sonia.scm.plugin.Extension;

@Extension
public class ModuleBinder extends AbstractModule {

  @Override
  protected void configure() {
    bind(PullRequestMapper.class).to(Mappers.getMapper(PullRequestMapper.class).getClass());
    bind(CommentMapper.class).to(Mappers.getMapper(CommentMapper.class).getClass());
    bind(ReplyMapper.class).to(Mappers.getMapper(ReplyMapper.class).getClass());
    bind(ExecutedTransitionMapper.class).to(Mappers.getMapper(ExecutedTransitionMapper.class).getClass());
    bind(PullRequestService.class).to(DefaultPullRequestService.class);
    bind(RepositoryConfigMapper.class).to(Mappers.getMapper(RepositoryConfigMapper.class).getClass());
    bind(GlobalConfigMapper.class).to(Mappers.getMapper(GlobalConfigMapper.class).getClass());
    bind(RepositoryEngineConfigMapper.class).to(Mappers.getMapper(RepositoryEngineConfigMapper.class).getClass());
  }
}
