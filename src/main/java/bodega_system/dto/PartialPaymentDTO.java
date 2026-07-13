package bodega_system.dto;

import bodega_system.enums.PaymentMethod;

public class PartialPaymentDTO {
    public Double amount;
    public PaymentMethod method;
    public Long customerId;
}