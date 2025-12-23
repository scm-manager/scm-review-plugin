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
    return mergeCheck?.sourceBranchMissing || mergeCheck?.targetBranchMissing;
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
