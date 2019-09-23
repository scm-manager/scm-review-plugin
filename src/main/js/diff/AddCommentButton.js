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
      <Button
        action={action}
        label={t("scm-review-plugin.comment.addComment")}
        icon="comment"
        reducedMobile={true}
      />
    );
  }
}

export default translate("plugins")(AddCommentButton);
