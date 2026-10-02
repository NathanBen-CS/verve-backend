package com.vervetutor.tutor_assistant.Stripe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto {
    private Long amount;
    private Long quantity;
    private String name;
    private String currency;
}
