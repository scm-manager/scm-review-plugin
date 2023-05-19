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
import { Button, Tooltip } from "@scm-manager/ui-components";
import ManualMergeInformation from "./ManualMergeInformation";
import { MergeCheck, MergeCommit, PullRequest } from "./types/PullRequest";
import { Link, Repository } from "@scm-manager/ui-types";
import MergeModal from "./MergeModal";
import OverrideModal from "./OverrideModal";

type Props = {
  merge: (strategy: string, commit: MergeCommit, emergency: boolean) => void;
  repository: Repository;
  mergeCheck?: MergeCheck;
  loading: boolean;
  pullRequest: PullRequest;
};

const MergeButton: FC<Props> = ({ merge, repository, pullRequest, loading, mergeCheck }) => {
  const [t] = useTranslation("plugins");
  const [mergeInformation, setMergeInformation] = useState(false);
  const [showMergeModal, setShowMergeModal] = useState(false);
  const [showOverrideModal, setShowOverrideModal] = useState(false);
  const [overrideMessage, setOverrideMessage] = useState("");

  const mergeWithOverride = (message: string) => {
    setOverrideMessage(message);
    setShowOverrideModal(false);
    setShowMergeModal(true);
  };

  const existsNotOverrideableObstacles = () => {
    return mergeCheck ? mergeCheck.mergeObstacles.filter(obstacle => !obstacle.overrideable).length > 0 : false;
  };

  const existsObstacles = () => {
    return mergeCheck ? mergeCheck.mergeObstacles.length > 0 : false;
  };

  const isMergeButtonDisabled = () => {
    if (existsObstacles()) {
      if (!existsNotOverrideableObstacles()) {
        return !(pullRequest?._links?.emergencyMerge as Link[]);
      }
    }
    return false;
  };

  const renderButton = () => {
    const checkHints = mergeCheck ? mergeCheck.mergeObstacles.map(o => t(o.key)).join("\n") : "";
    const obstaclesPresent = existsObstacles();
    const obstaclesNotOverrideable = existsNotOverrideableObstacles();
    let color;
    if (mergeCheck?.hasConflicts) {
      color = "warning";
    } else if (obstaclesPresent) {
      if (obstaclesNotOverrideable) {
        color = "warning";
      } else {
        color = "danger";
      }
    } else {
      color = "primary";
    }

    const disabled = isMergeButtonDisabled();

    let action;
    if (mergeCheck?.hasConflicts) {
      action = () => setMergeInformation(true);
    } else if (!disabled && !obstaclesNotOverrideable) {
      if (obstaclesPresent) {
        action = () => setShowOverrideModal(!showOverrideModal);
      } else {
        action = () => setShowMergeModal(!showMergeModal);
      }
    }

    const button = (
      <Button
        label={t("scm-review-plugin.showPullRequest.mergeButton.buttonTitle")}
        loading={loading}
        action={action}
        color={color}
        disabled={disabled || obstaclesNotOverrideable}
        icon={checkHints ? "exclamation-triangle" : ""}
      />
    );
    if (checkHints) {
      return (
        <Tooltip message={checkHints} location={"top"}>
          {button}
        </Tooltip>
      );
    } else {
      return button;
    }
  };

  const addOverrideMessageToMergeCommit = (mergeCommit: MergeCommit) => {
    if (overrideMessage) {
      return {
        ...mergeCommit,
        overrideMessage: overrideMessage
      };
    } else {
      return mergeCommit;
    }
  };

  if (showOverrideModal) {
    return (
      <OverrideModal
        proceed={(message: string) => mergeWithOverride(message)}
        close={() => setShowOverrideModal(false)}
        mergeCheck={mergeCheck}
      />
    );
  }

  if (showMergeModal) {
    return (
      <MergeModal
        merge={(strategy: string, mergeCommit: MergeCommit, emergency: boolean) =>
          merge(strategy, addOverrideMessageToMergeCommit(mergeCommit), emergency)
        }
        close={() => setShowMergeModal(false)}
        pullRequest={pullRequest}
        emergencyMerge={existsObstacles() && !existsNotOverrideableObstacles()}
        mergeCheck={mergeCheck}
      />
    );
  }

  return (
    <>
      {renderButton()}
      <ManualMergeInformation
        showMergeInformation={mergeInformation}
        repository={repository}
        pullRequest={pullRequest}
        onClose={() => setMergeInformation(false)}
      />
    </>
  );
};

export default MergeButton;
