import { useState } from "react";
import { Button } from "../../../../components/ui/Button";
import { Modal, ModalActions } from "../../../../components/ui/Modal";
import { TextAreaField } from "../../../../components/ui/Input";
import { DECISION_IMPORT_STATUSES, type ImportRunResponse } from "../types";

interface ImportDecisionBarProps {
  run: ImportRunResponse;
  canApprove: boolean;
  approveLoading: boolean;
  rejectLoading: boolean;
  approveError: string;
  rejectError: string;
  onApprove: () => void;
  onReject: (reason?: string) => void;
}

export function ImportDecisionBar({
  run,
  canApprove,
  approveLoading,
  rejectLoading,
  approveError,
  rejectError,
  onApprove,
  onReject,
}: ImportDecisionBarProps) {
  const [rejectOpen, setRejectOpen] = useState(false);
  const [reason, setReason] = useState("");

  const canDecide = canApprove && DECISION_IMPORT_STATUSES.includes(run.status);
  const isFinalized = ["approved", "applied", "rejected", "failed"].includes(run.status);
  const hasErrors = run.errorRowsCount > 0;
  const allRowsAreErrors =
    run.totalRows > 0 && run.errorRowsCount === run.totalRows;

  if (!canApprove) return null;

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">Решение по импорту</h3>
          {isFinalized ? (
            <p className="mt-1 text-sm text-slate-500">
              Импорт уже завершён со статусом «{run.status}».
            </p>
          ) : canDecide ? (
            <p className="mt-1 text-sm text-slate-500">
              Подтвердите применение валидных строк к прайс-листу или отклоните импорт.
            </p>
          ) : (
            <p className="mt-1 text-sm text-slate-500">
              Решение будет доступно после завершения обработки файла.
            </p>
          )}
          {hasErrors && canDecide && !allRowsAreErrors && (
            <p className="mt-2 text-xs text-amber-700">
              В файле есть строки с ошибками — они не будут применены. Проверьте и исправьте
              критичные строки перед подтверждением.
            </p>
          )}
          {allRowsAreErrors && canDecide && (
            <p className="mt-2 text-xs text-red-700">
              Все строки содержат ошибки — подтверждение недоступно.
            </p>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          <Button
            variant="secondary"
            disabled={!canDecide || rejectLoading || approveLoading}
            loading={rejectLoading}
            onClick={() => setRejectOpen(true)}
          >
            Отклонить
          </Button>
          <Button
            disabled={!canDecide || allRowsAreErrors || approveLoading || rejectLoading}
            loading={approveLoading}
            title={
              allRowsAreErrors
                ? "Нет строк, которые можно применить"
                : hasErrors
                  ? "Строки с ошибками будут пропущены"
                  : undefined
            }
            onClick={onApprove}
          >
            Подтвердить
          </Button>
        </div>
      </div>

      {(approveError || rejectError) && (
        <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {approveError || rejectError}
        </p>
      )}

      <Modal
        open={rejectOpen}
        title="Отклонить импорт"
        onClose={() => setRejectOpen(false)}
        footer={
          <ModalActions
            danger
            confirmLabel="Отклонить"
            loading={rejectLoading}
            onCancel={() => setRejectOpen(false)}
            onConfirm={() => {
              onReject(reason);
              setRejectOpen(false);
              setReason("");
            }}
          />
        }
      >
        <TextAreaField
          label="Причина (необязательно)"
          maxLength={500}
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          placeholder="Например: неверный формат колонок"
        />
      </Modal>
    </div>
  );
}
