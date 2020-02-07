import React, { FC } from "react";
import styled from "styled-components";
import { MentionsInput } from "react-mentions";

const StyledMentionsInput = styled(MentionsInput)`
  min-height: 110px;
  & * {
    border: none;
  }
  & strong {
    color: transparent;
  }
  & > div [class*="suggestions__list"] {
    max-height: 120px;
    overflow: hidden;
  }
`;

type Props = {
  value?: string;
  placeholder?: string;
  autofocus?: boolean;
  children: any;
  onChange: (event: any) => void;
  onSubmit: () => void;
  onCancel?: () => void;
  allowSpaceInQuery: boolean;
};

const MentionTextarea: FC<Props> = ({
  value,
  placeholder,
  children,
  onChange,
  onSubmit,
  onCancel,
  allowSpaceInQuery
}) => {
  return (
    <div className="textarea is-clipped">
      <StyledMentionsInput
        value={value}
        onChange={onChange}
        onSubmit={onSubmit}
        onCancel={onCancel}
        placeholder={placeholder}
        allowSpaceInQuery={allowSpaceInQuery}
      >
        {children}
      </StyledMentionsInput>
    </div>
  );
};

export default MentionTextarea;
