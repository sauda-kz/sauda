package com.sauda.service;

import com.sauda.domain.entity.Lot;
import com.sauda.dto.lot.CreateLotRequest;
import com.sauda.dto.lot.IncompleteLotWarningResponse;
import com.sauda.dto.lot.UpdateLotRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LotValidationService {

    private static final Map<String, String> KEY_FIELD_LABELS =
            Map.of(
                    "budgetAmount", "сумма лота",
                    "deliveryDeadline", "срок поставки",
                    "technicalRequirements", "технические требования",
                    "submissionDeadline", "срок подачи",
                    "quantity", "количество");

    public List<String> findMissingKeyFields(CreateLotRequest request) {
        return findMissingKeyFields(
                request.budgetAmount(),
                request.deliveryDeadline(),
                request.technicalRequirements(),
                request.submissionDeadline(),
                request.quantity());
    }

    public List<String> findMissingKeyFields(UpdateLotRequest request) {
        return findMissingKeyFields(
                request.budgetAmount(),
                request.deliveryDeadline(),
                request.technicalRequirements(),
                request.submissionDeadline(),
                request.quantity());
    }

    public List<String> findMissingKeyFields(Lot lot) {
        return findMissingKeyFields(
                lot.getBudgetAmount(),
                lot.getDeliveryDeadline(),
                lot.getTechnicalRequirements(),
                lot.getSubmissionDeadline(),
                lot.getQuantity());
    }

    public IncompleteLotWarningResponse buildWarning(List<String> missingFields) {
        String labels =
                String.join(
                        ", ",
                        missingFields.stream()
                                .map(field -> KEY_FIELD_LABELS.getOrDefault(field, field))
                                .toList());
        String message =
                "Не заполнены важные поля: "
                        + labels
                        + ". Вы уверены, что хотите сохранить лот с пометкой «Требует проверки»?";
        return new IncompleteLotWarningResponse(message, List.copyOf(missingFields));
    }

    private static List<String> findMissingKeyFields(
            BigDecimal budgetAmount,
            Instant deliveryDeadline,
            String technicalRequirements,
            Instant submissionDeadline,
            Integer quantity) {
        List<String> missing = new ArrayList<>();
        if (budgetAmount == null || budgetAmount.signum() <= 0) {
            missing.add("budgetAmount");
        }
        if (deliveryDeadline == null) {
            missing.add("deliveryDeadline");
        }
        if (!StringUtils.hasText(technicalRequirements)) {
            missing.add("technicalRequirements");
        }
        if (submissionDeadline == null) {
            missing.add("submissionDeadline");
        }
        if (quantity == null || quantity <= 0) {
            missing.add("quantity");
        }
        return List.copyOf(missing);
    }
}
