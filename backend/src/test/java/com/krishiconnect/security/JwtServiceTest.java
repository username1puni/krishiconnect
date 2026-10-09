package com.krishiconnect.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.User;

class JwtServiceTest {

    private final JwtService jwt = new JwtService("a-test-secret-that-is-long-enough-1234567890", 60);

    @Test
    void tokenCarriesTheUserId() {
        User user = new User("Ramesh", "9900000001", null, "hash", Role.SEEKER);
        user.setId(42L);
        String token = jwt.createToken(new AuthUser(user));
        assertThat(jwt.validateAndGetSubject(token)).isEqualTo("42");
    }

    @Test
    void tamperedTokenIsRejected() {
        User user = new User("Ramesh", "9900000001", null, "hash", Role.SEEKER);
        user.setId(42L);
        String token = jwt.createToken(new AuthUser(user));
        String tampered = token.substring(0, token.length() - 4) + "abcd";
        assertThat(jwt.validateAndGetSubject(tampered)).isNull();
        assertThat(jwt.validateAndGetSubject("not-a-token")).isNull();
    }

    @Test
    void tokenFromAnotherServerIsRejected() {
        JwtService other = new JwtService("some-other-secret-that-is-long-enough-xyz", 60);
        User user = new User("Ramesh", "9900000001", null, "hash", Role.SEEKER);
        user.setId(7L);
        assertThat(jwt.validateAndGetSubject(other.createToken(new AuthUser(user)))).isNull();
    }

    @Test
    void shortSecretIsRefused() {
        assertThatThrownBy(() -> new JwtService("short", 60)).isInstanceOf(IllegalStateException.class);
    }
}
