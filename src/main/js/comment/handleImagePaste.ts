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
