import React, { ReactNode } from "react";
import styled from "styled-components";

const Group = styled.span`
  & > * {
    margin-right: 0.5rem;
  }
`;

type Props = {
  children?: ReactNode;
};

class TagGroup extends React.Component<Props> {
  render() {
    const { children } = this.props;
    return <Group>{children}</Group>;
  }
}

export default TagGroup;
