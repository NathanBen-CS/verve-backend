package com.vervetutor.tutor_assistant.Stripe2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionDto {
    private String id;
    private String status;
    private Long daysUntilRepay;
    private Long currentPeriodStart;
    private String priceId;
    private double priceAmount;
    private boolean willCancel;
}