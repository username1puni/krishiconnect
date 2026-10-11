package com.krishiconnect.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.User;
import com.krishiconnect.support.IntegrationTest;
import com.krishiconnect.support.TestData;

class AuthAndApiIT extends IntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void registerEachRoleAndLogInWithPhoneOrEmail() throws Exception {
        for (String role : new String[] {"SEEKER", "PROVIDER", "SPECIALIST"}) {
            String phone = switch (role) {
                case "SEEKER" -> "9811111111";
                case "PROVIDER" -> "9822222222";
                default -> "9833333333";
            };
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                            "fullName", "Test " + role, "phone", phone, "email", role.toLowerCase() + "@test.in",
                            "password", "secret123", "role", role, "village", "Mandya",
                            "latitude", 12.52, "longitude", 76.89, "preferredLang", "kn"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.user.role").value(role))
                    .andExpect(jsonPath("$.user.preferredLang").value("kn"))
                    // only farmers are trusted straight away
                    .andExpect(jsonPath("$.user.verified").value(role.equals("SEEKER")));
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"9811111111\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"PROVIDER@test.in\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("PROVIDER"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"9811111111\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationIsValidated() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "fullName", "X", "phone", "12345", "password", "secret123", "role", "SEEKER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.phone").exists());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "fullName", "X", "phone", "9844444444", "password", "secret123", "role", "ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rolesAreEnforced() throws Exception {
        User farmer = data.seeker("Farmer");
        String token = token(farmer.getPhone());

        mvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/equipment/mine").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        // browsing is public
        mvc.perform(get("/api/equipment")).andExpect(status().isOk());
        mvc.perform(get("/api/recommendations?month=11")).andExpect(status().isOk());
        mvc.perform(get("/api/places?q=Man")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Mandya"));
    }

    @Test
    void blockedUserLosesAccessImmediately() throws Exception {
        User farmer = data.seeker("Farmer");
        String token = token(farmer.getPhone());
        String admin = adminToken();
        mvc.perform(patch("/api/admin/users/" + farmer.getId() + "/enabled").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void bookingOverHttpAnswers409ForTheSecondFarmer() throws Exception {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User a = data.seeker("A");
        User b = data.seeker("B");
        String body = json.writeValueAsString(data.equipmentBooking(tractor.getId(), data.day(2, 6), data.day(2, 12)));

        mvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token(a.getPhone()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token(b.getPhone()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Swaraj 744 FE is already booked for these dates"));
    }

    @Test
    void providerListingNeedsApprovalUntilVerified() throws Exception {
        User newcomer = data.provider("New provider");
        mvc.perform(patch("/api/admin/users/" + newcomer.getId() + "/verify").header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":false}")).andExpect(status().isOk());
        String token = token(newcomer.getPhone());
        String listing = json.writeValueAsString(Map.of("title", "Eicher 380", "category", "TRACTOR",
                "pricePerHour", 800, "village", "Maddur", "latitude", 12.58, "longitude", 77.04));

        JsonNode created = json.readTree(mvc.perform(post("/api/equipment").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(listing))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moderationStatus").value("PENDING"))
                .andReturn().getResponse().getContentAsString());
        long id = created.get("id").asLong();
        mvc.perform(get("/api/equipment/" + id)).andExpect(status().isNotFound());

        mvc.perform(patch("/api/admin/equipment/" + id + "/moderation").header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/equipment/" + id)).andExpect(status().isOk());
        assertThat(data.count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'LISTING_APPROVED'",
                newcomer.getId())).isEqualTo(1);
    }

    @Test
    void languagePreferenceIsSaved() throws Exception {
        User farmer = data.seeker("Farmer");
        String token = token(farmer.getPhone());
        mvc.perform(put("/api/users/me/language").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"lang\":\"hi\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.preferredLang").value("hi"));
    }

    @Test
    void adminDashboardAnswers() throws Exception {
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersByRole.ADMIN").value(1))
                .andExpect(jsonPath("$.routing").exists());
    }

    private String token(String identifier) throws Exception {
        return login(identifier, TestData.PASSWORD);
    }

    private String adminToken() throws Exception {
        return login("admin@test.local", "admin-password");
    }

    private String login(String identifier, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("identifier", identifier, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
