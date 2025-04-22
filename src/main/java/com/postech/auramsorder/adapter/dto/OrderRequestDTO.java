package com.postech.auramsorder.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    private Long clientId;
    private RequestStockReserveDTO items;
    private PaymentDataDTO paymentData;
}