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
