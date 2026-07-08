import { AlertTriangle, EyeOff, Star } from "lucide-react";
import type { LotMatchStatus } from "../../../../types/api";
import { Button } from "../../../../components/ui/Button";

interface LotMatchStatusActionsProps {
  canManage: boolean;
  loading: boolean;
  onStatusChange: (status: LotMatchStatus) => void;
}

export function LotMatchStatusActions({
  canManage,
  loading,
  onStatusChange,
}: LotMatchStatusActionsProps) {
  if (!canManage) {
    return (
      <p className="text-sm text-slate-500">
        У вас доступ только для просмотра. Изменение статуса доступно менеджеру.
      </p>
    );
  }

  return (
    <div className="flex flex-wrap items-center gap-3">
      <Button loading={loading} onClick={() => onStatusChange("interested")}>
        <Star className="h-4 w-4" />
        Интересно
      </Button>
      <Button
        variant="secondary"
        loading={loading}
        onClick={() => onStatusChange("dismissed")}
      >
        <EyeOff className="h-4 w-4" />
        Скрыть
      </Button>
      <Button
        variant="ghost"
        loading={loading}
        className="text-amber-700"
        onClick={() => onStatusChange("mismatch_reported")}
      >
        <AlertTriangle className="h-4 w-4" />
        Сообщить о несоответствии
      </Button>
    </div>
  );
}
