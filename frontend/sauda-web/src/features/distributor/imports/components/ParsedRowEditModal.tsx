import { useEffect, useState } from "react";
import { InputField, SelectField } from "../../../../components/ui/Input";
import { Modal, ModalActions } from "../../../../components/ui/Modal";
import {
  STOCK_STATUS_LABELS,
  rowToUpdateRequest,
  type ParsedRowResponse,
  type StockStatus,
  type UpdateParsedRowRequest,
} from "../types";

interface ParsedRowEditModalProps {
  row: ParsedRowResponse | null;
  open: boolean;
  loading: boolean;
  error: string;
  onClose: () => void;
  onSave: (rowId: string, payload: UpdateParsedRowRequest) => void;
}

function validateForm(payload: UpdateParsedRowRequest): string | null {
  if (!payload.sku.trim()) return "SKU обязателен";
  if (!payload.name.trim()) return "Название обязательно";
  if (payload.price != null && payload.price < 0) return "Цена не может быть отрицательной";
  if (payload.stockQuantity != null && payload.stockQuantity < 0) {
    return "Остаток не может быть отрицательным";
  }
  if (payload.leadTimeDays != null && payload.leadTimeDays < 0) {
    return "Срок поставки не может быть отрицательным";
  }
  return null;
}

export function ParsedRowEditModal({
  row,
  open,
  loading,
  error,
  onClose,
  onSave,
}: ParsedRowEditModalProps) {
  const [form, setForm] = useState<UpdateParsedRowRequest | null>(null);
  const [clientError, setClientError] = useState("");

  useEffect(() => {
    if (row) {
      setForm(rowToUpdateRequest(row));
      setClientError("");
    }
  }, [row]);

  if (!row || !form) return null;

  function updateField<K extends keyof UpdateParsedRowRequest>(
    key: K,
    value: UpdateParsedRowRequest[K],
  ) {
    setForm((current) => (current ? { ...current, [key]: value } : current));
  }

  function handleSave() {
    if (!row || !form) return;
    const validationError = validateForm(form);
    if (validationError) {
      setClientError(validationError);
      return;
    }
    setClientError("");
    onSave(row.id, form);
  }

  const fieldErrors = (field: string) =>
    (row.errors ?? []).filter((item) => item.field === field);

  const fieldWarnings = (field: string) =>
    (row.warnings ?? []).filter((item) => item.field === field);

  function renderFieldMessages(field: string) {
    const errors = fieldErrors(field);
    const warnings = fieldWarnings(field);
    if (errors.length === 0 && warnings.length === 0) return null;
    return (
      <div className="mt-1 space-y-0.5">
        {errors.map((item, index) => (
          <p key={`e-${index}`} className="text-xs text-red-600">
            {item.message}
          </p>
        ))}
        {warnings.map((item, index) => (
          <p key={`w-${index}`} className="text-xs text-amber-600">
            {item.message}
          </p>
        ))}
      </div>
    );
  }

  return (
    <Modal
      open={open}
      title={`Строка ${row.sourceRowNumber ?? "—"}`}
      wide
      onClose={onClose}
      footer={
        <ModalActions
          onCancel={onClose}
          onConfirm={handleSave}
          confirmLabel="Сохранить"
          loading={loading}
        />
      }
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <InputField
            label="SKU"
            value={form.sku}
            onChange={(event) => updateField("sku", event.target.value)}
          />
          {renderFieldMessages("sku")}
        </div>
        <div>
          <InputField
            label="Название"
            value={form.name}
            onChange={(event) => updateField("name", event.target.value)}
          />
          {renderFieldMessages("name")}
        </div>
        <InputField
          label="Бренд"
          value={form.brand ?? ""}
          onChange={(event) => updateField("brand", event.target.value || null)}
        />
        <InputField
          label="MPN"
          value={form.mpn ?? ""}
          onChange={(event) => updateField("mpn", event.target.value || null)}
        />
        <div>
          <InputField
            label="Цена"
            type="number"
            min={0}
            step="0.01"
            value={form.price ?? ""}
            onChange={(event) =>
              updateField(
                "price",
                event.target.value === "" ? null : Number(event.target.value),
              )
            }
          />
          {renderFieldMessages("price")}
        </div>
        <SelectField
          label="НДС включён"
          value={
            form.priceIncludesVat == null ? "" : form.priceIncludesVat ? "true" : "false"
          }
          onChange={(event) => {
            const value = event.target.value;
            updateField(
              "priceIncludesVat",
              value === "" ? null : value === "true",
            );
          }}
        >
          <option value="">Не указано</option>
          <option value="true">Да</option>
          <option value="false">Нет</option>
        </SelectField>
        <InputField
          label="Остаток"
          type="number"
          min={0}
          value={form.stockQuantity ?? ""}
          onChange={(event) =>
            updateField(
              "stockQuantity",
              event.target.value === "" ? null : Number(event.target.value),
            )
          }
        />
        <SelectField
          label="Статус остатка"
          value={form.stockStatus ?? ""}
          onChange={(event) =>
            updateField(
              "stockStatus",
              (event.target.value as StockStatus) || null,
            )
          }
        >
          <option value="">Не указано</option>
          {(Object.keys(STOCK_STATUS_LABELS) as StockStatus[]).map((status) => (
            <option key={status} value={status}>
              {STOCK_STATUS_LABELS[status]}
            </option>
          ))}
        </SelectField>
        <InputField
          label="Срок поставки (дней)"
          type="number"
          min={0}
          value={form.leadTimeDays ?? ""}
          onChange={(event) =>
            updateField(
              "leadTimeDays",
              event.target.value === "" ? null : Number(event.target.value),
            )
          }
        />
      </div>

      {(clientError || error) && (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {clientError || error}
        </p>
      )}

      {(row.errors?.length ?? 0) > 0 && (
        <div className="mt-4 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">
          После сохранения строка будет перепроверена сервером.
        </div>
      )}
    </Modal>
  );
}
