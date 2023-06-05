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
import { CardList } from "@scm-manager/ui-layout";
import { MarkdownView, Notification } from "@scm-manager/ui-components";
import { Button, Icon } from "@scm-manager/ui-buttons";
import styled from "styled-components";
import { useTranslation } from "react-i18next";

type Props = {
  value: string[];
  onChange: (newList: string[]) => void;
};

const StyledCardList = styled(CardList)`
  height: min-content;
`;

const CardWrapper = styled.div`
  .empty-state,
  ul:empty {
    display: none !important;
  }
  ul:empty + .empty-state {
    display: block !important;
  }
`;

const NoScrollMarkdownView = styled(MarkdownView)`
  overflow-y: hidden;
`;

const InitialTasks: FC<Props> = ({ value, onChange }) => {
  const [t] = useTranslation("plugins");

  return (
    <div className="field">
      <div className="is-flex is-justify-content-space-between label">
        <label className="" htmlFor="default-tasks-cardlist">
          {t("scm-review-plugin.config.defaultTasks.label")}
        </label>
      </div>
      <CardWrapper className="control">
        <StyledCardList id="default-tasks-cardlist" className="input">
          {value.map((task, index) => (
            <CardList.Card
              className="is-full-width"
              action={
                <Button
                  className="is-borderless has-background-transparent has-hover-color-blue px-2"
                  onClick={() => onChange(value.filter((_, i) => i !== index))}
                >
                  <Icon>trash</Icon>
                </Button>
              }
            >
              <CardList.Card.Row>
                <NoScrollMarkdownView content={task} />
              </CardList.Card.Row>
            </CardList.Card>
          ))}
        </StyledCardList>
        <Notification type="info" className="empty-state">
          {t("scm-review-plugin.config.defaultTasks.noTasks")}
        </Notification>
      </CardWrapper>
    </div>
  );
};

export default InitialTasks;
