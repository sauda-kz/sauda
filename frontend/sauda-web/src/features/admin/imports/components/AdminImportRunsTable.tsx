import { Link } from "react-router-dom";
import { ImportStatusBadge } from "../../../distributor/imports/components/ImportStatusBadge";
import { formatDateTime } from "../../../../utils/format";
import type { ImportRunResponse } from "../../../distributor/imports/types";

interface AdminImportRunsTableProps {
  items: ImportRunResponse[];
}

export function AdminImportRunsTable({ items }: AdminImportRunsTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
          <tr>
            <th className="px-4 py-3">Дистрибьютор</th>
            <th className="px-4 py-3">Файл</th>
            <th className="px-4 py-3">Дата</th>
            <th className="px-4 py-3">Статус</th>
            <th className="px-4 py-3 text-right">Строк</th>
            <th className="px-4 py-3 text-right">Ошибок</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {items.map((run) => (
            <tr key={run.id} className="hover:bg-slate-50">
              <td className="px-4 py-3 text-slate-700">
                {run.distributorName ?? run.distributorId.slice(0, 8)}
              </td>
              <td className="px-4 py-3">
                <Link
                  to={`/admin/imports/${run.distributorId}/${run.id}`}
                  className="font-medium text-brand-600 hover:text-brand-700"
                >
                  {run.originalFilename}
                </Link>
              </td>
              <td className="px-4 py-3 text-slate-600">{formatDateTime(run.createdAt)}</td>
              <td className="px-4 py-3">
                <ImportStatusBadge status={run.status} />
              </td>
              <td className="px-4 py-3 text-right text-slate-700">{run.parsedRowsCount}</td>
              <td className="px-4 py-3 text-right">
                <span className={run.errorRowsCount > 0 ? "font-medium text-red-600" : "text-slate-700"}>
                  {run.errorRowsCount}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
