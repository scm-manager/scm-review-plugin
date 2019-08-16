// @flow

import {mapCommentToFile} from "./commentToFileMapper";

describe("commentToFileMapper", () => {
  it('should map context lines', function () {
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
      context
    };

    const mappedContext = mapCommentToFile(comment);

    expect(mappedContext.hunks[0].changes[0].isInsert).toBe(true);
    expect(mappedContext.hunks[0].changes[0].type).toBe("insert");

  });
});
