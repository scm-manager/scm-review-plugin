///
/// MIT License
///
/// Copyright (c) 2020-present Cloudogu GmbH and Contributors
///
/// Permission is hereby granted, free of charge, to any person obtaining a copy
/// of this software and associated documentation files (the "Software"), to deal
/// in the Software without restriction, including without limitation the rights
/// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
/// copies of the Software, and to permit persons to whom the Software is
/// furnished to do so, subject to the following conditions:
///
/// The above copyright notice and this permission notice shall be included in all
/// copies or substantial portions of the Software.
///
/// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
/// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
/// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
/// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
/// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
/// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
/// SOFTWARE.
///

import { mapCommentToFile } from "./commentToFileMapper";

describe("commentToFileMapper", () => {
  it("should map context lines", function() {
    const context = {
      lines: [
        {
          oldLineNumber: null,
          newLineNumber: 3,
          content: "added line"
        },
        {
          oldLineNumber: 2,
          newLineNumber: null,
          content: "deleted line"
        },
        {
          oldLineNumber: 10,
          newLineNumber: 11,
          content: "unchanged line"
        }
      ]
    };

    const comment = {
      context,
      location: {
        file: "file"
      }
    };

    const mappedContext = mapCommentToFile(comment);

    const changes = mappedContext.hunks[0].changes;
    expect(changes[0].isInsert).toBe(true);
    expect(changes[0].type).toBe("insert");
    expect(changes[0].lineNumber).toBe(3);

    expect(changes[1].isDelete).toBe(true);
    expect(changes[1].type).toBe("delete");
    expect(changes[1].lineNumber).toBe(2);

    expect(changes[2].isNormal).toBe(true);
    expect(changes[2].type).toBe("normal");
    expect(changes[2].oldLineNumber).toBe(10);
    expect(changes[2].newLineNumber).toBe(11);

    expect(mappedContext.newPath).toBe("file");
  });
});
