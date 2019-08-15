// @flow
import React, {type StatelessFunctionalComponent} from "react";
import {translate, type TFunction} from "react-i18next";
import classNames from "classnames";

type Props = {
  icon: string,
  label: string,
  title?: string,
  color?: string,

  // context props
  t: TFunction
};

const Tag: StatelessFunctionalComponent<Props> = ({icon, label, title, color}) => {
  let classes = "tag is-rounded";
  if (color) {
    classes += ` is-${color}`;
  }
  return (
    <span className={classes} title={title}>
      <span className={classNames("fas", "fa-" + icon)}>&nbsp;</span> {label}
    </span>
  );
};

type TranslatedProps = Props & {
  // context props
  t: TFunction
};

const TranslatedTag = translate("plugins")(({label, title, t, ...restProps}: TranslatedProps) => {
  let translatedTitle;
  if (title) {
    const i18nKey = `scm-review-plugin.comment.tag.${title}`;
    translatedTitle = t(i18nKey);
    if (i18nKey === translatedTitle) {
      translatedTitle = title;
    }
  }
  // $FlowFixMe Tag requires prop t, but t is injected by translate ...
  return <Tag label={t(`scm-review-plugin.comment.tag.${label}`)} title={translatedTitle} {...restProps} />;
});

export const SystemTag = () => <TranslatedTag icon="bolt" label="system"/>;
export const OutdatedTag = () => <TranslatedTag icon="clock" label="outdated.label" title="outdated.title"/>;
export const TaskTodoTag = () => <TranslatedTag icon="check-circle" label="task" color="warning" />;

type TitleOnlyProps = {
  title?: string
};

export const TaskDoneTag = ({title}: TitleOnlyProps) =>
  <TranslatedTag icon="check-circle" label="done" color="success" title={title} />;

type FileProps = {
  path: string
};

export const FileTag = ({path}: FileProps) => {
  const file = path.replace(/^.+\//, "");
  // $FlowFixMe Tag requires prop t, but t is injected by translate ...
  return <Tag icon="code" title={path} label={file} />;
};
