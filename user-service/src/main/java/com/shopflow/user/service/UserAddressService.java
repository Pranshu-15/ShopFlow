package com.shopflow.user.service;

import com.shopflow.user.dto.UserAddressRequest;
import com.shopflow.user.dto.UserAddressResponse;
import com.shopflow.user.entity.UserAddress;
import com.shopflow.user.repository.UserAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserAddressService {

    private final UserAddressRepository addressRepository;

    @Autowired
    public UserAddressService(UserAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<UserAddressResponse> getUserAddresses(String userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserAddressResponse getDefaultAddress(String userId) {
        return addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default address not found"));
    }

    @Transactional
    public UserAddressResponse createAddress(String userId, UserAddressRequest request) {
        if (request.isDefault()) {
            resetDefaultAddress(userId);
        }

        UserAddress address = UserAddress.builder()
                .userId(userId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .streetAddress(request.getStreetAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .phoneNumber(request.getPhoneNumber())
                .isDefault(request.isDefault())
                .build();

        UserAddress savedAddress = addressRepository.save(address);
        return mapToResponse(savedAddress);
    }

    @Transactional
    public UserAddressResponse updateAddress(String userId, Long addressId, UserAddressRequest request) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        if (!address.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: unauthorized operation");
        }

        if (request.isDefault() && !address.isDefault()) {
            resetDefaultAddress(userId);
        }

        address.setFirstName(request.getFirstName());
        address.setLastName(request.getLastName());
        address.setStreetAddress(request.getStreetAddress());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setDefault(request.isDefault());

        UserAddress updatedAddress = addressRepository.save(address);
        return mapToResponse(updatedAddress);
    }

    @Transactional
    public void deleteAddress(String userId, Long addressId) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        if (!address.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: unauthorized operation");
        }

        addressRepository.delete(address);
    }

    private void resetDefaultAddress(String userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(address -> {
                    address.setDefault(false);
                    addressRepository.save(address);
                });
    }

    private UserAddressResponse mapToResponse(UserAddress address) {
        return UserAddressResponse.builder()
                .id(address.getId())
                .userId(address.getUserId())
                .firstName(address.getFirstName())
                .lastName(address.getLastName())
                .streetAddress(address.getStreetAddress())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .phoneNumber(address.getPhoneNumber())
                .isDefault(address.isDefault())
                .build();
    }
}
