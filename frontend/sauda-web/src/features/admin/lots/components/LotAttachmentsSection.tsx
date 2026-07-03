import { Download, Trash2, Upload } from "lucide-react";
import { useRef } from "react";
import { attachmentDownloadUrl } from "../../../../api/lotAttachments";
import { useAuth } from "../../../../auth/AuthProvider";
import { Button } from "../../../../components/ui/Button";
import { useLotAttachments } from "../hooks/useLotAttachments";

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

interface LotAttachmentsSectionProps {
  lotId: string | undefined;
  readOnly?: boolean;
}

export function LotAttachmentsSection({ lotId, readOnly }: LotAttachmentsSectionProps) {
  const { token } = useAuth();
  const inputRef = useRef<HTMLInputElement>(null);
  const { attachments, loading, uploading, error, upload, remove } =
    useLotAttachments(lotId);

  if (!lotId) {
    return (
      <p className="text-sm text-slate-500">
        Документы можно загрузить после сохранения лота.
      </p>
    );
  }

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-base font-semibold text-slate-900">Документы</h3>
        {!readOnly && (
          <>
            <input
              ref={inputRef}
              type="file"
              className="hidden"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              onChange={async (e) => {
                const file = e.target.files?.[0];
                if (file) await upload(file);
                e.target.value = "";
              }}
            />
            <Button
              variant="secondary"
              loading={uploading}
              onClick={() => inputRef.current?.click()}
            >
              <Upload className="h-4 w-4" />
              Загрузить
            </Button>
          </>
        )}
      </div>

      {error && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
      )}
      {loading && <p className="text-sm text-slate-500">Загрузка…</p>}

      {!loading && attachments.length === 0 && (
        <p className="text-sm text-slate-500">Документы не прикреплены</p>
      )}

      <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
        {attachments.map((file) => (
          <li
            key={file.id}
            className="flex items-center justify-between gap-4 px-4 py-3 text-sm"
          >
            <div>
              <p className="font-medium text-slate-900">{file.originalFilename}</p>
              <p className="text-xs text-slate-400">
                {formatFileSize(file.fileSize)} · {file.mimeType}
              </p>
            </div>
            <div className="flex items-center gap-1">
              <a
                href={attachmentDownloadUrl(lotId, file.id)}
                className="inline-flex rounded-lg p-2 text-brand-600 hover:bg-brand-50"
                download
                onClick={(e) => {
                  e.preventDefault();
                  if (!token) return;
                  fetch(attachmentDownloadUrl(lotId, file.id), {
                    headers: { Authorization: `Bearer ${token}` },
                  })
                    .then((r) => r.blob())
                    .then((blob) => {
                      const url = URL.createObjectURL(blob);
                      const a = document.createElement("a");
                      a.href = url;
                      a.download = file.originalFilename;
                      a.click();
                      URL.revokeObjectURL(url);
                    });
                }}
              >
                <Download className="h-4 w-4" />
              </a>
              {!readOnly && (
                <Button
                  variant="ghost"
                  className="px-2 py-1.5 text-red-600"
                  onClick={() => remove(file.id)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
