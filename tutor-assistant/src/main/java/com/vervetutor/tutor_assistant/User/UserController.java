package com.vervetutor.tutor_assistant.User;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.SubscriptionItem;
import com.vervetutor.tutor_assistant.Config.JwtService;
import com.vervetutor.tutor_assistant.Mappers.Mapper;
import com.vervetutor.tutor_assistant.Mappers.UserMapper;
import com.vervetutor.tutor_assistant.Stripe2.SubscriptionDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class UserController
{
    private UserService userService;
    private Mapper<User, UserDto> userMapper;
    private BCryptPasswordEncoder passwordEncoder;
    private JwtService jwtService;

    @Value("${stripe.apikey}")
    String stripeKey;

    @Value("${stripe.pro}")
    private String stripeProPrice;

    @Value("${stripe.starter}")
    private String stripeStarterPrice;

    @Autowired
    public UserController(UserService userService, UserMapper userMapper, BCryptPasswordEncoder passwordEncoder)
    {
        this.userService = userService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

//    @PostMapping(path = "/users")
//    public ResponseEntity<UserDto> createUser(@RequestBody UserDto user)
//    {
//        User userEntity = userMapper.mapFrom(user);
//        User savedUser = userService.createUser(userEntity);
//        return new ResponseEntity<>(userMapper.mapTo(savedUser), HttpStatus.CREATED);
//    }

//    @PatchMapping("/users/{id}")
//    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
//        User updatedEntity = userMapper.mapFrom(userDto);
//        User updatedUser = userService.updateUser(id, updatedEntity);
//        return new ResponseEntity<>(userMapper.mapTo(updatedUser), HttpStatus.OK);
//    }

//    @DeleteMapping("/users/{id}")
//    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
//        Optional<User> user = userService.getUserById(id);
//
//        if (user.isPresent()) {
//            userService.deleteUser(id);
//            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//        } else {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//    }

//    @GetMapping("/users")
//    public List<UserDto> findAllUsers()
//    {
//        List<User> users = userService.getAllUsers();
//        return users.stream().map(userMapper::mapTo).collect(Collectors.toList());
//    }

//    @GetMapping("/users/id/{id}")
//    public ResponseEntity<UserDto> getUser(@PathVariable("id") Long id)
//    {
//        Optional<User> savedUser = userService.getUserById(id);
//        return savedUser.map(user -> {
//            UserDto userDto = userMapper.mapTo(user);
//            return new ResponseEntity<>(userDto, HttpStatus.OK);
//        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
//    }

    @GetMapping("/users/email/me")
    public ResponseEntity<Long> getCurrentUserTutorId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<User> savedUser = userService.findByEmail(userEmail);

            if (savedUser.isEmpty() || savedUser.get().getTutor() == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(savedUser.get().getTutor().getId());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/users/byEmail/me")
    public ResponseEntity<UserDto> getCurrentUserLimited(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<User> savedUser = userService.findByEmail(userEmail);

            if (savedUser.isEmpty() || savedUser.get().getTutor() == null) {
                return ResponseEntity.notFound().build();
            }

            UserDto userDto = userMapper.mapTo(savedUser.get());
            userDto.setPassword(null);
            userDto.setStripeId(null);
            return ResponseEntity.ok(userDto);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/profile/cancel")
    public ResponseEntity<Boolean> cancelMembership() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            String userEmail = authentication.getName();
            Optional<User> userOpt = userService.findByEmail(userEmail);

            if (userOpt.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            User user = userOpt.get();
            String customerId = user.getStripeId();

            if (customerId == null || customerId.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Set Stripe API key (you can also do this globally in a config)
            Stripe.apiKey = stripeKey;

            // Get the most recent subscription for this customer
            Map<String, Object> params = new HashMap<>();
            params.put("customer", customerId);
            params.put("limit", 1);  // get most recent subscription

            SubscriptionCollection subscriptions = Subscription.list(params);

            if (subscriptions.getData().isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND); // No active subscription
            }

            Subscription subscription = subscriptions.getData().get(0);

            Subscription updatedSubscription = Subscription.retrieve(subscription.getId());

            Map<String, Object> updateParams = new HashMap<>();
            updateParams.put("cancel_at_period_end", true);

            updatedSubscription = updatedSubscription.update(updateParams);

            return new ResponseEntity<>(true, HttpStatus.OK);

        } catch (StripeException e) {
            e.printStackTrace();
            return new ResponseEntity<>(false, HttpStatus.BAD_GATEWAY); // Stripe failure
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/users/profile/subscription")
    public ResponseEntity<Object> membershipStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
            }

            String userEmail = authentication.getName();
            Optional<User> userOpt = userService.findByEmail(userEmail);

            if (userOpt.isEmpty()) {
                return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOpt.get();
            String customerId = user.getStripeId();

            if (customerId == null || customerId.isEmpty()) {
                return new ResponseEntity<>("Customer ID not found", HttpStatus.NOT_FOUND);
            }

            Stripe.apiKey = stripeKey;

            Map<String, Object> params = new HashMap<>();
            params.put("customer", customerId);
            params.put("limit", 1); // Get the most recent subscription

            SubscriptionCollection subscriptions = Subscription.list(params);

            if (subscriptions.getData().isEmpty()) {
                return new ResponseEntity<>("No subscriptions found", HttpStatus.OK);
            }

            Subscription subscription = subscriptions.getData().get(0);

            // Get price item
            SubscriptionItem item = subscription.getItems().getData().get(0);
            Price price = item.getPrice();

            // Determine plan name from priceId
            String planName;

            if (price.getId().equals(stripeStarterPrice)) {
                planName = "Verve Starter";
            } else if (price.getId().equals(stripeProPrice)) {
                planName = "Verve Pro";
            } else {
                planName = "Unknown Plan";
            }

            // Calculate days until repay/trial end properly
            Integer daysUntilRepay = null;
            long currentTimeSeconds = System.currentTimeMillis() / 1000;

            if (subscription.getTrialEnd() != null && subscription.getTrialEnd() > currentTimeSeconds) {
                // During trial period - calculate days until trial ends
                long trialEndSeconds = subscription.getTrialEnd();
                long secondsRemaining = trialEndSeconds - currentTimeSeconds;
                daysUntilRepay = (int) Math.max(0, Math.ceil(secondsRemaining / (double)(24 * 60 * 60)));
            } else if (item.getCurrentPeriodEnd() != null) {
                // Regular billing cycle - calculate days until next billing
                long periodEndSeconds = item.getCurrentPeriodEnd();
                long secondsRemaining = periodEndSeconds - currentTimeSeconds;
                daysUntilRepay = (int) Math.max(0, Math.ceil(secondsRemaining / (double)(24 * 60 * 60)));
            }

            // Build DTO with corrected daysUntilRepay calculation
            SubscriptionDto dto = SubscriptionDto.builder()
                    .id(planName)
                    .status(subscription.getStatus())
                    .currentPeriodStart(subscription.getStartDate()) // subscription start timestamp
                    .daysUntilRepay(Long.valueOf(daysUntilRepay)) // properly calculated days remaining
                    .priceAmount(price.getUnitAmount() != null ? price.getUnitAmount() / 100.0 : 0.0) // price in dollars
                    .willCancel(subscription.getCancelAtPeriodEnd()) // true if subscription set to cancel at period end
                    .build();

            return new ResponseEntity<>(dto, HttpStatus.OK);

        } catch (StripeException e) {
            System.err.println("Stripe error: " + e.getMessage());
            return new ResponseEntity<>("Stripe service error", HttpStatus.BAD_GATEWAY);
        } catch (Exception e) {
            System.err.println("Internal error: " + e.getMessage());
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // NEW ENDPOINTS FOR FRONTEND INTEGRATION

    /**
     * Get current user profile (requires authentication)
     */
    @GetMapping("/users/profile/current")
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            String userEmail = authentication.getName();
            Optional<User> user = userService.findByEmail(userEmail);

            if (user.isPresent()) {
                UserDto userDto = userMapper.mapTo(user.get());
                return new ResponseEntity<>(userDto, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/users/profile/{id}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable Long id,
            @RequestBody UserProfileUpdateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<User> requestingUser = userService.findByEmail(userEmail);

            if (requestingUser.isEmpty() || !requestingUser.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not allowed to update this profile."));
            }

            User updatedUser = userService.updateUserProfile(id, request);
            UserDto userDto = userMapper.mapTo(updatedUser);
            return ResponseEntity.ok(userDto);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update profile"));
        }
    }


    @PatchMapping("/users/password/{id}")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @RequestBody PasswordChangeRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            Optional<User> requestingUser = userService.findByEmail(userEmail);

            if (requestingUser.isEmpty() || !requestingUser.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not allowed to change this password."));
            }

            // Validate user exists and current password is correct
            User user = requestingUser.get();
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Current password is incorrect"));
            }

            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "New password must be at least 6 characters long"));
            }

            userService.changePassword(id, request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to change password"));
        }
    }
}