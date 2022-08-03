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
import { SmallLoadingSpinner, NoStyleButton } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import { PullRequest } from "../types/PullRequest";
import StatusIcon, { getColor, getIcon } from "./StatusIcon";
import * as Tooltip from "@radix-ui/react-tooltip";
import ModalRow from "./ModalRow";
import styled from "styled-components";

const StyledArrow = styled(Tooltip.Arrow)`
  fill: var(--scm-popover-border-color);
`;

const PopoverWrapper = styled.div`
  z-index: 500;
`;

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const PullRequestStatusColumn: FC<Props> = ({ pullRequest, repository }) => {
  const { data, error, isLoading } = useStatusbar(repository, pullRequest);

  if (isLoading) {
    return <SmallLoadingSpinner />;
  }

  if (error || !data) {
    return null;
  }

  const icon = <StatusIcon color={getColor(data.results)} icon={getIcon(data.results)} />;

  if (!data.results.length) {
    return icon;
  }

  return (
    <Tooltip.Provider>
      <Tooltip.Root>
        <Tooltip.Trigger asChild={true}>
          <NoStyleButton>{icon}</NoStyleButton>
        </Tooltip.Trigger>
        <Tooltip.Portal>
          <Tooltip.Content>
            <PopoverWrapper className="box m-0 popover">
              {data.results.map(r => (
                <ModalRow result={r} />
              ))}
            </PopoverWrapper>
            <StyledArrow />
          </Tooltip.Content>
        </Tooltip.Portal>
      </Tooltip.Root>
    </Tooltip.Provider>
  );
};

export default PullRequestStatusColumn;
