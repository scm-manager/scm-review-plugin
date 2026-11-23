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

import React, { FC, useEffect, useRef, useState } from "react";
import { Notification, Button, Icon } from "@scm-manager/ui-core";
import { Modal } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import MergeForm from "./MergeForm";
import { MergeCheck, MergeCommit, PullRequest } from "./types/PullRequest";
import { Link } from "@scm-manager/ui-types";
import { MergeStrategyInfo } from "./types/MergeStrategyInfo";
import { getMergeStrategyInfo } from "./pullRequest";

type Props = {
  close: () => void;
  merge: (strategy: string, mergeCommit: MergeCommit, emergency: boolean) => void;
  pullRequest: PullRequest;
  emergencyMerge: boolean;
  mergeCheck?: MergeCheck;
};

type DefaultConfig = {
  mergeStrategy: string;
  deleteBranchOnMerge: boolean;
};

const extractFirstMergeStrategy: (mergeStrategyLinks: Link[]) => string = mergeStrategyLinks => {
  if (mergeStrategyLinks && mergeStrategyLinks.length > 0 && mergeStrategyLinks[0].name) {
    return mergeStrategyLinks[0].name;
  } else {
    throw new Error("no merge strategies found");
  }
};

const MergeModal: FC<Props> = ({ pullRequest, emergencyMerge, close, merge, mergeCheck }) => {
  const [t] = useTranslation("plugins");
  const [mergeStrategy, setMergeStrategy] = useState(
    (pullRequest._embedded?.defaultConfig as DefaultConfig).mergeStrategy ||
      extractFirstMergeStrategy(pullRequest._links.merge as Link[])
  );
  const [loading, setLoading] = useState(false);
  const [loadingDefaultMessage, setLoadingDefaultMessage] = useState(false);
  const [messageChanged, setMessageChanged] = useState(false);
  const [mergeFailed, setMergeFailed] = useState(false);
  const [mergeCommit, setMergeCommit] = useState<MergeCommit>({
    commitMessage: "",
    shouldDeleteSourceBranch: pullRequest.shouldDeleteSourceBranch
  });
  const [commitStrategyInfos, setCommitStrategyInfos] = useState<{ [key: string]: MergeStrategyInfo }>({});
  const initialFocusRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!messageChanged) {
      updateMergeStrategyInfo();
    }
  }, [mergeStrategy]);

  useEffect(() => {
    if (mergeCheck?.hasConflicts) {
      setMergeFailed(true);
      setLoading(false);
    } else {
      setMergeFailed(false);
    }
  }, [mergeCheck]);

  const updateMergeStrategyInfo = () => {
    if (mergeStrategy in commitStrategyInfos) {
      setMergeCommit({ ...mergeCommit, commitMessage: commitStrategyInfos[mergeStrategy].defaultCommitMessage });
      setMessageChanged(false);
    } else if (pullRequest && pullRequest._links && pullRequest._links.defaultCommitMessage) {
      setLoadingDefaultMessage(true);
      getMergeStrategyInfo((pullRequest._links.mergeStrategyInfo as Link).href + "?strategy=" + mergeStrategy)
        .then((commitStrategyInfo: MergeStrategyInfo) => {
          commitStrategyInfos[mergeStrategy] = commitStrategyInfo;
          setCommitStrategyInfos(commitStrategyInfos);
          setMergeCommit({ ...mergeCommit, commitMessage: commitStrategyInfo.defaultCommitMessage });
          setMessageChanged(false);
          setLoadingDefaultMessage(false);
        })
        .catch(() => {
          setMergeCommit({ ...mergeCommit, commitMessage: "" });
          setMessageChanged(false);
          setLoadingDefaultMessage(false);
        });
    }
  };

  const selectStrategy = (strategy: string) => {
    setMergeStrategy(strategy);
    setMergeFailed(false);
  };

  const onChangeCommitMessage = (newMessage: string) => {
    setMessageChanged(true);
    setMergeCommit({ ...mergeCommit, commitMessage: newMessage });
  };

  const performMerge = (emergency: boolean) => {
    setLoading(true);
    merge(mergeStrategy, mergeCommit, emergency);
  };

  const shouldDisableMergeButton = () =>
    !mergeCommit.commitMessage || mergeCommit.commitMessage.trim() === "" || mergeFailed;

  const onChangeDeleteSourceBranch = (value: boolean) =>
    setMergeCommit({ ...mergeCommit, shouldDeleteSourceBranch: value });

  const footer = (
    <>
      {emergencyMerge ? (
        <Button
          type="submit"
          variant="danger"
          onClick={() => {
            performMerge(true);
            window.scrollTo(0, 0);
          }}
          isLoading={loading}
          disabled={shouldDisableMergeButton()}
        >
          <Icon>exclamation-triangle</Icon>
          <span>{t("scm-review-plugin.showPullRequest.mergeModal.merge")}</span>
        </Button>
      ) : (
        <Button
          type="submit"
          variant="primary"
          onClick={() => {
            performMerge(false);
            window.scrollTo(0, 0);
          }}
          isLoading={loading}
          disabled={shouldDisableMergeButton()}
        >
          {t("scm-review-plugin.showPullRequest.mergeModal.merge")}
        </Button>
      )}
      <Button onClick={() => close()}>{t("scm-review-plugin.showPullRequest.mergeModal.cancel")}</Button>
    </>
  );

  const body = (
    <>
      <MergeForm
        selectedStrategy={mergeStrategy}
        selectStrategy={selectStrategy}
        strategyLinks={pullRequest._links.merge as Link[]}
        commitMessage={mergeCommit.commitMessage}
        onChangeCommitMessage={onChangeCommitMessage}
        onResetCommitMessage={updateMergeStrategyInfo}
        shouldDeleteSourceBranch={mergeCommit.shouldDeleteSourceBranch}
        onChangeDeleteSourceBranch={onChangeDeleteSourceBranch}
        loading={loadingDefaultMessage}
        commitMessageDisabled={commitStrategyInfos[mergeStrategy]?.commitMessageDisabled}
        commitMessageHint={commitStrategyInfos[mergeStrategy]?.commitMessageHint}
        commitAuthor={commitStrategyInfos[mergeStrategy]?.commitAuthor}
        onSubmit={() => !shouldDisableMergeButton() && performMerge(emergencyMerge)}
        ref={initialFocusRef}
      />
      {mergeFailed && (
        <>
          <hr />
          <Notification type="danger">{t("scm-review-plugin.showPullRequest.mergeModal.mergeConflict")}</Notification>
        </>
      )}
    </>
  );

  return (
    <Modal
      title={
        emergencyMerge
          ? t("scm-review-plugin.showPullRequest.mergeModal.title.emergencyMerge")
          : t("scm-review-plugin.showPullRequest.mergeModal.title.merge")
      }
      active={true}
      body={body}
      closeFunction={close}
      footer={footer}
      initialFocusRef={initialFocusRef}
    />
  );
};

export default MergeModal;
