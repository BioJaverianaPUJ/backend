package com.biojaveriana.api.user;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.user.exceptions.UserNotFoundException;
import com.biojaveriana.api.user.exceptions.UsernameAlreadyExistsException;
import com.biojaveriana.api.user.dto.UserUpdateRequest;
import com.biojaveriana.api.user.dto.MeUserUpdateRequest;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException(username));
    }

    public User updateById(UUID id, UserUpdateRequest dto) {
        var user = findById(id);
        if (dto.firstName() != null) {
            user.setFirstName(dto.firstName());
        }

        if (dto.lastName() != null) {
            user.setLastName(dto.lastName());
        }

        if (dto.username() != null) {
            if (!dto.username().equals(user.getUsername())) {
                if (userRepository.existsByUsername(dto.username())) {
                    throw new UsernameAlreadyExistsException(dto.username());
                }
                user.setUsername(dto.username());
            }
        }

        if (dto.role() != null) {
            user.setRole(dto.role());
        }

        if (dto.gender() != null) {
            user.setGender(dto.gender());
        }

        if (dto.enabled() != null) {
            user.setDeactivated(!dto.enabled());
        }

        return userRepository.save(user);
    }

    public void deleteById(UUID id) {
        var user = findById(id);
        userRepository.delete(user);
    }


    public User disableUser(UUID id) {
        var user = findById(id);
        user.setDeactivated(true);
        return userRepository.save(user);
    }

    public User updateMe(User user, MeUserUpdateRequest dto) {
        if (dto.firstName() != null) {
            user.setFirstName(dto.firstName());
        }

        if (dto.lastName() != null) {
            user.setLastName(dto.lastName());
        }

        if (dto.username() != null) {
            if (!dto.username().equals(user.getUsername())) {
                if (userRepository.existsByUsername(dto.username())) {
                    throw new UsernameAlreadyExistsException(dto.username());
                }

                user.setUsername(dto.username());
            }
        }

        if (dto.gender() != null) {
            user.setGender(dto.gender());
        }

        return userRepository.save(user);
    }
}
