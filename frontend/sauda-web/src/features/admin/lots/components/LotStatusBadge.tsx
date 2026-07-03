import type { LotStatus } from "../types";
import { Badge } from "../../../../components/ui/Badge";

const labels: Record<LotStatus, string> = {
  draft: "Черновик",
  active: "Активный",
  needs_review: "Требует проверки",
  expired: "Истёк",
  archived: "Архив",
  cancelled: "Отменён",
  closed: "Закрыт",
};

const tones: Record<
  LotStatus,
  "green" | "orange" | "red" | "blue" | "gray"
> = {
  draft: "gray",
  active: "green",
  needs_review: "orange",
  expired: "red",
  archived: "gray",
  cancelled: "red",
  closed: "blue",
};

export function LotStatusBadge({ status }: { status: LotStatus }) {
  return <Badge tone={tones[status]}>{labels[status]}</Badge>;
}
