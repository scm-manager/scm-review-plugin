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

import { CommentImage } from "../types/PullRequest";
import React, { ClipboardEvent } from "react";

const allowedFileFormats = ["image/png", "image/gif", "image/jpeg"];

const handleImagePaste = (
  url: string,
  setImages: (value: React.SetStateAction<CommentImage[]>) => void,
  setCommentText: (value: React.SetStateAction<string>) => void
) => {
  return async (pasteEvent: ClipboardEvent<HTMLTextAreaElement>) => {
    if (pasteEvent.clipboardData.items.length === 0) {
      return;
    }

    const clipboardItem = pasteEvent.clipboardData.items[0];
    if (clipboardItem.kind !== "file" || !allowedFileFormats.includes(clipboardItem.type)) {
      return;
    }
    const filetype = clipboardItem.type;

    const imageFile = clipboardItem.getAsFile();
    if (!imageFile) {
      return;
    }

    const selectedStart = pasteEvent.currentTarget.selectionStart;
    const selectedEnd = pasteEvent.currentTarget.selectionEnd;
    pasteEvent.preventDefault();

    const hashBuffer = await crypto.subtle.digest("SHA-256", await imageFile.arrayBuffer());
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const hashHex = hashArray.map(byte => byte.toString(16).padStart(2, "0")).join("");

    setImages(prevState =>
      prevState.find(commentImage => commentImage.fileHash === hashHex)
        ? prevState
        : [...prevState, { fileHash: hashHex, file: imageFile, filetype: filetype }]
    );

    const imageUrl = `${new URL(url).pathname}/${hashHex}`;
    setCommentText(prevState => `${prevState.slice(0, selectedStart)}![](${imageUrl})${prevState.slice(selectedEnd)}`);
  };
};

export default handleImagePaste;
