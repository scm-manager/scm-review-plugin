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
  Notification,
  DiffDropDown,
  DiffStatistics
} from "@scm-manager/ui-components";
import { Comment } from "../types/PullRequest";
import PartialNotification from "./PartialNotification";
import styled from "styled-components";

type LoadingDiffProps = DiffObjectProps & {
  diffUrl: string;
  actions: any;
  pullRequestComments: Comment[];
  refetchOnWindowFocus?: boolean;
  stickyHeader?: boolean | number;
};

const StickyContainer = styled.div<{ top: number }>`
  position: sticky;
  display: flex;
  justify-content: end;
  margin: 0 1.25rem 1rem 0;
  gap: 0.5rem;
  top: calc(${props => props.top}px + var(--scm-navbar-main-height));
  z-index: 11;
`;

const LoadingDiff: FC<LoadingDiffProps> = ({ diffUrl, actions, pullRequestComments, stickyHeader, ...props }) => {
  const [t] = useTranslation("plugins");
  const [ignoreWhitespace, setIgnoreWhitespace] = useState(false);
  const { error, isLoading, data, fetchNextPage, isFetchingNextPage } = useDiff(diffUrl, {
    limit: 25,
    refetchOnWindowFocus: false,
    ignoreWhitespace: ignoreWhitespace ? "ALL" : "NONE"
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

  const ignoreWhitespaces = () => {
    setIgnoreWhitespace(!ignoreWhitespace);
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
      <DiffStatistics data={data.statistics}/>
      <StickyContainer
        className="is-hidden-mobile"
        top={typeof stickyHeader === "number" && stickyHeader > buttonHeight ? (stickyHeader - buttonHeight) / 2 : 0}
      >
        <DiffDropDown collapseDiffs={collapseDiffs} ignoreWhitespaces={ignoreWhitespaces} renderOnMount={true}/>
      </StickyContainer>
      <CoreDiff
        diff={data.files}
        ignoreWhitespace={ignoreWhitespace ? "ALL" : "NONE"}
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
