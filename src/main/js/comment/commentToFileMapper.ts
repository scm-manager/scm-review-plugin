import { File } from "@scm-manager/ui-components";
import { Comment, ContextLine } from "../types/PullRequest";

export function mapCommentToFile(comment: Comment): File {
  const changes = comment.context.lines.map(line => mapContextLine(line));

  return {
    hunks: [
      {
        changes
      }
    ],
    newPath: comment.location.file
  };
}

function mapContextLine(contextLine: ContextLine) {
  return {
    content: contextLine.content,
    isInsert: !!contextLine.newLineNumber && !contextLine.oldLineNumber,
    isDelete: !contextLine.newLineNumber && !!contextLine.oldLineNumber,
    isNormal: !!contextLine.newLineNumber && !!contextLine.oldLineNumber,
    lineNumber: determineLineNumber(contextLine),
    newLineNumber: contextLine.newLineNumber,
    oldLineNumber: contextLine.oldLineNumber,
    type: determineType(contextLine)
  };
}

function determineType(contextLine: ContextLine) {
  if (contextLine.newLineNumber) {
    if (contextLine.oldLineNumber) {
      return "normal";
    } else {
      return "insert";
    }
  } else {
    return "delete";
  }
}

function determineLineNumber(contextLine: ContextLine) {
  if (contextLine.newLineNumber) {
    return contextLine.newLineNumber;
  } else {
    return contextLine.oldLineNumber;
  }
}
