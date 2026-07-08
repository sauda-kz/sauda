import type { ChangeEvent, FormEvent, ReactNode } from "react";
import type { LotFormValues, LotStatus } from "../types";
import { InputField, SelectField, TextAreaField } from "../../../../components/ui/Input";
import { Button } from "../../../../components/ui/Button";
import { LotAttachmentsSection } from "./LotAttachmentsSection";

const STATUS_OPTIONS: { value: LotStatus; label: string }[] = [
  { value: "draft", label: "Черновик" },
  { value: "active", label: "Активный" },
  { value: "needs_review", label: "Требует проверки" },
  { value: "expired", label: "Истёк" },
  { value: "archived", label: "Архив" },
  { value: "cancelled", label: "Отменён" },
  { value: "closed", label: "Закрыт" },
];

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="space-y-4 rounded-xl border border-slate-200 bg-white p-6">
      <legend className="px-1 text-base font-semibold text-slate-900">{title}</legend>
      {children}
    </fieldset>
  );
}

interface LotFormProps {
  values: LotFormValues;
  onChange: (values: LotFormValues) => void;
  onSubmit: (e: FormEvent) => void;
  loading?: boolean;
  submitLabel?: string;
  lotId?: string;
  error?: string;
}

export function LotForm({
  values,
  onChange,
  onSubmit,
  loading,
  submitLabel = "Сохранить",
  lotId,
  error,
}: LotFormProps) {
  const set =
    (key: keyof LotFormValues) =>
    (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
      onChange({ ...values, [key]: e.target.value });
    };

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
      )}

      <Section title="1. Источник">
        <div className="grid gap-4 sm:grid-cols-2">
          <InputField label="Источник" value={values.source} onChange={set("source")} />
          <InputField
            label="ID закупки"
            value={values.externalPurchaseId}
            onChange={set("externalPurchaseId")}
          />
          <InputField
            label="ID лота"
            value={values.externalLotId}
            onChange={set("externalLotId")}
          />
          <InputField label="URL источника" value={values.sourceUrl} onChange={set("sourceUrl")} />
        </div>
      </Section>

      <Section title="2. Общая информация">
        <div className="grid gap-4 sm:grid-cols-2">
          <InputField label="Название *" required value={values.title} onChange={set("title")} />
          <InputField
            label="Заказчик *"
            required
            value={values.customerName}
            onChange={set("customerName")}
          />
          <InputField
            label="Категория *"
            required
            value={values.category}
            onChange={set("category")}
          />
          <InputField
            label="Способ закупки"
            value={values.procurementMethod}
            onChange={set("procurementMethod")}
          />
          <InputField label="Тип лота" value={values.lotType} onChange={set("lotType")} />
        </div>
        <TextAreaField
          label="Описание"
          value={values.description}
          onChange={set("description")}
        />
      </Section>

      <Section title="3. Финансы и количество">
        <div className="grid gap-4 sm:grid-cols-3">
          <InputField
            label="Количество"
            type="number"
            min={1}
            value={values.quantity}
            onChange={set("quantity")}
          />
          <InputField label="Единица" value={values.unit} onChange={set("unit")} />
          <InputField
            label="Бюджет"
            type="number"
            min={0}
            value={values.budgetAmount}
            onChange={set("budgetAmount")}
          />
          <InputField label="Валюта" value={values.currency} onChange={set("currency")} />
        </div>
      </Section>

      <Section title="4. Поставка и сроки">
        <div className="grid gap-4 sm:grid-cols-2">
          <InputField
            label="Место поставки"
            value={values.deliveryLocation}
            onChange={set("deliveryLocation")}
          />
          <InputField
            label="Срок поставки"
            type="datetime-local"
            value={values.deliveryDeadline}
            onChange={set("deliveryDeadline")}
          />
          <InputField
            label="Дедлайн подачи"
            type="datetime-local"
            value={values.submissionDeadline}
            onChange={set("submissionDeadline")}
          />
          <InputField
            label="Дата публикации"
            type="datetime-local"
            value={values.publishedAt}
            onChange={set("publishedAt")}
          />
        </div>
      </Section>

      <Section title="5. Требования">
        <div className="grid gap-4">
          <TextAreaField
            label="Технические требования"
            value={values.technicalRequirements}
            onChange={set("technicalRequirements")}
          />
          <TextAreaField
            label="Гарантия"
            value={values.warrantyRequirements}
            onChange={set("warrantyRequirements")}
          />
          <TextAreaField
            label="Документы"
            value={values.requiredDocuments}
            onChange={set("requiredDocuments")}
          />
          <TextAreaField
            label="Квалификация"
            value={values.qualificationRequirements}
            onChange={set("qualificationRequirements")}
          />
          <TextAreaField
            label="Условия контракта"
            value={values.contractTermsSummary}
            onChange={set("contractTermsSummary")}
          />
        </div>
      </Section>

      <Section title="6. Документы">
        <LotAttachmentsSection lotId={lotId} />
      </Section>

      <Section title="7. Статус">
        <SelectField label="Статус лота" value={values.status} onChange={set("status")}>
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </SelectField>
        <TextAreaField
          label="Сырой текст (опционально)"
          value={values.rawText}
          onChange={set("rawText")}
          rows={2}
        />
      </Section>

      <div className="flex justify-end">
        <Button type="submit" loading={loading}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
