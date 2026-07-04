package com.filecabinet.user.service;

import com.filecabinet.shared.exception.ServiceExceptions;
import com.filecabinet.user.model.Role;
import com.filecabinet.user.model.User;
import com.filecabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String username, String email, String rawPassword) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ServiceExceptions.DuplicateException("Username already taken: " + username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ServiceExceptions.DuplicateException("Email already registered: " + email);
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.CLERK)
                .createdOn(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    public Optional<User> login(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ServiceExceptions.NotFoundException("User not found: " + id));
    }

    public User updateEmail(UUID id, String email) {
        User user = findById(id);
        user.setEmail(email);
        return userRepository.save(user);
    }

    public User changePassword(UUID id, String newRawPassword) {
        User user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        return userRepository.save(user);
    }

    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ServiceExceptions.NotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }
}
