/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import React, { FC } from "react";
import styled from "styled-components";
import { Mention, MentionsInput, SuggestionDataItem } from "react-mentions";
import { mapAutocompleteToSuggestions } from "./mention";
import { BasicComment } from "../types/PullRequest";
import { getUserAutoCompleteLink } from "../index";
import { compose } from "redux";
import { connect } from "react-redux";
import { withTranslation } from "react-i18next";

const StyledSuggestion = styled.div`
  background-color: ${props => props.focused && `#ccecf9`};
  :hover {
    background-color: #ccecf9;
  }
`;

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
  userAutocompleteLink: string;
  comment?: BasicComment;
  onAddMention: (id: string, displayName: string) => void;
  onChange: (event: any) => void;
  onSubmit: () => void;
  onCancel?: () => void;
};

const MentionTextarea: FC<Props> = ({
  value,
  placeholder,
  userAutocompleteLink,
  comment,
  onAddMention,
  onChange,
  onSubmit,
  onCancel
}) => {
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
          <Mention
            markup="@[__id__]"
            displayTransform={(id: string) => {
              return comment?.mentions && comment.mentions.length > 0
                ? `@${comment.mentions?.filter(entry => entry.id === id)[0]?.displayName}`
                : `@${id}`;
            }}
            trigger="@"
            data={(query, callback) => mapAutocompleteToSuggestions(userAutocompleteLink, query, callback)}
            onAdd={onAddMention}
            renderSuggestion={(
              suggestion: SuggestionDataItem,
              search: string,
              highlightedDisplay: React.ReactNode,
              index: number,
              focused: boolean
            ) => (
              <StyledSuggestion className="user" focused={focused} index={index}>
                {highlightedDisplay}
              </StyledSuggestion>
            )}
            style={{
              color: "transparent"
            }}
            appendSpaceOnAdd={true}
          />
        </StyledMentionsInput>
      </div>
    </div>
  );
};

const mapStateToProps = (state: any) => {
  const { indexResources } = state;
  const userAutocompleteLink = getUserAutoCompleteLink(indexResources.links);

  return {
    userAutocompleteLink
  };
};

export default compose(connect(mapStateToProps), withTranslation("plugins"))(MentionTextarea);
