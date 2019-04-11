// @flow
import { isUrlSuffixMatching } from "./PullRequestInformation";

describe("test isUrlSuffixMatching", () => {
  it("should return true", () => {
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix", "suffix")).toBe(
      true
    );
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix/1", "suffix")).toBe(
      true
    );
    expect(
      isUrlSuffixMatching("/my/base", "my/base/suffix/1/2", "suffix")
    ).toBe(true);
  });

  it("should return false", () => {
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix", "other")).toBe(
      false
    );
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix/1", "other")).toBe(
      false
    );
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix/1/2", "other")).toBe(
      false
    );
  });
});
