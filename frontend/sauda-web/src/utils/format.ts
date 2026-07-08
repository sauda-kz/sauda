/** TypeScript — удобен для чистых функций без JSX. */

export function formatMoney(amount: number | null | undefined, currency = "KZT"): string {
  if (amount == null || Number.isNaN(amount)) return "—";
  const formatted = new Intl.NumberFormat("ru-KZ", {
    maximumFractionDigits: 0,
  }).format(amount);
  return currency === "KZT" ? `${formatted} ₸` : `${formatted} ${currency}`;
}

export function formatPercent(score: number | null | undefined): number | null {
  if (score == null) return null;
  return Math.round(score * 100);
}

export function formatDeadline(iso: string | null | undefined): string {
  if (!iso) return "—";
  const date = new Date(iso);
  const now = new Date();
  const diffMs = date.getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays < 0) return "Истёк";
  if (diffDays === 0) return "Сегодня";
  if (diffDays === 1) return "1 день";
  if (diffDays < 5) return `${diffDays} дня`;
  return `${diffDays} дней`;
}

export function isDeadlineUrgent(iso: string | null | undefined): boolean {
  if (!iso) return false;
  const diffDays = Math.ceil((new Date(iso).getTime() - Date.now()) / (1000 * 60 * 60 * 24));
  return diffDays <= 1;
}

export function formatMarginPercent(margin: number | null, budget: number | null): string | null {
  if (margin == null || budget == null || budget <= 0) return null;
  const pct = (margin / budget) * 100;
  return `≈ ${Math.round(pct)}%`;
}

export function formatQuantity(qty: number | null, unit: string | null): string {
  if (qty == null) return "—";
  return unit ? `${qty} ${unit}` : String(qty);
}

export function formatPriceListUpdatedAt(iso: string | null | undefined): string | null {
  if (!iso) return null;

  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return null;

  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  const datePart = new Intl.DateTimeFormat("ru-RU", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(date);
  const timePart = new Intl.DateTimeFormat("ru-RU", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);

  return isToday ? `Сегодня · ${datePart} · ${timePart}` : `${datePart} · ${timePart}`;
}

const stockStatusLabels: Record<string, string> = {
  in_stock: "В наличии",
  low_stock: "Мало на складе",
  out_of_stock: "Нет в наличии",
  on_order: "Под заказ",
  unknown: "Не указано",
};

export function formatStockStatus(status: string | null | undefined): string {
  if (!status) return "Не указано";
  return stockStatusLabels[status] ?? prettifyCode(status);
}

function pluralRu(n: number, forms: [string, string, string]): string {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return forms[0];
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) return forms[1];
  return forms[2];
}

/** Localise common backend lead-time strings like "3 days" → "3 дня". */
export function formatLeadTime(value: string | null | undefined): string {
  if (!value) return "—";
  const match = value.trim().match(/^(\d+)\s*(day|days|week|weeks|month|months)$/i);
  if (!match) return value;
  const n = Number(match[1]);
  const unit = match[2].toLowerCase();
  if (unit.startsWith("day")) return `${n} ${pluralRu(n, ["день", "дня", "дней"])}`;
  if (unit.startsWith("week")) return `${n} ${pluralRu(n, ["неделя", "недели", "недель"])}`;
  return `${n} ${pluralRu(n, ["месяц", "месяца", "месяцев"])}`;
}

/** Turn a raw enum code like "price_out_of_range" into "Price out of range". */
export function prettifyCode(code: string): string {
  const text = code.replace(/[_-]+/g, " ").trim();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "—";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("ru-RU", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
