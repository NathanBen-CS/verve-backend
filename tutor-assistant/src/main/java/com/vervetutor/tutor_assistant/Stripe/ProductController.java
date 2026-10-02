package com.vervetutor.tutor_assistant.Stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.vervetutor.tutor_assistant.Config.JwtService;
import com.vervetutor.tutor_assistant.Stripe2.StripeUtil;
import com.vervetutor.tutor_assistant.User.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private StripeService stripeService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StripeUtil stripeUtil;

    @Autowired
    private UserService userService;

    @Value("${stripe.pro}")
    private String stripeProPrice;

    @Value("${stripe.starter}")
    private String stripeStarterPrice;

    @PostMapping("/checkout")
    public ResponseEntity<ProductResponse> checkoutProducts(@RequestBody ProductDto productRequest)
    {
        ProductResponse productResponse= stripeService.checkoutProduct(productRequest);
        return ResponseEntity.status(HttpStatus.OK).body(productResponse);
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, Object>> createCheckoutSession(
            HttpServletRequest httpRequest, @RequestBody Map<String, String> payload
    ) throws StripeException {
        String priceId;

        String authHeader = httpRequest.getHeader("Authorization"); // e.g. "Bearer eyJhbGciOi..."
        String token = authHeader.substring(7); // remove "Bearer "

        String dbUserEmail = jwtService.extractUsername(token);  // your method to extract user ID from JWT
        log.info(dbUserEmail);
        Long dbUserId = userService.findByEmail(dbUserEmail).orElseThrow().getId();
        log.info(String.valueOf(dbUserId));

        String tierType = payload.get("tierType");

        if ((tierType.equals("starter")))
        {
            priceId = stripeStarterPrice;
        }
        else if (tierType.equals("pro"))
        {
            priceId = stripeProPrice;
        }
        else {
            throw new IllegalArgumentException("Price ID is not correct " + tierType);
        }

        // ✅ Look up Stripe Customer ID for this user
        String stripeCustomerId = stripeUtil.idByEmail(dbUserEmail);
        log.info(String.valueOf(stripeCustomerId));
        // 3️⃣ Store this new customer ID in your DB for future use!
        userService.saveStripeCustomerId(dbUserId, stripeCustomerId);

        if (stripeCustomerId == null) {
            // 2️⃣ Create a new Stripe customer if needed
            Map<String, Object> customerParams = new HashMap<>();
            customerParams.put("email", dbUserEmail); // you have this from your DB
            Customer customer = Customer.create(customerParams);

            stripeCustomerId = customer.getId();
        }

        // ✅ Build the params with correct IDs
        Map<String, Object> params = new HashMap<>();
        params.put("customer", stripeCustomerId);
        params.put("mode", "subscription");
        params.put("success_url", "http://vervetutor.com/portal");
        params.put("cancel_url", "http://vervetutor.com/");
        params.put("client_reference_id", dbUserId); // ✅ your DB ID, NOT the Stripe ID

// ✅ Add subscription data for 14-day trial
        Map<String, Object> subscriptionData = new HashMap<>();
        subscriptionData.put("trial_period_days", 14);
        params.put("subscription_data", subscriptionData);

// ✅ Line items
        List<Object> lineItems = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("price", priceId); // dev price
        item.put("quantity", 1);
        lineItems.add(item);
        params.put("line_items", lineItems);

// ✅ Create Checkout Session
        Session session = Session.create(params);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("checkoutUrl", session.getUrl());
        return ResponseEntity.ok(responseData);
    }

}
