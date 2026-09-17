package com.indiratrading.service;

import com.indiratrading.model.User;
import com.indiratrading.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired private UserRepository userRepo;

    public Optional<User> getUser(String userId) {
        return userRepo.findByUserId(userId);
    }

    public List<String> getClientScope(String userId) {
        return userRepo.findByUserId(userId)
            .map(u -> {
                String scope = u.getClientScope();
                if (scope == null || scope.trim().isEmpty()) return Collections.<String>emptyList();
                return Arrays.asList(scope.split(","));
            })
            .orElse(Collections.<String>emptyList());
    }

    public boolean canAccessClient(String userId, String clientId) {
        return userRepo.findByUserId(userId)
            .map(u -> {
                String scope = u.getClientScope();
                if (scope == null || scope.isEmpty()) return true; // auditor sees all
                return Arrays.asList(scope.split(",")).contains(clientId);
            })
            .orElse(false);
    }

    public boolean hasRole(String userId, String requiredRole) {
        return userRepo.findByUserId(userId)
            .map(u -> u.getRole().equalsIgnoreCase(requiredRole))
            .orElse(false);
    }
}
