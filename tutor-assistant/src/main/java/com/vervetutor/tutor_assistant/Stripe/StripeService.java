package com.vervetutor.tutor_assistant.Stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.StripeResponse;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@NoArgsConstructor
public class StripeService {

    //productname->price->quantity->currency
    //return sessionId and url

    @Value("${stripe.apikey}")
    private String secretKey;

    public ProductResponse checkoutProduct(ProductDto productDto)
    {
        Stripe.apiKey=secretKey;
        SessionCreateParams.LineItem.PriceData.ProductData productData = SessionCreateParams.LineItem.PriceData.ProductData.builder()
                .setName(productDto.getName()).build();

        SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams.LineItem.PriceData.builder().setCurrency(productDto.getCurrency() != null ? productDto.getCurrency() : "CAD")
                .setUnitAmount(productDto.getAmount()).setProductData(productData).build();

        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder().setQuantity(productDto.getQuantity()).setPriceData(priceData).build();

        SessionCreateParams params = SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT).setSuccessUrl("http://localhost:8080/stripe/subscription/success").setCancelUrl("http://localhost:8080/stripe/subscription/cancel")
                .addLineItem(lineItem).build();

        Session session = null;
        try {
            session = Session.create(params);
            return ProductResponse.builder()
                    .status("SUCCESS")
                    .message("Payment Session Created")
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .build();
        } catch (StripeException exception) {
            return ProductResponse.builder()
                    .status("FAILED")
                    .message("Stripe error: " + exception.getMessage())
                    .sessionId(null)
                    .sessionUrl(null)
                    .build();
        }
    }
}
