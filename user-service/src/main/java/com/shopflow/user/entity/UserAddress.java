package com.shopflow.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotBlank(message = "User ID is required")
    private String userId;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    private String lastName;

    @Column(name = "street_address", nullable = false)
    @NotBlank(message = "Street address is required")
    private String streetAddress;

    @Column(nullable = false)
    @NotBlank(message = "City is required")
    private String city;

    @Column(nullable = false)
    @NotBlank(message = "State is required")
    private String state;

    @Column(name = "zip_code", nullable = false)
    @NotBlank(message = "Zip code is required")
    @Size(min = 3, max = 10, message = "Zip code must be between 3 and 10 characters")
    private String zipCode;

    @Column(nullable = false)
    @NotBlank(message = "Country is required")
    private String country;

    @Column(name = "phone_number", nullable = false)
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
}
