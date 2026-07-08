package com.sauda.service.imports.event;

import com.sauda.service.imports.ImportRunService;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ImportTriggerListener {

    private final ImportRunService importRunService;

    public ImportTriggerListener(ImportRunService importRunService) {
        this.importRunService = importRunService;
    }

    @Async("importTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRawUploadStored(RawUploadStoredEvent event) {
        UUID rawUploadId = event.rawUploadId();
        try {
            UUID importRunId = importRunService.createRun(rawUploadId);
            importRunService.process(importRunId);
        } catch (RuntimeException exception) {
            log.error("Import trigger failed: rawUploadId={}", rawUploadId, exception);
        }
    }
}
