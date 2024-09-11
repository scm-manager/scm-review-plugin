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
import { WithTranslation, withTranslation } from "react-i18next";
import classNames from "classnames";
import { Tag } from "@scm-manager/ui-components";

type Props = {
  icon: string;
  label: string;
  title?: string;
  color?: string;
  onClick?: () => void;
};

const CustomTag: FC<Props> = ({ icon, label, title, color, onClick }) => {
  return (
    <Tag
      className={classNames("is-rounded", onClick ? "has-cursor-pointer" : "")}
      color={color}
      icon={onClick ? icon + " has-text-link" : icon}
      onClick={onClick}
      label={label}
      title={title}
    />
  );
};

type TranslatedProps = WithTranslation & Props;

const TranslatedTag = withTranslation("plugins")(({ label, title, t, ...restProps }: TranslatedProps) => {
  let translatedTitle;
  if (title) {
    const i18nKey = `scm-review-plugin.comment.tag.${title}`;
    translatedTitle = t(i18nKey);
    if (i18nKey === translatedTitle) {
      translatedTitle = title;
    }
  }
  // $FlowFixMe Tag requires prop t, but t is injected by translate ...
  return <CustomTag label={t(`scm-review-plugin.comment.tag.${label}`)} title={translatedTitle} {...restProps} />;
});

type ClickableTagProps = {
  onClick?: () => void;
};

export const EmergencyMergeTag = () => (
  <TranslatedTag icon="exclamation-triangle" label="emergencyMerged" color="danger" />
);

export const SystemTag = () => <TranslatedTag icon="bolt" label="system" />;
export const OutdatedTag = ({ onClick }: ClickableTagProps) => (
  <TranslatedTag
    icon="clock"
    label="outdated.label"
    title={onClick ? "outdated.titleClickable" : "outdated.title"}
    onClick={onClick}
  />
);
export const TaskTodoTag = () => <TranslatedTag icon="check-circle" label="task" color="warning" />;

type TitleOnlyProps = {
  title?: string;
};

export const TaskDoneTag = ({ title }: TitleOnlyProps) => (
  <TranslatedTag icon="check-circle" label="done" color="success is-outlined" title={title} />
);

type FileProps = ClickableTagProps & {
  path: string;
};

export const FileTag = ({ path, onClick }: FileProps) => {
  const file = path.replace(/^.+\//, "");
  // $FlowFixMe Tag requires prop t, but t is injected by translate ...
  return <CustomTag icon="code" title={path} label={file} onClick={onClick} />;
};
