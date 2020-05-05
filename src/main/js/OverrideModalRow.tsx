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
import { useTranslation, withTranslation, WithTranslation } from "react-i18next";
import styled from "styled-components";
import { Result } from "./types/EngineConfig";
import { Icon } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  result: Result;
  useObstacleText: boolean;
};

const Entry = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  padding: 0.3rem 0;
  div {
    background: #f5f5f5;
    display: flex;
    align-items: center;
    padding: 0.5rem;
  }
`;

const Left = styled.div`
  min-width: 40%;
  flex: 1;
`;

const Right = styled.div`
  flex: 2;
`;

const PaddingRightIcon = styled(Icon)`
  padding-right: 0.5rem;
`;

const OverrideModalRow: FC<Props> = ({ result, useObstacleText }) => {
  const [t] = useTranslation("plugins");

  const evaluateTranslationKey = () => {
    if (result.failed) {
      if (useObstacleText) {
        return ".obstacle";
      }
      return ".failed";
    }
    return "success";
  };

  return (
    <Entry>
      <Left>
        <PaddingRightIcon
          color={result?.failed ? "warning" : "success"}
          name={result?.failed ? "exclamation-triangle" : "check-circle"}
        />
        <strong>{t("workflow.rule." + result?.rule + ".name")}</strong>
      </Left>
      <Right>
        <p>{t("workflow.rule." + result?.rule + evaluateTranslationKey())}</p>
      </Right>
    </Entry>
  );
};

export default withTranslation("plugins")(OverrideModalRow);
