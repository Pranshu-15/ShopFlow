package com.shopflow.user.service;

import com.shopflow.user.dto.UserAddressRequest;
import com.shopflow.user.dto.UserAddressResponse;
import com.shopflow.user.entity.UserAddress;
import com.shopflow.user.repository.UserAddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository addressRepository;

    @InjectMocks
    private UserAddressService addressService;

    private static final String TEST_USER_ID = "test-user-123";

    private UserAddress buildAddress(Long id, String userId, boolean isDefault) {
        return UserAddress.builder()
                .id(id)
                .userId(userId)
                .firstName("John")
                .lastName("Doe")
                .streetAddress("123 Main St")
                .city("Springfield")
                .state("IL")
                .zipCode("62701")
                .country("US")
                .phoneNumber("+1-555-0100")
                .isDefault(isDefault)
                .build();
    }

    private UserAddressRequest buildRequest(boolean isDefault) {
        return UserAddressRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .streetAddress("123 Main St")
                .city("Springfield")
                .state("IL")
                .zipCode("62701")
                .country("US")
                .phoneNumber("+1-555-0100")
                .isDefault(isDefault)
                .build();
    }

    @Test
    void getUserAddresses_whenAddressesExist_returnsMappedList() {
        when(addressRepository.findByUserId(TEST_USER_ID))
                .thenReturn(List.of(
                        buildAddress(1L, TEST_USER_ID, false),
                        buildAddress(2L, TEST_USER_ID, true)));

        List<UserAddressResponse> result = addressService.getUserAddresses(TEST_USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getUserId().equals(TEST_USER_ID));
    }

    @Test
    void getUserAddresses_whenNoAddresses_returnsEmptyList() {
        when(addressRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of());

        assertThat(addressService.getUserAddresses(TEST_USER_ID)).isEmpty();
    }

    @Test
    void getDefaultAddress_whenExists_returnsResponse() {
        UserAddress address = buildAddress(1L, TEST_USER_ID, true);
        when(addressRepository.findByUserIdAndIsDefaultTrue(TEST_USER_ID))
                .thenReturn(Optional.of(address));

        UserAddressResponse response = addressService.getDefaultAddress(TEST_USER_ID);

        assertThat(response.isDefault()).isTrue();
        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    void getDefaultAddress_whenNotFound_throwsNotFoundException() {
        when(addressRepository.findByUserIdAndIsDefaultTrue(TEST_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getDefaultAddress(TEST_USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createAddress_withIsDefaultTrue_resetsExistingDefault() {
        UserAddress oldDefault = buildAddress(1L, TEST_USER_ID, true);
        when(addressRepository.findByUserIdAndIsDefaultTrue(TEST_USER_ID))
                .thenReturn(Optional.of(oldDefault));
        when(addressRepository.save(any(UserAddress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        addressService.createAddress(TEST_USER_ID, buildRequest(true));

        // save called twice: once to reset old default, once to persist new address
        verify(addressRepository, times(2)).save(any(UserAddress.class));
    }

    @Test
    void createAddress_withIsDefaultFalse_savesWithoutResettingDefault() {
        when(addressRepository.save(any(UserAddress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserAddressResponse response = addressService.createAddress(TEST_USER_ID, buildRequest(false));

        assertThat(response.isDefault()).isFalse();
        verify(addressRepository, never()).findByUserIdAndIsDefaultTrue(any());
    }

    @Test
    void updateAddress_whenFound_updatesFieldsAndReturns() {
        UserAddress existing = buildAddress(1L, TEST_USER_ID, false);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(addressRepository.save(any(UserAddress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserAddressRequest request = buildRequest(false);
        request.setCity("Chicago");

        UserAddressResponse response = addressService.updateAddress(TEST_USER_ID, 1L, request);

        assertThat(response.getCity()).isEqualTo("Chicago");
    }

    @Test
    void updateAddress_whenNotFound_throwsNotFoundException() {
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(TEST_USER_ID, 99L, buildRequest(false)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAddress_whenAddressBelongsToDifferentUser_throwsForbiddenException() {
        UserAddress otherUsersAddress = buildAddress(1L, "other-user-456", false);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(otherUsersAddress));

        assertThatThrownBy(() -> addressService.updateAddress(TEST_USER_ID, 1L, buildRequest(false)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteAddress_whenFound_deletesSuccessfully() {
        UserAddress address = buildAddress(1L, TEST_USER_ID, false);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));

        addressService.deleteAddress(TEST_USER_ID, 1L);

        verify(addressRepository).delete(address);
    }

    @Test
    void deleteAddress_whenNotFound_throwsNotFoundException() {
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.deleteAddress(TEST_USER_ID, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteAddress_whenAddressBelongsToDifferentUser_throwsForbiddenException() {
        UserAddress otherUsersAddress = buildAddress(1L, "other-user-456", false);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(otherUsersAddress));

        assertThatThrownBy(() -> addressService.deleteAddress(TEST_USER_ID, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
