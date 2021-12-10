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
import styled from "styled-components";
import { useDiff } from "@scm-manager/ui-api";
import {
  Button,
  Diff as CoreDiff,
  DiffObjectProps,
  ErrorNotification,
  Level,
  Loading,
  NotFoundError,
  Notification
} from "@scm-manager/ui-components";
import { Comment } from "../types/PullRequest";
import PartialNotification from "./PartialNotification";

type LoadingDiffProps = DiffObjectProps & {
  diffUrl: string;
  actions: any;
  pullRequestComments: Comment[];
  refetchOnWindowFocus?: boolean;
};

const LevelWithMargin = styled(Level)`
  margin-bottom: 1rem !important;
`;

const LoadingDiff: FC<LoadingDiffProps> = ({ diffUrl, actions, pullRequestComments, ...props }) => {
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
  return (
    <>
      <LevelWithMargin
        right={
          <Button
            action={collapseDiffs}
            color="default"
            icon={collapsed ? "eye" : "eye-slash"}
            label={t("scm-review-plugin.diff.collapseDiffs")}
            reducedMobile={true}
          />
        }
      />
      <CoreDiff diff={data.files} {...props} />
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
