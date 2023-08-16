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

import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { Result } from "../types/EngineConfig";
import { Icon } from "@scm-manager/ui-components";
import classNames from "classnames";

type Props = {
  result: Result;
};

const Entry = styled.div`
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
      <Left>
        <span className="has-text-weight-bold">{t("workflow.rule." + result?.rule + ".name")}</span>
        <Icon
          className="ml-1"
          color={result?.failed ? "danger" : "success"}
          name={result?.failed ? "times-circle" : "check-circle"}
          aria-label={t(`scm-review-plugin.pullRequests.aria.workflow.status.${result.failed ? "fail" : "success"}`)}
        />
      </Left>
      <Right>
        <p>{t(getTranslationKey(result), result?.context)}</p>
      </Right>
    </Entry>
  );
};

export default ModalRow;
