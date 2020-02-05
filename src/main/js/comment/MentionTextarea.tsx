import React, { FC } from "react";
import styled from "styled-components";
import { MentionsInput } from "react-mentions";

const StyledMentionsInput = styled(MentionsInput)`
  border: none;
  & * {
    border: none;
  }
  min-height: 100px;
  & strong {
    color: transparent;
  }
`;

type Props = {
  value?: string;
  placeholder?: string;
  autofocus?: boolean;
  children: any;
  onChange: (event: any) => void;
  onSubmit: () => void;
  onCancel: () => void;
};

const MentionTextarea: FC<Props> = ({ value, placeholder, children, onChange, onSubmit, onCancel }) => {
  return (
    <div className="field control">
      <div className="textarea">
        <StyledMentionsInput
          value={value}
          onChange={onChange}
          onSubmit={onSubmit}
          onCancel={onCancel}
          placeholder={placeholder}
        >
          {children}
        </StyledMentionsInput>
      </div>
    </div>
  );
};

export default MentionTextarea;
