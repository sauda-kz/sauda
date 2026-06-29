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
