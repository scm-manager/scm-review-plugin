import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { DiffButton } from "@scm-manager/ui-components";

type Props = {
  action: () => void;
};

const AddCommentButton: FC<Props> = ({ action }) => {
  const { t } = useTranslation("plugins");
  return <DiffButton onClick={action} title={t("scm-review-plugin.comment.addComment")} icon="comment" />;
};

export default AddCommentButton;
