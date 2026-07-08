import type { IncompleteLotWarning } from "../types";
import { Modal, ModalActions } from "../../../../components/ui/Modal";

interface IncompleteLotModalProps {
  open: boolean;
  warning: IncompleteLotWarning | null;
  onEdit: () => void;
  onConfirm: () => void;
  loading?: boolean;
}

export function IncompleteLotModal({
  open,
  warning,
  onEdit,
  onConfirm,
  loading,
}: IncompleteLotModalProps) {
  return (
    <Modal
      open={open}
      title="Неполные данные лота"
      onClose={onEdit}
      footer={
        <ModalActions
          cancelLabel="Вернуться к редактированию"
          confirmLabel='Сохранить с пометкой «Требует проверки»'
          onCancel={onEdit}
          onConfirm={onConfirm}
          loading={loading}
        />
      }
    >
      <p className="text-sm text-slate-600">{warning?.message}</p>
      {warning?.missingFields && warning.missingFields.length > 0 && (
        <ul className="mt-3 list-inside list-disc text-sm text-slate-700">
          {warning.missingFields.map((field) => (
            <li key={field}>{field}</li>
          ))}
        </ul>
      )}
    </Modal>
  );
}
