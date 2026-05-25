package com.shopflow.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.user.PostgresTestContainerBase;
import com.shopflow.user.dto.UserAddressRequest;
import com.shopflow.user.entity.UserAddress;
import com.shopflow.user.repository.UserAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SuppressWarnings("null")
class UserAddressControllerIT extends PostgresTestContainerBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAddressRepository addressRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String BASE_URL = "/api/v1/users/me/addresses";

    @BeforeEach
    void clearDatabase() {
        addressRepository.deleteAll();
    }

    private UserAddressRequest buildRequest() {
        return UserAddressRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .streetAddress("456 Oak Ave")
                .city("Chicago")
                .state("IL")
                .zipCode("60601")
                .country("US")
                .phoneNumber("+1-555-0200")
                .isDefault(false)
                .build();
    }

    private UserAddress saveTestAddress(String userId, boolean isDefault) {
        return addressRepository.save(UserAddress.builder()
                .userId(userId)
                .firstName("Jane")
                .lastName("Smith")
                .streetAddress("456 Oak Ave")
                .city("Chicago")
                .state("IL")
                .zipCode("60601")
                .country("US")
                .phoneNumber("+1-555-0200")
                .isDefault(isDefault)
                .build());
    }

    @Test
    void getAllAddresses_withValidJwt_returnsOkWithEmptyList() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllAddresses_withNoJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAddress_withValidRequest_returnsCreated() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest());

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.city").value("Chicago"))
                .andExpect(jsonPath("$.isDefault").value(false));
    }

    @Test
    void createAddress_withMissingRequiredFields_returnsBadRequest() throws Exception {
        String invalidBody = objectMapper.writeValueAsString(
                UserAddressRequest.builder().firstName("Jane").build());

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void getDefaultAddress_whenDefaultExists_returnsOk() throws Exception {
        saveTestAddress(TEST_USER_ID, true);

        mockMvc.perform(get(BASE_URL + "/default")
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void getDefaultAddress_whenNoDefault_returnsNotFound() throws Exception {
        saveTestAddress(TEST_USER_ID, false);

        mockMvc.perform(get(BASE_URL + "/default")
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAddress_withValidRequest_returnsOkWithUpdatedCity() throws Exception {
        UserAddress saved = saveTestAddress(TEST_USER_ID, false);
        UserAddressRequest updated = buildRequest();
        updated.setCity("Naperville");
        String body = objectMapper.writeValueAsString(updated);

        mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Naperville"));
    }

    @Test
    void deleteAddress_withValidJwt_returnsNoContent() throws Exception {
        UserAddress saved = saveTestAddress(TEST_USER_ID, false);

        mockMvc.perform(delete(BASE_URL + "/" + saved.getId())
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAddress_onOtherUsersAddress_returnsForbidden() throws Exception {
        UserAddress otherUsersAddress = saveTestAddress("other-user-456", false);

        mockMvc.perform(delete(BASE_URL + "/" + otherUsersAddress.getId())
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID))))
                .andExpect(status().isForbidden());
    }
}
