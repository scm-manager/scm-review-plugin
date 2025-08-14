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
  DiffStatistics,
  DiffFileTree,
  FileTreeContent,
  getFileNameFromHash,
  WhitespaceMode,
  LayoutRadioButtons,
  useLayoutState,
  devices
} from "@scm-manager/ui-components";
import { Comment } from "../types/PullRequest";
import PartialNotification from "./PartialNotification";
import styled from "styled-components";
import { useHistory, useLocation } from "react-router-dom";

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
  top: calc(${(props) => props.top}px + var(--scm-navbar-main-height));
  z-index: 11;
`;

export const StickyTreeContainer = styled.div`
  @media (min-width: ${devices.widescreen.width}px) {
    position: sticky;
    flex: none;
    width: 25%;
  }
  top: 6rem;
  height: 100%;
`;

export const Divider = styled.div`
  margin-bottom: 16px;
  margin-left: 12px;
  margin-right: 12px;
  border-bottom: 1px solid var(--scm-border-color);
  box-shadow: 0 24px 3px -24px var(--scm-border-color);
`;

const LoadingDiff: FC<LoadingDiffProps> = ({ diffUrl, actions, pullRequestComments, stickyHeader, ...props }) => {
  const [t] = useTranslation("plugins");
  const [ignoreWhitespace, setIgnoreWhitespace] = useState<WhitespaceMode>("NONE");
  const [layout, setLayout] = useLayoutState();
  const { error, isLoading, data, fetchNextPage, isFetchingNextPage } = useDiff(diffUrl, {
    limit: 25,
    refetchOnWindowFocus: false,
    ignoreWhitespace: ignoreWhitespace,
  });
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const history = useHistory();
  const [prevHash, setPrevHash] = useState("");

  const setFilePath = (path: string) => {
    setPrevHash("");
    setLayout("Both");
    history.push(`#diff-${encodeURIComponent(path)}`);
  };

  const collapseDiffs = () => {
    if (collapsed) {
      actions.uncollapseAll();
    } else {
      actions.collapseAll();
    }
    setCollapsed((current) => !current);
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
    <div className="columns is-multiline">
      <div className="column is-four-fifth p-0 pt-3 pl-2">
        <DiffStatistics data={data.statistics} />
      </div>
      <StickyContainer
        className="is-hidden-mobile column is-one-fifth p-0"
        top={typeof stickyHeader === "number" && stickyHeader > buttonHeight ? (stickyHeader - buttonHeight) / 2 : 0}
      >
        <DiffDropDown
          collapseDiffs={collapseDiffs}
          renderOnMount={true}
          ignoreWhitespacesMode={ignoreWhitespace}
          setIgnoreWhitespacesMode={(whiteSpaceMode: WhitespaceMode) => {
            setIgnoreWhitespace(whiteSpaceMode);
          }}
        />
      </StickyContainer>
      <div className="column is-full p-0 pl-3 pb-4">
        <LayoutRadioButtons layout={layout} setLayout={setLayout} />
      </div>
      <StickyTreeContainer
        className={
          (layout === "Both" ? "column pl-3" : "column pl-3 is-full") +
          (layout !== "Diff" ? "" : " is-hidden")
        }
      >
        <FileTreeContent isBorder={layout !== "Diff"}>
          <h3 className={"title is-6 mt-5 pl-3"}>{t("scm-review-plugin.diff.treeTitle")}</h3>
          <Divider />
          {data?.tree && (
            <DiffFileTree
              tree={data.tree}
              currentFile={decodeURIComponent(getFileNameFromHash(location.hash) ?? "")}
              setCurrentFile={setFilePath}
            />
          )}
        </FileTreeContent>
      </StickyTreeContainer>
      <div className={layout !== "Tree" ? "column" : "is-hidden"}>
        <CoreDiff
          diff={data.files}
          ignoreWhitespace={ignoreWhitespace}
          fetchNextPage={fetchNextPage}
          isFetchingNextPage={isFetchingNextPage}
          isDataPartial={data.partial}
          prevHash={prevHash}
          setPrevHash={setPrevHash}
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
      </div>
    </div>
  );
};

LoadingDiff.defaultProps = {
  sideBySide: false,
};

export default LoadingDiff;
