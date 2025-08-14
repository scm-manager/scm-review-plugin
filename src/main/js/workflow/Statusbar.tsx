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
import { PullRequest } from "../types/PullRequest";
import { ErrorNotification } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import classNames from "classnames";
import StatusModalView from "./StatusModalView";
import { useStatusbar } from "./useStatusbar";
import { StatusIcon, StatusVariants } from "@scm-manager/ui-core";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const Notification = styled.div`
  margin-top: 1rem;
  margin-bottom: 0 !important;
  padding: 1rem 1.25rem;
  line-height: 1;
  border-top: none !important;
`;

const Statusbar: FC<Props> = ({ repository, pullRequest }) => {
  const [t] = useTranslation("plugins");
  const { data, error, isLoading } = useStatusbar(repository, pullRequest);
  const [modalOpen, setModalOpen] = useState(false);

  // no workflow rules configured
  if (isLoading || !data || data?.results?.length === 0) {
    return null;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  const failedRules = data.results.filter((r) => r.failed).length;
  const failed = failedRules > 0;
  const icon = failed ? StatusVariants.DANGER : StatusVariants.SUCCESS;

  const toggleModal = () => {
    setModalOpen(!modalOpen);
  };

  return (
    <>
      {modalOpen && <StatusModalView onClose={toggleModal} result={data.results} failed={failed} />}
      <Notification
        className={classNames(
          "media",
          "notification is-grey-lighter",
          "has-cursor-pointer",
          "is-flex is-align-items-center",
        )}
        onClick={() => toggleModal()}
      >
        <StatusIcon className="mr-2" variant={icon} sizes="lg" />
        <span>
          <span className="has-text-weight-bold">
            {t("scm-review-plugin.workflow.statusbar.rules", {
              count: data?.results && data.results.length,
            })}
          </span>
          <i className="fas fa-angle-right mx-2 my-0" />
          <span>
            {failedRules === 0
              ? t("scm-review-plugin.workflow.statusbar.noFailedRules")
              : t("scm-review-plugin.workflow.statusbar.failedRules", {
                  count: failedRules,
                })}
          </span>{" "}
        </span>
      </Notification>
    </>
  );
};

export default Statusbar;
