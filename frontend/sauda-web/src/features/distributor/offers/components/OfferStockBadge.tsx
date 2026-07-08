import { Badge } from "../../../../components/ui/Badge";
import type { StockStatus } from "../types";

const config: Record<StockStatus, { label: string; tone: "green" | "orange" | "red" | "blue" | "gray" }> = {
  in_stock: { label: "В наличии", tone: "green" },
  low_stock: { label: "Мало", tone: "orange" },
  out_of_stock: { label: "Нет в наличии", tone: "red" },
  on_order: { label: "Под заказ", tone: "blue" },
  unknown: { label: "Не указано", tone: "gray" },
};

export function OfferStockBadge({ status }: { status: StockStatus | null }) {
  const { label, tone } = config[status ?? "unknown"];
  return <Badge tone={tone}>{label}</Badge>;
}
