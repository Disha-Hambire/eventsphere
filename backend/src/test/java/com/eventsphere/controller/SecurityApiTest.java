package com.eventsphere.controller;

import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import com.eventsphere.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end HTTP tests of authentication and role-based access. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        users.save(new User("Pat", "pat@test.com", encoder.encode("secret123"), Role.PARTICIPANT));
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void loginReturnsTokenThatAuthenticatesRequests() throws Exception {
        String token = login("pat@test.com", "secret123");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pat@test.com"))
                .andExpect(jsonPath("$.role").value("PARTICIPANT"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "pat@test.com", "password", "nope"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        mvc.perform(get("/api/events")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/public/stats")).andExpect(status().isOk());
    }

    @Test
    void participantsCannotCreateEvents() throws Exception {
        String token = login("pat@test.com", "secret123");
        String event = """
                {"title":"Hack","category":"COLLEGE","venue":"Hall","capacity":10,
                 "startDateTime":"2030-01-10T09:00","endDateTime":"2030-01-10T18:00",
                 "registrationDeadline":"2030-01-09T09:00"}""";
        mvc.perform(post("/api/events").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isForbidden());
    }

    @Test
    void signUpCreatesParticipantAndValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("fullName", "New", "email", "bad", "password", "123"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("fullName", "New User", "email", "new@test.com", "password", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PARTICIPANT"));
    }
}
