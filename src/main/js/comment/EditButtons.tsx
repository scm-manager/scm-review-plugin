import React, { FC } from "react";
import { SubmitButton, Button, confirmAlert } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { Comment } from "../types/PullRequest";

type Props = {
  comment: Comment;
  onSubmit: () => void;
  onCancel: () => void;
};

const EditButtons: FC<Props> = ({ comment, onSubmit, onCancel }) => {
  const { t } = useTranslation("plugins");

  const confirmCancelUpdate = () => {
    confirmAlert({
      title: t("scm-review-plugin.comment.confirmCancelUpdateAlert.title"),
      message: t("scm-review-plugin.comment.confirmCancelUpdateAlert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.comment.confirmCancelUpdateAlert.submit"),
          onClick: onCancel
        },
        {
          label: t("scm-review-plugin.comment.confirmCancelUpdateAlert.cancel"),
          onClick: () => null
        }
      ]
    });
  };

  return (
    <div className="level-left">
      <div className="level-item">
        <SubmitButton
          label={t("scm-review-plugin.comment.save")}
          action={onSubmit}
          disabled={comment.comment.trim() === ""}
          scrollToTop={false}
        />
      </div>
      <div className="level-item">
        <Button label={t("scm-review-plugin.comment.cancel")} color="warning" action={confirmCancelUpdate} />
      </div>
    </div>
  );
};

export default EditButtons;
