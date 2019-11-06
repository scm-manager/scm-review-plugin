import React, { FC } from "react";

type Props = {
  collapsed?: boolean;
};

const Toolbar: FC<Props> = ({ collapsed, children }) => {
  if (collapsed) {
    return null;
  }
  return (
    <div className="media-right">
      <div className="level-right">{collapsed ? null : children}</div>
    </div>
  );
};

export default Toolbar;
