// @flow

import greet from "./greet";

describe("greeting tests", () => {

  it("should greet trillian", () => {
    const greeting = greet("trillian");
    expect(greeting).toBe("hello trillian");
  });

});
