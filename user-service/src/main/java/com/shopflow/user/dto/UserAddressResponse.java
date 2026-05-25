package com.shopflow.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddressResponse {

    private Long id;
    private String userId;
    private String firstName;
    private String lastName;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phoneNumber;
    @Getter(onMethod_ = {@JsonProperty("isDefault")})
    private boolean isDefault;
}
