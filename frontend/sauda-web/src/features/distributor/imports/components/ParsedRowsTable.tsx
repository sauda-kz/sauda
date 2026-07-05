import { Pencil } from "lucide-react";
import { Badge } from "../../../../components/ui/Badge";
import { formatMoney } from "../../../../utils/format";
import { RowErrorsPopover } from "./RowErrorsPopover";
import {
  PARSED_ROW_STATUS_LABELS,
  STOCK_STATUS_LABELS,
  parsedBoolean,
  parsedNumber,
  parsedString,
  type ParsedRowResponse,
  type ParsedRowStatus,
} from "../types";

interface ParsedRowsTableProps {
  items: ParsedRowResponse[];
  readOnly?: boolean;
  onEdit?: (row: ParsedRowResponse) => void;
  statusFilter: ParsedRowStatus | "";
  onStatusFilterChange: (value: ParsedRowStatus | "") => void;
}

const rowStatusTone: Record<
  ParsedRowStatus,
  "green" | "orange" | "red" | "blue"
> = {
  valid: "green",
  needs_review: "orange",
  error: "red",
  edited: "blue",
};

export function ParsedRowsTable({
  items,
  readOnly = false,
  onEdit,
  statusFilter,
  onStatusFilterChange,
}: ParsedRowsTableProps) {
  const showEdit = !readOnly && onEdit != null;
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <label className="text-sm font-medium text-slate-700">
          Фильтр:
          <select
            className="ml-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm"
            value={statusFilter}
            onChange={(event) =>
              onStatusFilterChange(event.target.value as ParsedRowStatus | "")
            }
          >
            <option value="">Все статусы</option>
            {(Object.keys(PARSED_ROW_STATUS_LABELS) as ParsedRowStatus[]).map((status) => (
              <option key={status} value={status}>
                {PARSED_ROW_STATUS_LABELS[status]}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="min-w-full divide-y divide-slate-200 text-sm">
          <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-3 py-3">#</th>
              <th className="px-3 py-3">SKU</th>
              <th className="px-3 py-3">Название</th>
              <th className="px-3 py-3">Цена</th>
              <th className="px-3 py-3">НДС</th>
              <th className="px-3 py-3">Остаток</th>
              <th className="px-3 py-3">Статус</th>
              <th className="px-3 py-3">Замечания</th>
              {showEdit && <th className="px-3 py-3" />}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {items.map((row) => {
              const data = row.parsedData;
              const isError = row.status === "error";
              return (
                <tr
                  key={row.id}
                  className={isError ? "bg-red-50/60" : "hover:bg-slate-50"}
                >
                  <td className="px-3 py-2.5 text-slate-500">{row.sourceRowNumber ?? "—"}</td>
                  <td className="px-3 py-2.5 font-mono text-xs">{parsedString(data, "sku") || "—"}</td>
                  <td className="px-3 py-2.5 max-w-[200px] truncate">
                    {parsedString(data, "name") || "—"}
                  </td>
                  <td className="px-3 py-2.5 whitespace-nowrap">
                    {formatMoney(parsedNumber(data, "price"))}
                  </td>
                  <td className="px-3 py-2.5">
                    {parsedBoolean(data, "price_includes_vat") == null
                      ? "—"
                      : parsedBoolean(data, "price_includes_vat")
                        ? "Да"
                        : "Нет"}
                  </td>
                  <td className="px-3 py-2.5 whitespace-nowrap">
                    {parsedNumber(data, "stock_quantity") ?? "—"}
                    {parsedString(data, "stock_status") && (
                      <span className="ml-1 text-xs text-slate-500">
                        (
                        {STOCK_STATUS_LABELS[
                          parsedString(data, "stock_status") as keyof typeof STOCK_STATUS_LABELS
                        ] ?? parsedString(data, "stock_status")}
                        )
                      </span>
                    )}
                  </td>
                  <td className="px-3 py-2.5">
                    <Badge tone={rowStatusTone[row.status]}>
                      {PARSED_ROW_STATUS_LABELS[row.status]}
                    </Badge>
                  </td>
                  <td className="px-3 py-2.5">
                    <RowErrorsPopover errors={row.errors} warnings={row.warnings} />
                  </td>
                  {showEdit && (
                    <td className="px-3 py-2.5">
                      <button
                        type="button"
                        onClick={() => onEdit(row)}
                        className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-brand-600"
                        aria-label="Редактировать строку"
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
