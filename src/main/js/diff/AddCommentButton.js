//@flow
import React from "react";
import { translate, type TFunction } from "react-i18next";
import { Button } from "@scm-manager/ui-components";

type Props = {
  action: () => void,
  // context props
  t: TFunction
};

class AddCommentButton extends React.Component<Props> {
  render() {
    const { action, t } = this.props;
    return (
      <Button action={action} className="reduced-mobile">
        <span className="icon">
          <i className="fas fa-comment" />
        </span>
        <span>{t("scm-review-plugin.comment.add")}</span>
      </Button>
    );
  }
}

export default translate("plugins")(AddCommentButton);
