package com.vervetutor.tutor_assistant.User;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vervetutor.tutor_assistant.Registration.Token.ConfirmationToken;
import com.vervetutor.tutor_assistant.Tutor.Tutor;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Getter
@Setter
@EqualsAndHashCode
@Table(name = "users")
public class User implements UserDetails {

    public User(String firstName, String lastName, String email, String password, String phone, String profilePhoto, Tutor tutor, AppUserRole appUserRole, Boolean locked, Boolean enabled, Boolean active, String stripeId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.profilePhoto = profilePhoto;
        this.tutor = tutor;
        this.appUserRole = appUserRole;
        this.locked = locked;
        this.enabled = false;
        this.active = active;
        this.stripeId = stripeId;
        this.subscriptionTier = "STARTER"; // Default to starter tier
    }

    @Id
    @SequenceGenerator(name = "student_sequence", sequenceName = "student_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "student_sequence")
    private Long id;

    @Column
    private String stripeId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String profilePhoto; // Store as base64 string or file path

    @Column
    private String address;

    @OneToOne
    @JoinColumn(name = "tutor_id", referencedColumnName = "id")
    private Tutor tutor;

    @Enumerated(EnumType.STRING)
    private AppUserRole appUserRole;

    @Column(nullable = false)
    private Boolean locked = false;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(nullable = false)
    private Boolean active = false;

    @Column(nullable = false)
    private Boolean notifications = true;

    // New subscription-related fields
    @Column(nullable = false)
    @Builder.Default
    private String subscriptionTier = "STARTER"; // STARTER, PRO, etc.

    @Column
    private LocalDateTime lastPaymentDate;

    public User(String firstName, String lastName, String email, String password, AppUserRole appUserRole) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.appUserRole = appUserRole;
        this.enabled = false;
        this.subscriptionTier = "STARTER"; // Default to starter tier
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(appUserRole.name());
        return Collections.singletonList(authority);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}