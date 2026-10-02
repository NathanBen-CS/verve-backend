package com.vervetutor.tutor_assistant.Stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.vervetutor.tutor_assistant.Tutor.Tutor;
import com.vervetutor.tutor_assistant.Tutor.TutorService;
import com.vervetutor.tutor_assistant.User.User;
import com.vervetutor.tutor_assistant.User.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@RestController
@NoArgsConstructor
@AllArgsConstructor
@RequestMapping("/webhook")
@Slf4j
public class WebhookController {

    @Autowired
    private UserService userService;

    @Autowired
    private TutorService tutorService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Value("${stripe.pro}")
    private String stripeProPrice;

    @Value("${stripe.starter}")
    private String stripeStarterPrice;

    @PostMapping("/stripe")
    @Transactional
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request, @RequestBody String payload) {
        String sigHeader = request.getHeader("Stripe-Signature");
        Event event;

        log.info("Received Stripe webhook with signature: {}", sigHeader != null ? "Present" : "Missing");

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            log.info("Successfully verified webhook signature for event: {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error constructing webhook event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing webhook");
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        if (dataObjectDeserializer.getObject().isEmpty()) {
            log.error("Could not deserialize event data for event type: {}", event.getType());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not deserialize event data");
        }

        StripeObject stripeObject = dataObjectDeserializer.getObject().get();
        log.info("Processing event type: {} with object type: {}", event.getType(), stripeObject.getClass().getSimpleName());

        // Handle different event types
        if (stripeObject instanceof PaymentIntent paymentIntent) {
            return handlePaymentIntentEvent(event, paymentIntent);
        } else if (stripeObject instanceof Session session) {
            return handleCheckoutSessionEvent(event, session);
        } else if (stripeObject instanceof Subscription subscription) {
            return handleSubscriptionEvent(event, subscription);
        } else if (stripeObject instanceof Invoice invoice) {
            return handleInvoiceEvent(event, invoice);
        } else {
            log.info("Received unhandled event type: {}, ignoring", event.getType());
            return ResponseEntity.ok("Event received but not processed");
        }
    }

    // Handle subscription events
    private ResponseEntity<String> handleSubscriptionEvent(Event event, Subscription subscription) {
        String customerId = subscription.getCustomer();

        if (customerId == null || customerId.trim().isEmpty()) {
            log.warn("Subscription {} has no customer ID", subscription.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing customer ID");
        }

        log.info("Processing subscription event {} for customer: {}", event.getType(), customerId);

        try {
            switch (event.getType()) {
                case "customer.subscription.created" -> {
                    return handleSubscriptionCreated(customerId, subscription);
                }
                case "customer.subscription.updated" -> {
                    return handleSubscriptionUpdated(customerId, subscription);
                }
                case "customer.subscription.deleted" -> {
                    return handleSubscriptionDeleted(customerId, subscription);
                }
                default -> {
                    log.info("Unhandled Subscription event type: {}", event.getType());
                    return ResponseEntity.ok("Event received but not handled");
                }
            }
        } catch (Exception e) {
            log.error("Error processing Subscription event {}: {}", event.getType(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing subscription event");
        }
    }

    // Handle invoice events (for billing cycles)
    private ResponseEntity<String> handleInvoiceEvent(Event event, Invoice invoice) {
        String customerId = invoice.getCustomer();

        if (customerId == null || customerId.trim().isEmpty()) {
            log.warn("Invoice {} has no customer ID", invoice.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing customer ID");
        }

        log.info("Processing invoice event {} for customer: {}", event.getType(), customerId);

        try {
            switch (event.getType()) {
                case "invoice.payment_succeeded" -> {
                    return handleInvoicePaymentSucceeded(customerId, invoice);
                }
                case "invoice.payment_failed" -> {
                    return handleInvoicePaymentFailed(customerId, invoice);
                }
                default -> {
                    log.info("Unhandled Invoice event type: {}", event.getType());
                    return ResponseEntity.ok("Event received but not handled");
                }
            }
        } catch (Exception e) {
            log.error("Error processing Invoice event {}: {}", event.getType(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing invoice event");
        }
    }

    // Subscription event handlers
    private ResponseEntity<String> handleSubscriptionCreated(String customerId, Subscription subscription) {
        log.info("Processing created subscription for customer: {}, subscription: {}, status: {}",
                customerId, subscription.getId(), subscription.getStatus());

        try {
            Optional<User> userOptional = userService.findByStripe(customerId);

            if (userOptional.isEmpty()) {
                log.error("No user found for Stripe customer ID: {}", customerId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found for customer ID: " + customerId);
            }

            User user = userOptional.get();
            String tier = getSubscriptionTier(subscription);
            String status = subscription.getStatus();

            log.info("Found user {} for subscription creation, tier: {}, status: {}",
                    user.getEmail(), tier, status);

            // Update user based on subscription
            boolean shouldBeActive = "active".equals(status) || "trialing".equals(status);
            user.setActive(shouldBeActive);
            user.setSubscriptionTier(tier);

            User updatedUser = userService.updateUser(user.getId(), user);

            // Set initial privileges
            refreshPrivileges(user, tier);

            log.info("Successfully created subscription for user {} with tier {}",
                    updatedUser.getEmail(), tier);

            return ResponseEntity.ok("Subscription created successfully");

        } catch (Exception e) {
            log.error("Error processing subscription creation for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing subscription creation");
        }
    }

    private ResponseEntity<String> handleSubscriptionUpdated(String customerId, Subscription subscription) {
        log.info("Processing updated subscription for customer: {}, subscription: {}, status: {}, cancel_at_period_end: {}, trial_end: {}",
                customerId, subscription.getId(), subscription.getStatus(), subscription.getCancelAtPeriodEnd(), subscription.getTrialEnd());

        try {
            Optional<User> userOptional = userService.findByStripe(customerId);

            if (userOptional.isEmpty()) {
                log.error("No user found for Stripe customer ID: {}", customerId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found for customer ID: " + customerId);
            }

            User user = userOptional.get();
            String tier = getSubscriptionTier(subscription);
            String status = subscription.getStatus();
            Boolean cancelAtPeriodEnd = subscription.getCancelAtPeriodEnd();

            log.info("Updating subscription for user {}: status={}, tier={}, cancel_at_period_end={}, trial_end={}",
                    user.getEmail(), status, tier, cancelAtPeriodEnd, subscription.getTrialEnd());

            // Check if subscription is scheduled for cancellation
            if (Boolean.TRUE.equals(cancelAtPeriodEnd)) {
                log.info("Subscription for user {} is scheduled for cancellation at period end", user.getEmail());
                // Keep user active until the subscription actually ends (customer.subscription.deleted event)
                // Just log this for now - user remains active until period ends
                return ResponseEntity.ok("Subscription marked for cancellation at period end");
            }

            // Update user status based on subscription status (including trial transitions)
            boolean shouldBeActive = "active".equals(status) || "trialing".equals(status);

            user.setActive(shouldBeActive);
            user.setSubscriptionTier(tier);
            userService.updateUser(user.getId(), user);

            // Update privileges if tier changed or subscription reactivated
            if (shouldBeActive) {
                refreshPrivileges(user, tier);
            }

            // Special handling for trial to active transition
            if ("active".equals(status) && subscription.getTrialEnd() != null) {
                log.info("User {} trial period ended, now on active subscription", user.getEmail());
                // Privileges should remain the same, just log the transition
            }

            log.info("Successfully updated subscription for user {}", user.getEmail());

            return ResponseEntity.ok("Subscription updated successfully");

        } catch (Exception e) {
            log.error("Error processing subscription update for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing subscription update");
        }
    }

    private ResponseEntity<String> handleSubscriptionDeleted(String customerId, Subscription subscription) {
        log.info("Processing deleted subscription for customer: {}, subscription: {}, canceled_at: {}, trial_end: {}",
                customerId, subscription.getId(), subscription.getCanceledAt(), subscription.getTrialEnd());

        try {
            Optional<User> userOptional = userService.findByStripe(customerId);

            if (userOptional.isEmpty()) {
                log.error("No user found for Stripe customer ID: {}", customerId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found for customer ID: " + customerId);
            }

            User user = userOptional.get();

            // Check if this was a trial cancellation vs regular cancellation
            boolean wasCanceledDuringTrial = subscription.getTrialEnd() != null &&
                    subscription.getCanceledAt() != null &&
                    subscription.getCanceledAt() <= subscription.getTrialEnd();

            log.info("Deactivating user {} due to subscription deletion{}",
                    user.getEmail(),
                    wasCanceledDuringTrial ? " (canceled during trial)" : " (expired/canceled after trial)");

            // Deactivate user and reset to basic tier
            user.setActive(false);
            user.setSubscriptionTier("STARTER");
            userService.updateUser(user.getId(), user);

            // Reset to basic privileges
            resetToBasicPrivileges(user);

            log.info("Successfully deactivated user {} due to subscription deletion", user.getEmail());

            return ResponseEntity.ok("Subscription deletion processed successfully");

        } catch (Exception e) {
            log.error("Error processing subscription deletion for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing subscription deletion");
        }
    }

    // Invoice event handlers
    private ResponseEntity<String> handleInvoicePaymentSucceeded(String customerId, Invoice invoice) {
        log.info("Processing successful invoice payment for customer: {}, invoice: {}, amount: {}",
                customerId, invoice.getId(), invoice.getAmountPaid());

        try {
            Optional<User> userOptional = userService.findByStripe(customerId);

            if (userOptional.isEmpty()) {
                log.error("No user found for Stripe customer ID: {}", customerId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found for customer ID: " + customerId);
            }

            User user = userOptional.get();

            log.info("Invoice payment succeeded for user {}, refreshing privileges", user.getEmail());

            // Refresh privileges on successful payment (new billing cycle)
            String tier = user.getSubscriptionTier() != null ? user.getSubscriptionTier() : "STARTER";
            refreshPrivileges(user, tier);

            // Update last payment date
            user.setLastPaymentDate(LocalDateTime.now(ZoneOffset.UTC));
            userService.updateUser(user.getId(), user);

            log.info("Successfully processed invoice payment for user {}", user.getEmail());

            return ResponseEntity.ok("Invoice payment processed successfully");

        } catch (Exception e) {
            log.error("Error processing invoice payment for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing invoice payment");
        }
    }

    private ResponseEntity<String> handleInvoicePaymentFailed(String customerId, Invoice invoice) {
        log.warn("Invoice payment failed for customer: {}, invoice: {}", customerId, invoice.getId());

        try {
            Optional<User> userOptional = userService.findByStripe(customerId);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                log.warn("Invoice payment failed for user {}", user.getEmail());

                // You might want to send notifications or take other actions here
                // Don't immediately deactivate - Stripe will retry payments
                // If all payment attempts fail, Stripe will eventually cancel the subscription
                // and send a customer.subscription.deleted event

            } else {
                log.warn("Invoice payment failed but no user found for customer ID: {}", customerId);
            }

            return ResponseEntity.ok("Invoice payment failure processed");

        } catch (Exception e) {
            log.error("Error handling invoice payment failure for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing invoice payment failure");
        }
    }

    // Helper methods
    private String getSubscriptionTier(Subscription subscription) {
        try {
            if (subscription.getItems() != null && !subscription.getItems().getData().isEmpty()) {
                SubscriptionItem item = subscription.getItems().getData().get(0);
                String priceId = item.getPrice().getId();

                if (stripeProPrice.equals(priceId)) {
                    return "PRO";
                } else if (stripeStarterPrice.equals(priceId)) {
                    return "STARTER";
                }
            }
        } catch (Exception e) {
            log.error("Error getting tier from subscription: {}", e.getMessage());
        }
        return "STARTER"; // default
    }

    private void refreshPrivileges(User user, String tier) {
        try {
            Tutor tutor = user.getTutor();
            if (tutor != null) {
                int queries = "PRO".equals(tier) ? 50 : 15;
                tutor.setAiQueries(queries);
                tutorService.saveTutor(tutor);

                log.info("Refreshed privileges for user {} (tier: {}, queries: {})",
                        user.getEmail(), tier, queries);
            } else {
                log.warn("User {} has no associated tutor", user.getEmail());
            }
        } catch (Exception e) {
            log.error("Failed to refresh privileges for user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void resetToBasicPrivileges(User user) {
        try {
            Tutor tutor = user.getTutor();
            if (tutor != null) {
                tutor.setAiQueries(15); // Basic tier queries
                tutorService.saveTutor(tutor);
                log.info("Reset user {} to basic privileges", user.getEmail());
            }
        } catch (Exception e) {
            log.error("Failed to reset privileges for user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // Existing checkout session methods
    private ResponseEntity<String> handleCheckoutSessionEvent(Event event, Session session) {
        String customerId = session.getCustomer();

        if (customerId == null || customerId.trim().isEmpty()) {
            log.warn("Checkout Session {} has no customer ID", session.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing customer ID");
        }

        log.info("Processing checkout session event {} for customer: {}", event.getType(), customerId);

        try {
            switch (event.getType()) {
                case "checkout.session.completed" -> {
                    return handleCheckoutSessionCompleted(customerId, session);
                }
                default -> {
                    log.info("Unhandled Checkout Session event type: {}", event.getType());
                    return ResponseEntity.ok("Event received but not handled");
                }
            }
        } catch (Exception e) {
            log.error("Error processing Checkout Session event {}: {}", event.getType(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing checkout session event");
        }
    }

    private ResponseEntity<String> handleCheckoutSessionCompleted(String customerId, Session session) {
        log.info("Processing completed checkout session for customer: {}, session: {}, payment status: {}",
                customerId, session.getId(), session.getPaymentStatus());

        try {
            if (!"paid".equals(session.getPaymentStatus())) {
                log.warn("Checkout session completed but payment status is: {}", session.getPaymentStatus());
                return ResponseEntity.ok("Checkout session completed but payment not confirmed");
            }

            Optional<User> userOptional = userService.findByStripe(customerId);

            if (userOptional.isEmpty()) {
                log.error("No user found for Stripe customer ID: {}", customerId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found for customer ID: " + customerId);
            }

            User user = userOptional.get();
            log.info("Found user {} (ID: {}) for customer {}, current active status: {}",
                    user.getEmail(), user.getId(), customerId, user.getActive());

            user.setActive(true);
            User updatedUser = userService.updateUser(user.getId(), user);

            log.info("Successfully activated user {} (ID: {}) from checkout session, new status: {}",
                    updatedUser.getEmail(), updatedUser.getId(), updatedUser.getActive());

            return ResponseEntity.ok("Checkout session processed successfully");

        } catch (Exception e) {
            log.error("Error activating user for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user status");
        }
    }

    // Existing PaymentIntent methods
    private ResponseEntity<String> handlePaymentIntentEvent(Event event, PaymentIntent paymentIntent) {
        String customerId = paymentIntent.getCustomer();

        if (customerId == null || customerId.trim().isEmpty()) {
            log.warn("PaymentIntent {} has no customer ID", paymentIntent.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing customer ID");
        }

        log.info("Processing event {} for customer: {}", event.getType(), customerId);

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    return handlePaymentSuccess(customerId, paymentIntent);
                }
                case "payment_intent.payment_failed" -> {
                    return handlePaymentFailure(customerId, paymentIntent);
                }
                case "payment_intent.canceled" -> {
                    return handlePaymentCanceled(customerId, paymentIntent);
                }
                default -> {
                    log.info("Unhandled PaymentIntent event type: {}", event.getType());
                    return ResponseEntity.ok("Event received but not handled");
                }
            }
        } catch (Exception e) {
            log.error("Error processing PaymentIntent event {}: {}", event.getType(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payment event");
        }
    }

    private ResponseEntity<String> handlePaymentSuccess(String customerId, PaymentIntent paymentIntent) {
        log.info("Processing successful payment for customer: {}, amount: {}",
                customerId, paymentIntent.getAmount());

        try {
            Optional<User> userOptional = userService.findByStripe(customerId);

            if (userOptional.isEmpty()) {
                log.error("No user found for Stripe customer ID: {}", customerId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found for customer ID: " + customerId);
            }

            User user = userOptional.get();
            log.info("Found user {} (ID: {}) for customer {}, current active status: {}",
                    user.getEmail(), user.getId(), customerId, user.getActive());

            user.setActive(true);
            User updatedUser = userService.updateUser(user.getId(), user);

            log.info("Successfully activated user {} (ID: {}), new status: {}",
                    updatedUser.getEmail(), updatedUser.getId(), updatedUser.getActive());

            return ResponseEntity.ok("Payment processed successfully");

        } catch (Exception e) {
            log.error("Error activating user for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user status");
        }
    }

    private ResponseEntity<String> handlePaymentFailure(String customerId, PaymentIntent paymentIntent) {
        log.warn("Payment failed for customer: {}, payment intent: {}, reason: {}",
                customerId, paymentIntent.getId(), paymentIntent.getLastPaymentError());

        try {
            Optional<User> userOptional = userService.findByStripe(customerId);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                log.info("Payment failed for user {} (ID: {})", user.getEmail(), user.getId());
            } else {
                log.warn("Payment failed but no user found for customer ID: {}", customerId);
            }

            return ResponseEntity.ok("Payment failure processed");

        } catch (Exception e) {
            log.error("Error handling payment failure for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payment failure");
        }
    }

    private ResponseEntity<String> handlePaymentCanceled(String customerId, PaymentIntent paymentIntent) {
        log.info("Payment canceled for customer: {}, payment intent: {}", customerId, paymentIntent.getId());

        try {
            Optional<User> userOptional = userService.findByStripe(customerId);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                log.info("Payment canceled for user {} (ID: {})", user.getEmail(), user.getId());
            } else {
                log.warn("Payment canceled but no user found for customer ID: {}", customerId);
            }

            return ResponseEntity.ok("Payment cancellation processed");

        } catch (Exception e) {
            log.error("Error handling payment cancellation for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payment cancellation");
        }
    }
}