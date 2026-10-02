package com.vervetutor.tutor_assistant.User;

import com.vervetutor.tutor_assistant.Registration.Token.ConfirmationToken;
import com.vervetutor.tutor_assistant.Registration.Token.ConfirmationTokenService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private ConfirmationTokenService confirmationTokenService;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    User createUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(existingUser -> {
            /*if (updatedUser.getUsername() != null) {
                existingUser.setUsername(updatedUser.getUsername());
            }*/
            if (updatedUser.getPassword() != null) {
                existingUser.setPassword(updatedUser.getPassword());
            }
            if (updatedUser.getActive() != null) {
                existingUser.setActive(updatedUser.getActive());
            }
            if (updatedUser.getTutor() != null) {
                existingUser.setTutor(updatedUser.getTutor());
            }
            if (updatedUser.getEnabled() != null) {
                existingUser.setEnabled(updatedUser.getEnabled());
            }
            if (updatedUser.getLocked() != null) {
                existingUser.setLocked(updatedUser.getLocked());
            }
            if(updatedUser.getStripeId() != null){
                existingUser.setStripeId(updatedUser.getStripeId());
            }

            return userRepository.save(existingUser);
        }).orElseThrow(() -> new RuntimeException("User with ID " + id + " not found"));
    }

    public String signUpUser(User user){
        boolean userExists = userRepository.findByEmail(user.getEmail()).isPresent();
        if (userExists)
        {
            throw new IllegalStateException("Email alr taken");
        }
        String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(user, LocalDateTime.now(), LocalDateTime.now().plusMinutes(15), token);
        confirmationTokenService.saveConfirmationToken(confirmationToken);

        return token;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(""));
    }

    public Optional<User> findByEmail(String email)
    {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void enableUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + email + " not found"));
        user.setEnabled(true);
        userRepository.save(user);
    }

    public void saveStripeCustomerId(Long dbUserId, String id) {
        userRepository.findById(dbUserId).ifPresent(user -> {
            user.setStripeId(id);
            userRepository.save(user);
        });
    }

    public Optional<User> findByStripe(String account) {
        return userRepository.findByStripeId(account);
    }

    public User getUserByTutorId(Long tutorId)
    {
        return userRepository.findByTutorId(tutorId).orElseThrow(() -> new RuntimeException("user not found for id: " + tutorId));
    }

    // NEW METHODS FOR FRONTEND INTEGRATION

    /**
     * Update user profile information
     */
    public User updateUserProfile(Long id, UserProfileUpdateRequest request) {
        return userRepository.findById(id).map(existingUser -> {
            if (request.getFirstName() != null) {
                existingUser.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                existingUser.setLastName(request.getLastName());
            }
            if (request.getPhone() != null) {
                existingUser.setPhone(request.getPhone());
            }
            if (request.getAddress() != null) {
                existingUser.setAddress(request.getAddress());
            }
            if (request.getNotifications() != null) {
                existingUser.setNotifications(request.getNotifications());
            }

            return userRepository.save(existingUser);
        }).orElseThrow(() -> new RuntimeException("User with ID " + id + " not found"));
    }

    /**
     * Change user password
     */
    public void changePassword(Long id, String newPassword) {
        userRepository.findById(id).ifPresentOrElse(user -> {
            String encodedPassword = bCryptPasswordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            userRepository.save(user);
        }, () -> {
            throw new RuntimeException("User with ID " + id + " not found");
        });
    }
}