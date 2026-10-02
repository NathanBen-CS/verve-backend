package com.vervetutor.tutor_assistant.Stripe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {
    private String status;
    private String message;
    private String sessionId;
    private String sessionUrl;

}
