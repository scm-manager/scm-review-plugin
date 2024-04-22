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

import React, { FC, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDiff } from "@scm-manager/ui-api";
import {
  Diff as CoreDiff,
  DiffObjectProps,
  ErrorNotification,
  Loading,
  NotFoundError,
  Notification
} from "@scm-manager/ui-components";
import { Comment } from "../types/PullRequest";
import PartialNotification from "./PartialNotification";
import { Button, Icon } from "@scm-manager/ui-core";
import styled from "styled-components";

type LoadingDiffProps = DiffObjectProps & {
  diffUrl: string;
  actions: any;
  pullRequestComments: Comment[];
  refetchOnWindowFocus?: boolean;
  stickyHeader?: boolean | number;
};

const StickyButton = styled(Button)<{ top: number }>`
  position: sticky;
  display: flex;
  margin-right: 1.25rem;
  margin-left: auto;
  top: calc(${props => props.top}px + var(--scm-navbar-main-height));
  z-index: 11;
  transition: top 200ms ease 0s;
`;

const LoadingDiff: FC<LoadingDiffProps> = ({ diffUrl, actions, pullRequestComments, stickyHeader, ...props }) => {
  const [t] = useTranslation("plugins");
  const { error, isLoading, data, fetchNextPage, isFetchingNextPage } = useDiff(diffUrl, {
    limit: 25,
    refetchOnWindowFocus: false
  });
  const [collapsed, setCollapsed] = useState(false);

  const collapseDiffs = () => {
    if (collapsed) {
      actions.uncollapseAll();
    } else {
      actions.collapseAll();
    }
    setCollapsed(current => !current);
  };

  if (error) {
    if (error instanceof NotFoundError) {
      return <Notification type="info">{t("scm-review-plugin.diff.noChangesets")}</Notification>;
    }
    return <ErrorNotification error={error} />;
  } else if (isLoading) {
    return <Loading />;
  } else if (!data?.files) {
    return null;
  }
  const buttonHeight = 40;
  const overlapBetweenPanelHeader = 5;
  return (
    <>
      <StickyButton
        onClick={collapseDiffs}
        className="mb-4 is-hidden-mobile"
        top={typeof stickyHeader === "number" && stickyHeader > buttonHeight ? (stickyHeader - buttonHeight) / 2 : 0}
      >
        <Icon className="mr-1">{collapsed ? "eye" : "eye-slash"}</Icon> {t("scm-review-plugin.diff.collapseDiffs")}
      </StickyButton>
      <CoreDiff
        diff={data.files}
        {...props}
        stickyHeader={
          typeof stickyHeader === "number" && stickyHeader > buttonHeight
            ? stickyHeader - overlapBetweenPanelHeader
            : true
        }
      />
      {data.partial ? (
        <PartialNotification
          pullRequestComments={pullRequestComments}
          pullRequestFiles={data?.files}
          fetchNextPage={fetchNextPage}
          isFetchingNextPage={isFetchingNextPage}
        />
      ) : null}
    </>
  );
};

LoadingDiff.defaultProps = {
  sideBySide: false
};

export default LoadingDiff;
