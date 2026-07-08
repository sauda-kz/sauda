import { Badge } from "../../../../components/ui/Badge";
import type { ImportStatus } from "../types";

const labels: Record<ImportStatus, string> = {
  pending: "Ожидает",
  processing: "Обработка",
  parsed: "Разобран",
  parsed_with_errors: "С ошибками",
  awaiting_approval: "На проверке",
  approved: "Подтверждён",
  rejected: "Отклонён",
  applied: "Применён",
  failed: "Сбой",
};

const tones: Record<
  ImportStatus,
  "green" | "orange" | "red" | "blue" | "gray"
> = {
  pending: "gray",
  processing: "blue",
  parsed: "blue",
  parsed_with_errors: "orange",
  awaiting_approval: "orange",
  approved: "green",
  rejected: "red",
  applied: "green",
  failed: "red",
};

export function ImportStatusBadge({ status }: { status: ImportStatus }) {
  return <Badge tone={tones[status]}>{labels[status] ?? status}</Badge>;
}
