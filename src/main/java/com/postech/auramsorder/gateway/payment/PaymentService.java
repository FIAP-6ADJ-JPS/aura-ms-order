package com.postech.auramsorder.gateway.payment;

import com.postech.auramsorder.domain.Order;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class PaymentService {
    // Um serviço real se conectaria com um gateway de pagamento Mercado pago ou pagar.me
    // pode deixar atualmente como mock

    public boolean processPayment(Order order) {

        String cardNumber = order.getPaymentCardNumber();


        boolean paymentApproved = cardNumber != null && cardNumber.startsWith("4");

        if (paymentApproved) {
            order.setPaymentId(generateRandomPaymentId());
            return true;
        }

        return false;
    }

    public void refundIfNecessary(Order order) {
        // Verificar se o pagamento foi feito
        if (order.getPaymentId() != null) {
            // Simular  estorno

        }
    }

    private Long generateRandomPaymentId() {
        return new Random().nextLong();
    }
}
