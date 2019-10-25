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
