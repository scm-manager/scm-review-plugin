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
