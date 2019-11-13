import React, { FC } from "react";
import { Icon } from "@scm-manager/ui-components";

type Props = {
  title: string;
  icon: string;
  onClick: () => void;
};

const ToolbarIcon: FC<Props> = ({ title, icon, onClick }) => (
  <a className="level-item" onClick={onClick} title={title}>
    <span className="icon is-small">
      <Icon color="link" name={icon} />
    </span>
  </a>
);

export default ToolbarIcon;
