import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  createLot,
  emptyLotFormValues,
  formValuesToPayload,
  IncompleteLotError,
} from "../../../../api/lots";
import { useAuth } from "../../../../auth/AuthProvider";
import { ApiError } from "../../../../api/client";
import { IncompleteLotModal } from "../components/IncompleteLotModal";
import { LotForm } from "../components/LotForm";
import type { IncompleteLotWarning, LotFormValues } from "../types";

export function LotCreatePage() {
  const navigate = useNavigate();
  const { token } = useAuth();
  const [values, setValues] = useState<LotFormValues>(emptyLotFormValues());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [warning, setWarning] = useState<IncompleteLotWarning | null>(null);
  const [showWarning, setShowWarning] = useState(false);

  async function submit(confirmIncomplete = false) {
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      const lot = await createLot(formValuesToPayload(values, confirmIncomplete), token);
      navigate(`/admin/lots/${lot.id}/edit`);
    } catch (err) {
      if (err instanceof IncompleteLotError) {
        setWarning(err.warning);
        setShowWarning(true);
      } else {
        setError(err instanceof ApiError ? err.message : "Не удалось создать лот");
      }
    } finally {
      setLoading(false);
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    void submit(false);
  }

  return (
    <div className="space-y-6">
      <div>
        <Link to="/admin/lots" className="text-sm text-brand-600 hover:underline">
          ← К списку лотов
        </Link>
        <h1 className="mt-2 text-2xl font-bold text-slate-900">Новый лот</h1>
      </div>

      <LotForm
        values={values}
        onChange={setValues}
        onSubmit={handleSubmit}
        loading={loading}
        submitLabel="Создать лот"
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
