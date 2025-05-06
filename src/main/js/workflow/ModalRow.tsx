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

import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { Result } from "../types/EngineConfig";
import classNames from "classnames";
import { StatusIcon, StatusVariants } from "@scm-manager/ui-core";

type Props = {
  result: Result;
};

const Entry = styled.li`
  // css adjacent with same component
  & + & {
    border-top: 1px solid rgba(219, 219, 219, 0.5);
  }
`;

const Left = styled.div`
  min-width: 50%;
  flex: 1;
`;

const Right = styled.div`
  flex: 1;
`;

function getTranslationKey(result: Result): string {
  let translationKey = "workflow.rule";
  if (result?.rule) {
    translationKey += `.${result.rule}`;
  }
  if (result?.failed) {
    translationKey += ".failed";
  } else {
    translationKey += ".success";
  }
  if (result?.context?.translationCode) {
    translationKey += `.${result?.context?.translationCode}`;
  }
  return translationKey;
}

const ModalRow: FC<Props> = ({ result }) => {
  const [t] = useTranslation("plugins");

  return (
    <Entry
      className={classNames("is-flex", "is-flex-direction-row", "is-justify-content-space-between", "px-0", "py-4")}
    >
      <Left className={"is-flex is-align-items-center"}>
        <span className="has-text-weight-bold">{t("workflow.rule." + result?.rule + ".name")}</span>
        <StatusIcon
          className="ml-2"
          variant={result?.failed ? StatusVariants.DANGER : StatusVariants.SUCCESS}
          alt={t(`scm-review-plugin.pullRequests.aria.workflow.status.${result.failed ? "fail" : "success"}`)}
        />
      </Left>
      <Right>
        <p>{t(getTranslationKey(result), result?.context)}</p>
      </Right>
    </Entry>
  );
};

export default ModalRow;
