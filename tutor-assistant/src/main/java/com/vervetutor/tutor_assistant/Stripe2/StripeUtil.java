package com.vervetutor.tutor_assistant.Stripe2;

import com.stripe.model.CustomerCollection;
import com.stripe.param.CustomerListParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;

import java.util.HashMap;
import java.util.Map;

@Component
public class StripeUtil {

    @Value("${stripe.apikey}")
    String stripeKey;

    public CustomerData getCustomer(String id) throws StripeException {
        Stripe.apiKey = stripeKey;

        Customer customer = Customer.retrieve(id);
        CustomerData data = setCustomerData(customer);
        return data;
    }

    public CustomerData setCustomerData(Customer customer) {
        CustomerData customerData = new CustomerData();
        customerData.setCustomerId(customer.getId());
        customerData.setName(customer.getName());
        customerData.setEmail(customer.getEmail());

        return customerData;
    }

    public String idByEmail(String email) throws StripeException {
        Stripe.apiKey = stripeKey;

        // 1️⃣ Build list params
        CustomerListParams params = CustomerListParams.builder()
                .setEmail(email)
                .setLimit(1L)
                .build();

        // 2️⃣ Call Stripe API
        CustomerCollection customers = Customer.list(params);

        // 3️⃣ Check if found
        Customer customer;
        if (customers.getData().isEmpty()) {
            // Not found → create new
            Map<String, Object> createParams = new HashMap<>();
            createParams.put("email", email);
            customer = Customer.create(createParams);
        } else {
            // Found → use first
            customer = customers.getData().get(0);
        }

        System.out.println("Resolved customer ID: " + customer.getId());
        return customer.getId();
    }

}