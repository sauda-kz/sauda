import { describe, expect, it } from "vitest";
import {
  parsedBoolean,
  parsedNumber,
  parsedString,
  rowToUpdateRequest,
  type ParsedRowResponse,
} from "../features/distributor/imports/types";

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

  it("builds update request from parsed row", () => {
    const row: ParsedRowResponse = {
      id: "row-1",
      importRunId: "run-1",
      sourceRowNumber: 2,
      rawRowData: {},
      parsedData: {
        sku: "SKU-001",
        name: "Item",
        price: 100,
        price_includes_vat: true,
        stock_quantity: 5,
        stock_status: "in_stock",
        lead_time_days: 3,
      },
      status: "valid",
      errors: null,
      warnings: null,
      editedAt: null,
    };

    const request = rowToUpdateRequest(row);
    expect(request.sku).toBe("SKU-001");
    expect(request.name).toBe("Item");
    expect(request.price).toBe(100);
    expect(request.stockStatus).toBe("in_stock");
    expect(request.leadTimeDays).toBe(3);
  });
});
