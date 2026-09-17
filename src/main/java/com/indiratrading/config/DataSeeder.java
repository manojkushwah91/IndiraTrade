package com.indiratrading.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indiratrading.model.User;
import com.indiratrading.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired private UserRepository userRepo;
    @Autowired private ObjectMapper objectMapper;

    @Value("classpath:data/access_scopes.json")
    private Resource accessScopesResource;

    @Override
    public void run(String... args) throws Exception {
        if (userRepo.count() > 0) return;

        try (InputStream is = accessScopesResource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode users = root.get("users");

            long idCounter = 1;
            for (JsonNode userNode : users) {
                String userId = userNode.get("user_id").asText();

                Optional<User> existing = userRepo.findByUserId(userId);
                if (existing.isPresent()) continue;

                String name = userNode.get("name").asText();
                String role = userNode.get("role").asText();
                String clientScope = userNode.get("client_scope").toString()
                    .replaceAll("[\\[\\]\"]", "");

                User user = new User();
                user.setId(idCounter++);
                user.setUserId(userId);
                user.setName(name);
                user.setRole(role);
                user.setClientScope(clientScope);
                userRepo.save(user);
            }
        }
        System.out.println("Seeded " + userRepo.count() + " users from access_scopes.json");
    }
}
