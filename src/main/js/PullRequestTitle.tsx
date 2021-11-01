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

import React, { FC } from "react";
import { PullRequest } from "./types/PullRequest";
import { useBinder } from "@scm-manager/ui-extensions";
import { SplitAndReplace, Replacement } from "@scm-manager/ui-components";

type Props = {
  pullRequest: PullRequest;
};

const PullRequestTitle: FC<Props> = ({ pullRequest }) => {
  const binder = useBinder();
  const value = pullRequest.title || "";

  const replacements: ((pullRequest: PullRequest, value: string) => Replacement[])[] = binder.getExtensions(
    "reviewPlugin.pullrequest.title.tokens",
    {
      pullRequest,
      value
    }
  );

  return <SplitAndReplace text={value} replacements={replacements.flatMap(r => r(pullRequest, value))} />;
};

export default PullRequestTitle;
