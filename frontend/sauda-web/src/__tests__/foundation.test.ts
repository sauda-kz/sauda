import { describe, expect, it } from "vitest";
import {
  formatDeadline,
  formatMoney,
  formatPercent,
  isDeadlineUrgent,
} from "../utils/format";

describe("format utils", () => {
  it("formats KZT amounts", () => {
    expect(formatMoney(4200000)).toContain("4");
    expect(formatMoney(4200000)).toContain("₸");
  });

  it("converts confidence score to percent", () => {
    expect(formatPercent(0.95)).toBe(95);
    expect(formatPercent(null)).toBeNull();
  });

  it("detects urgent deadlines", () => {
    const tomorrow = new Date(Date.now() + 86400000).toISOString();
    expect(isDeadlineUrgent(tomorrow)).toBe(true);
    expect(formatDeadline(tomorrow)).toBe("1 день");
  });
});
