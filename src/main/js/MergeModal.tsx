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
import React, { FC, useEffect, useRef, useState } from "react";
import { Button, Modal, Notification, SubmitButton } from "@scm-manager/ui-components";
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

const extractFirstMergeStrategy: (mergeStrategyLinks: Link[]) => string = mergeStrategyLinks => {
  if (mergeStrategyLinks && mergeStrategyLinks.length > 0 && mergeStrategyLinks[0].name) {
    return mergeStrategyLinks[0].name;
  } else {
    throw new Error("no merge strategies found");
  }
};

const MergeModal: FC<Props> = ({ pullRequest, emergencyMerge, close, merge, mergeCheck }) => {
  const [t] = useTranslation("plugins");
  const [mergeStrategy, setMergeStrategy] = useState(extractFirstMergeStrategy(pullRequest._links.merge as Link[]));
  const [loading, setLoading] = useState(false);
  const [loadingDefaultMessage, setLoadingDefaultMessage] = useState(false);
  const [messageChanged, setMessageChanged] = useState(false);
  const [mergeFailed, setMergeFailed] = useState(false);
  const [mergeCommit, setMergeCommit] = useState<MergeCommit>({
    commitMessage: "",
    shouldDeleteSourceBranch: false
  });
  const [commitStrategyInfos, setCommitStrategyInfos] = useState<{ [key: string]: MergeStrategyInfo }>({});
  const initialFocusRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    updateMergeStrategyInfo();
  }, []);

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
        <SubmitButton
          icon="exclamation-triangle"
          color="danger"
          label={t("scm-review-plugin.showPullRequest.mergeModal.merge")}
          action={() => performMerge(true)}
          loading={loading}
          disabled={shouldDisableMergeButton()}
        />
      ) : (
        <SubmitButton
          label={t("scm-review-plugin.showPullRequest.mergeModal.merge")}
          action={() => performMerge(false)}
          loading={loading}
          disabled={shouldDisableMergeButton()}
        />
      )}
      <Button label={t("scm-review-plugin.showPullRequest.mergeModal.cancel")} action={() => close()} color="grey" />
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
