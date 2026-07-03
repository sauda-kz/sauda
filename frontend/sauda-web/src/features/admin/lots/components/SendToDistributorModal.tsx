import { useEffect, useState } from "react";
import type { PotentialMatch } from "../types";
import { sendLotToDistributor } from "../../../../api/lots";
import { useAuth } from "../../../../auth/AuthProvider";
import { ApiError } from "../../../../api/client";
import { InputField, SelectField, TextAreaField } from "../../../../components/ui/Input";
import { Modal, ModalActions } from "../../../../components/ui/Modal";

interface SendToDistributorModalProps {
  lotId: string;
  match: PotentialMatch | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function SendToDistributorModal({
  lotId,
  match,
  onClose,
  onSuccess,
}: SendToDistributorModalProps) {
  const { token } = useAuth();
  const [matchReason, setMatchReason] = useState("");
  const [riskFlags, setRiskFlags] = useState("");
  const [adminComment, setAdminComment] = useState("");
  const [status, setStatus] = useState<"matched" | "needs_review">("matched");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const open = match != null;

  useEffect(() => {
    if (match) {
      setMatchReason(match.matchReason);
      setRiskFlags("");
      setAdminComment("");
      setStatus(match.recommendedStatus === "needs_review" ? "needs_review" : "matched");
      setError("");
      setSuccess(false);
    }
  }, [match]);

  function resetAndClose() {
    setMatchReason("");
    setRiskFlags("");
    setAdminComment("");
    setStatus("matched");
    setError("");
    setSuccess(false);
    onClose();
  }

  async function handleSend() {
    if (!token || !match) return;
    setLoading(true);
    setError("");
    try {
      await sendLotToDistributor(
        lotId,
        {
          offerId: match.offerId,
          matchReason: matchReason || match.matchReason,
          riskFlags: riskFlags
            ? riskFlags.split(",").map((s) => s.trim()).filter(Boolean)
            : match.missingData,
          adminComment: adminComment || undefined,
          status,
        },
        token,
      );
      setSuccess(true);
      setTimeout(() => {
        resetAndClose();
        onSuccess();
      }, 1200);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось отправить");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal
      open={open}
      title="Отправить дистрибьютору"
      onClose={resetAndClose}
      wide
      footer={
        !success ? (
          <ModalActions
            confirmLabel="Отправить"
            onCancel={resetAndClose}
            onConfirm={handleSend}
            loading={loading}
          />
        ) : undefined
      }
    >
      {success ? (
        <p className="text-sm font-medium text-emerald-700">
          Лот успешно отправлен дистрибьютору «{match?.distributorName}»
        </p>
      ) : (
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            Offer: <strong>{match?.offerName}</strong> · {match?.distributorName}
          </p>
          {error && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
          )}
          <TextAreaField
            label="Причина совпадения"
            value={matchReason}
            onChange={(e) => setMatchReason(e.target.value)}
            rows={2}
          />
          <InputField
            label="Risk flags (через запятую)"
            value={riskFlags}
            onChange={(e) => setRiskFlags(e.target.value)}
            hint="Например: on_order, low_stock"
          />
          <TextAreaField
            label="Комментарий админа"
            value={adminComment}
            onChange={(e) => setAdminComment(e.target.value)}
            rows={2}
          />
          <SelectField
            label="Статус match"
            value={status}
            onChange={(e) => setStatus(e.target.value as "matched" | "needs_review")}
          >
            <option value="matched">matched</option>
            <option value="needs_review">needs_review</option>
          </SelectField>
        </div>
      )}
    </Modal>
  );
}
