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
import React, { FC, useRef, useState } from "react";
import { PullRequest } from "./types/PullRequest";
import { Button, ErrorNotification, Modal, useDateFormatter } from "@scm-manager/ui-components";
import { useDeleteBranch } from "@scm-manager/ui-api";
import { Branch, Repository } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import { useSourceBranch } from "./pullRequest";
import classNames from "classnames";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  loading: boolean;
};

type ModalProps = {
  repository: Repository;
  branch: Branch;
  sameRevision: boolean;
  close: () => void;
};

const DeleteModal: FC<ModalProps> = ({ repository, branch, sameRevision, close }) => {
  const { remove, isLoading, error } = useDeleteBranch(repository);
  const [t] = useTranslation("plugins");
  const formatter = useDateFormatter({ date: branch.lastCommitDate });
  const initialFocusButton = useRef<HTMLButtonElement>(null);

  const message = t("scm-review-plugin.showPullRequest.deleteSourceBranchButton.deleteModal.message", {
    branch: branch.name
  });

  const footer = (
    <div className="field is-grouped">
      <p className="control">
        <button
          className={classNames("button", sameRevision ? "" : "is-warning", isLoading ? "is-loading" : "")}
          onClick={() => remove(branch)}
          onKeyDown={e => e.key === "Enter" && remove(branch)}
          tabIndex={0}
        >
          {t("scm-review-plugin.showPullRequest.deleteSourceBranchButton.deleteModal.submit")}
        </button>
      </p>
      <p className="control">
        <button
          className={classNames("button", "is-info")}
          onClick={close}
          onKeyDown={e => e.key === "Enter" && close()}
          tabIndex={0}
          ref={initialFocusButton}
        >
          {t("scm-review-plugin.showPullRequest.deleteSourceBranchButton.deleteModal.cancel")}
        </button>
      </p>
    </div>
  );

  const warning = sameRevision ? null : (
    <p>
      {t("scm-review-plugin.showPullRequest.deleteSourceBranchButton.deleteModal.messageForDifferentRevision", {
        branch: branch.name,
        lastCommitDate: formatter?.formatDistance()
      })}
    </p>
  );

  return (
    <Modal
      title={t("scm-review-plugin.showPullRequest.deleteSourceBranchButton.deleteModal.title")}
      closeFunction={() => {
        close();
      }}
      body={
        <>
          {warning}
          <p>{message}</p>
          {error && <ErrorNotification error={error} />}
        </>
      }
      active={true}
      footer={footer}
      initialFocusRef={initialFocusButton}
      headColor={sameRevision ? "" : "warning"}
    />
  );
};

const DeleteSourceBranchButton: FC<Props> = ({ pullRequest, repository, loading }) => {
  const [showModal, setShowModal] = useState(false);
  const [t] = useTranslation("plugins");
  const { sourceBranch, isLoading: sourceBranchLoading } = useSourceBranch(repository, pullRequest);

  if (sourceBranchLoading || !sourceBranch || sourceBranch.defaultBranch) {
    return null;
  }

  const sameRevision = pullRequest.sourceRevision === sourceBranch.revision;

  return (
    <>
      {showModal && (
        <DeleteModal
          repository={repository}
          branch={sourceBranch}
          close={() => setShowModal(false)}
          sameRevision={sameRevision}
        />
      )}
      <Button
        label={t("scm-review-plugin.showPullRequest.deleteSourceBranchButton.buttonTitle")}
        icon={sameRevision ? "" : "exclamation-triangle"}
        loading={loading}
        action={() => setShowModal(true)}
      />
    </>
  );
};

export default DeleteSourceBranchButton;
