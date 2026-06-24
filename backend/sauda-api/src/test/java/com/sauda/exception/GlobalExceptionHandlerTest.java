package com.sauda.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.dto.ApiErrorResponse;
import com.sauda.dto.lot.CreateLotRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleSaudaExceptionReturnsBadRequest() {
        var response = handler.handleSaudaException(new SaudaException("Invalid request"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Invalid request");
    }

    @Test
    void handleSaudaNotFoundExceptionReturnsNotFound() {
        var response =
                handler.handleSaudaNotFoundException(
                        new SaudaNotFoundException("Lot not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Lot not found");
    }

    @Test
    void handleValidationExceptionReturnsBadRequest() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(
                        new CreateLotRequest(
                                "manual",
                                null,
                                "LOT-001",
                                "",
                                null,
                                "customer",
                                "SSD",
                                null,
                                "товар",
                                10,
                                "шт",
                                java.math.BigDecimal.TEN,
                                "KZT",
                                "Алматы",
                                java.time.Instant.now(),
                                null,
                                null,
                                "tech",
                                "docs",
                                null,
                                null,
                                null,
                                "https://example.com",
                                null,
                                com.sauda.domain.enums.LotStatus.active),
                        "request");
        bindingResult.rejectValue("title", "NotBlank", "must not be blank");

        MethodParameter parameter =
                new MethodParameter(
                        GlobalExceptionHandlerTest.class.getDeclaredMethod(
                                "validationTarget", CreateLotRequest.class),
                        0);
        var response =
                handler.handleValidationException(
                        new MethodArgumentNotValidException(parameter, bindingResult), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("title");
    }

    @SuppressWarnings("unused")
    private void validationTarget(CreateLotRequest request) {}

    @Test
    void handleGenericExceptionReturnsInternalServerError() {
        var response = handler.handleGenericException(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("An unexpected error occurred");
    }
}
