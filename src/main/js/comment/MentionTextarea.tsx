import React, { FC } from "react";
import styled from "styled-components";
import { MentionsInput } from "react-mentions";

const StyledMentionsInput = styled(MentionsInput)`
  min-height: 110px;
  & * {
    border: none;
  }
  > div [class*="__control"] {
    background-color: white;
    font-size: 14px;
    font-weight: normal;
    font-family: "monospace";
  }
  > div [class*="__input"] {
    padding: 9px;
    overflow-y: scroll !important;
    min-height: 63px;
    height: 70px;
    outline: 0;
  }
  > div [class*="__highlighter"] {
    overflow: hidden;
    padding: 9px;
  }
  div:nth-child(2) {
    top: 20px !important;
  }
  > div [class*="suggestions__list"] {
    background-color: white;
    width: max-content;
    border: 1px solid rgba(0, 0, 0, 0.15);
    font-size: 14px;
  }
  > div [class*="suggestions__item"] {
    padding: 4px;
    borderbottom: 1px solid rgba(0, 0, 0, 0.15);
    :focus {
      backgroundcolor: #cee4e5;
    }
  }
`;

type Props = {
  value?: string;
  placeholder?: string;
  children: any;
  onChange: (event: any) => void;
  onSubmit: () => void;
  onCancel?: () => void;
};

const MentionTextarea: FC<Props> = ({ value, placeholder, children, onChange, onSubmit, onCancel }) => {
  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (onCancel && event.key === "Escape") {
      onCancel();
      return;
    }

    if (onSubmit && event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
      onSubmit();
    }
  };

  return (
    <div className="field">
      <div className="control">
        <StyledMentionsInput
          className="textarea"
          value={value}
          onKeyDown={onKeyDown}
          onChange={onChange}
          onSubmit={onSubmit}
          onCancel={onCancel}
          placeholder={placeholder}
          allowSpaceInQuery={true}
          allowSuggestionsAboveCursor={true}
        >
          {children}
        </StyledMentionsInput>
      </div>
    </div>
  );
};

export default MentionTextarea;
