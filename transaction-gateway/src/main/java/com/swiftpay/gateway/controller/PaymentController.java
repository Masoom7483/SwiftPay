package com.swiftpay.gateway.controller;

import com.swiftpay.gateway.dto.ErrorResponse;
import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.dto.PaymentResponse;
import com.swiftpay.gateway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/v1/payments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Payments", description = "Initiate and track peer-to-peer payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(
            summary = "Initiate a payment",
            description = "Validates the request, checks the referenced accounts and balance, "
                    + "enforces idempotency (24h), persists a PENDING record and queues a "
                    + "PaymentInitiated event through the transactional outbox. Returns 202 Accepted."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Payment accepted (PENDING)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponse.class),
                            examples = @ExampleObject(name = "accepted", value = """
                                    {
                                      "transaction_id": "a3f1c2d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
                                      "status": "PENDING",
                                      "sender_id": "acc_1001",
                                      "receiver_id": "acc_2002",
                                      "amount": 150.75,
                                      "currency": "USD",
                                      "created_at": "2026-07-01T10:15:30Z"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate transaction id",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Referenced account not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Business rule rejection",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentResponse> createPayment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "sample", value = """
                            {
                              "transaction_id": "a3f1c2d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
                              "sender_id": "acc_1001",
                              "receiver_id": "acc_2002",
                              "amount": 150.75,
                              "currency": "USD"
                            }""")))
            @Valid @RequestBody PaymentRequest request) {

        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
