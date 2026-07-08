import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  formValuesToPayload,
  IncompleteLotError,
  lotToFormValues,
  updateLot,
} from "../../../../api/lots";
import { useAuth } from "../../../../auth/AuthProvider";
import { ApiError } from "../../../../api/client";
import { IncompleteLotModal } from "../components/IncompleteLotModal";
import { LotForm } from "../components/LotForm";
import { useLot } from "../hooks/useLot";
import type { IncompleteLotWarning, LotFormValues } from "../types";
import { emptyLotFormValues } from "../../../../api/lots";

export function LotEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { token } = useAuth();
  const { lot, loading: lotLoading, error: lotError } = useLot(id);
  const [values, setValues] = useState<LotFormValues>(emptyLotFormValues());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [warning, setWarning] = useState<IncompleteLotWarning | null>(null);
  const [showWarning, setShowWarning] = useState(false);

  useEffect(() => {
    if (lot) setValues(lotToFormValues(lot));
  }, [lot]);

  async function submit(confirmIncomplete = false) {
    if (!token || !id) return;
    setLoading(true);
    setError("");
    try {
      await updateLot(id, formValuesToPayload(values, confirmIncomplete), token);
      navigate(`/admin/lots/${id}`);
    } catch (err) {
      if (err instanceof IncompleteLotError) {
        setWarning(err.warning);
        setShowWarning(true);
      } else {
        setError(err instanceof ApiError ? err.message : "Не удалось сохранить");
      }
    } finally {
      setLoading(false);
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    void submit(false);
  }

  if (lotLoading) {
    return <p className="text-sm text-slate-500">Загрузка…</p>;
  }
  if (lotError || !lot) {
    return <p className="text-sm text-red-600">{lotError || "Лот не найден"}</p>;
  }

  return (
    <div className="space-y-6">
      <div>
        <Link to={`/admin/lots/${id}`} className="text-sm text-brand-600 hover:underline">
          ← К карточке лота
        </Link>
        <h1 className="mt-2 text-2xl font-bold text-slate-900">Редактирование: {lot.title}</h1>
      </div>

      <LotForm
        values={values}
        onChange={setValues}
        onSubmit={handleSubmit}
        loading={loading}
        lotId={id}
        error={error}
      />

      <IncompleteLotModal
        open={showWarning}
        warning={warning}
        loading={loading}
        onEdit={() => setShowWarning(false)}
        onConfirm={() => {
          setShowWarning(false);
          void submit(true);
        }}
      />
    </div>
  );
}
