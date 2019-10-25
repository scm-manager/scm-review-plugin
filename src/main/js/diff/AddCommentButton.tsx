import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  action: () => void;
};

class AddCommentButton extends React.Component<Props> {
  render() {
    const { action, t } = this.props;
    return (
      <Button action={action} label={t("scm-review-plugin.comment.addComment")} icon="comment" reducedMobile={true} />
    );
  }
}

export default withTranslation("plugins")(AddCommentButton);
