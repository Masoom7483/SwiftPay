package com.swiftpay.gateway.controller;

import com.swiftpay.common.PaymentStatus;
import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.dto.PaymentResponse;
import com.swiftpay.gateway.exception.GlobalExceptionHandler;
import com.swiftpay.gateway.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void createPaymentAcceptsAndReturnsSnakeCaseJson() throws Exception {
        when(paymentService.initiatePayment(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(
                        "txn-1",
                        PaymentStatus.PENDING,
                        "acc_1001",
                        "acc_2002",
                        new BigDecimal("12.50"),
                        "USD",
                        Instant.parse("2026-07-01T10:15:30Z")));

        mockMvc.perform(post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transaction_id": "txn-1",
                                  "sender_id": "acc_1001",
                                  "receiver_id": "acc_2002",
                                  "amount": 12.50,
                                  "currency": "USD"
                                }"""))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.transaction_id").value("txn-1"))
                .andExpect(jsonPath("$.sender_id").value("acc_1001"))
                .andExpect(jsonPath("$.receiver_id").value("acc_2002"))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.transactionId").doesNotExist());

        ArgumentCaptor<PaymentRequest> requestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).initiatePayment(requestCaptor.capture());
        assertThat(requestCaptor.getValue().transactionId()).isEqualTo("txn-1");
        assertThat(requestCaptor.getValue().senderId()).isEqualTo("acc_1001");
        assertThat(requestCaptor.getValue().receiverId()).isEqualTo("acc_2002");
        assertThat(requestCaptor.getValue().amount()).isEqualByComparingTo("12.50");
    }

    @Test
    void createPaymentReturnsStandardValidationError() throws Exception {
        mockMvc.perform(post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transaction_id": "",
                                  "sender_id": "acc_1001",
                                  "receiver_id": "acc_2002",
                                  "amount": 0,
                                  "currency": "usd"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field_errors").isArray())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }
}
