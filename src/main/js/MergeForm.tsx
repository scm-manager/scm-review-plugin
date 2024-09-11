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

import React, { ChangeEvent, FC } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "@scm-manager/ui-types";
import { Button, CommitAuthor, Textarea } from "@scm-manager/ui-components";
import { Checkbox } from "@scm-manager/ui-forms";
import MergeStrategies from "./MergeStrategies";

type Props = {
  strategyLinks: Link[];
  selectStrategy: (strategy: string) => void;
  selectedStrategy: string;
  commitMessage: string;
  commitMessageDisabled?: boolean;
  commitMessageHint?: string;
  commitAuthor?: string;
  onChangeCommitMessage: (message: string) => void;
  onResetCommitMessage: () => void;
  shouldDeleteSourceBranch: boolean;
  onChangeDeleteSourceBranch: (value: boolean) => void;
  loading: boolean;
  onSubmit: () => void;
};

type InnerProps = Props & {
  innerRef: React.Ref<HTMLInputElement>;
};

const MergeForm: FC<InnerProps> = ({
  strategyLinks,
  selectedStrategy,
  selectStrategy,
  commitMessage,
  onChangeCommitMessage,
  shouldDeleteSourceBranch,
  onChangeDeleteSourceBranch,
  onResetCommitMessage,
  commitMessageHint,
  commitAuthor,
  commitMessageDisabled,
  loading,
  onSubmit,
  innerRef
}) => {
  const [t] = useTranslation("plugins");

  const commitMessageElement = !commitMessageDisabled ? (
    <>
      <Textarea
        placeholder={t("scm-review-plugin.showPullRequest.mergeModal.commitMessage")}
        disabled={loading}
        value={commitMessage}
        onChange={onChangeCommitMessage}
        onSubmit={onSubmit}
      />
      {commitMessageHint ? (
        <div className="is-size-7 mb-4">
          <span className="icon is-small has-text-info">
            <i className="fas fa-info-circle" />
          </span>{" "}
          <span>{t("scm-review-plugin.showPullRequest.mergeModal.commitMessageHint." + commitMessageHint)}</span>
        </div>
      ) : null}
      <div className="mb-2">
        {commitAuthor ? (
          <span className="mb-2">
            <strong>{t("scm-review-plugin.showPullRequest.mergeModal.commitAuthor")}</strong> {commitAuthor}
          </span>
        ) : (
          <CommitAuthor />
        )}
      </div>
      <Button label={t("scm-review-plugin.showPullRequest.mergeModal.resetMessage")} action={onResetCommitMessage} />
      <hr />
    </>
  ) : null;

  return (
    <>
      <MergeStrategies
        strategyLinks={strategyLinks}
        selectedStrategy={selectedStrategy}
        selectStrategy={selectStrategy}
        ref={innerRef}
      />
      <hr />
      {commitMessageElement}
      <Checkbox
        label={t("scm-review-plugin.showPullRequest.mergeModal.deleteSourceBranch.flag")}
        checked={shouldDeleteSourceBranch}
        helpText={t("scm-review-plugin.showPullRequest.mergeModal.deleteSourceBranch.help")}
        onChange={(event: ChangeEvent<HTMLInputElement>) => onChangeDeleteSourceBranch(event.target.checked)}
        disabled={loading}
      />
    </>
  );
};

export default React.forwardRef<HTMLInputElement, Props>((props, ref) => <MergeForm {...props} innerRef={ref} />);
