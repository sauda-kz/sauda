import { describe, expect, it } from "vitest";
import { parsedBoolean, parsedNumber, parsedString } from "../features/distributor/imports/types";

describe("import parsed field helpers", () => {
  it("reads string and number fields from parsed data", () => {
    const data = {
      sku: " SKU-1 ",
      price: 100.5,
      stock_quantity: "12",
    };

    expect(parsedString(data, "sku")).toBe("SKU-1");
    expect(parsedNumber(data, "price")).toBe(100.5);
    expect(parsedNumber(data, "stock_quantity")).toBe(12);
  });

  it("reads boolean fields from parsed data", () => {
    expect(parsedBoolean({ price_includes_vat: true }, "price_includes_vat")).toBe(true);
    expect(parsedBoolean({ price_includes_vat: "false" }, "price_includes_vat")).toBe(false);
    expect(parsedBoolean({}, "price_includes_vat")).toBeNull();
  });
});
