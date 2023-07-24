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
import { useStatusbar } from "./useStatusbar";
import { SmallLoadingSpinner } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import { PullRequest } from "../types/PullRequest";
import StatusIcon, { getColor, getIcon } from "./StatusIcon";
import ModalRow from "./ModalRow";
import { Tooltip } from "@scm-manager/ui-overlays";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const PullRequestStatusColumn: FC<Props> = ({ pullRequest, repository }) => {
  const { data, error, isLoading } = useStatusbar(repository, pullRequest);

  if (isLoading) {
    return <SmallLoadingSpinner className="is-inline-block" />;
  }

  if (error || !data) {
    return null;
  }

  const icon = <StatusIcon color={getColor(data.results)} icon={getIcon(data.results)} />;

  if (!data.results.length) {
    return icon;
  }

  return (
    <Tooltip
      message={
        <>
          {data.results.map(r => (
            <ModalRow key={r.rule} result={r} />
          ))}
        </>
      }
    >
      {icon}
    </Tooltip>
  );
};

export default PullRequestStatusColumn;
