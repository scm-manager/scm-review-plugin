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

import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button, Icon } from "@scm-manager/ui-core";

type Props = WithTranslation & {
  loading: boolean;
  action: () => void;
};

type State = {};

class UnsubscribeButton extends React.Component<Props, State> {
  render() {
    const { loading, action, t } = this.props;
    return (
      <Button
        isLoading={loading}
        onClick={action}
        className="is-link is-outlined"
        aria-label={t("scm-review-plugin.pullRequest.details.buttons.unsubscribe")}
      >
        <Icon>minus</Icon>
      </Button>
    );
  }
}

export default withTranslation("plugins")(UnsubscribeButton);
