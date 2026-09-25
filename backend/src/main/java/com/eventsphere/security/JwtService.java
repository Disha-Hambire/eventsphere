package com.eventsphere.security;

import com.eventsphere.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long expiryHours;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.expiry-hours}") long expiryHours) {
        this.encoder = encoder;
        this.expiryHours = expiryHours;
    }

    /** Subject = user id; "roles" drives authorisation; name/email are for convenience only. */
    public String issueToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("eventsphere")
                .issuedAt(now)
                .expiresAt(now.plus(expiryHours, ChronoUnit.HOURS))
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
