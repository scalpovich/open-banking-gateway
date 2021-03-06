package de.adorsys.opba.fintech.impl.database.entities;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.opba.fintech.impl.tppclients.Consts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Slf4j
public class SessionEntity {
    @Id
    private String loginUserName;
    private String fintechUserId;
    private String password;
    private String xsrfToken;
    private String psuConsentSession;
    private UUID serviceSessionId;

    // TODO orphanRemoval should be true, but thatn deleting  fails. Dont know hot to
    // test with different transactions yet
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = false)
    private List<LoginEntity> logins = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "session_cookie_name")),
            @AttributeOverride(name = "value", column = @Column(name = "session_cookie_value"))
    })
    private CookieEntity sessionCookie;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "redirect_cookie_name")),
            @AttributeOverride(name = "value", column = @Column(name = "redirect_cookie_value"))
    })
    private CookieEntity redirectCookie;

    @SneakyThrows
    public static String createSessionCookieValue(String fintechUserId, String xsrfToken) {
        ObjectMapper mapper = new ObjectMapper();
        return URLEncoder.encode(mapper.writeValueAsString(new SessionCookieValue(fintechUserId, xsrfToken.hashCode())), JsonEncoding.UTF8.getJavaName());
    }

    @SneakyThrows
    public static void validateSessionCookieValue(String sessionCookieValueString, String xsrfToken) {
        String decode = URLDecoder.decode(sessionCookieValueString, JsonEncoding.UTF8.getJavaName());
        ObjectMapper mapper = new ObjectMapper();
        SessionCookieValue sessionCookieValue = mapper.readValue(decode, SessionCookieValue.class);
        if (sessionCookieValue.getHashedXsrfToken() == xsrfToken.hashCode()) {
            log.info("validation of token ok");
            return;
        }
        throw new RuntimeException("session cookie not valid " +  decode);
    }

    public SessionEntity setSessionCookieValue(String value) {
        log.info("session cookie will be {}", value);
        sessionCookie = CookieEntity.builder().name(Consts.COOKIE_SESSION_COOKIE_NAME).value(value).build();
        return this;
    }

    public void addLogin(OffsetDateTime time) {
        if (logins == null) {
            logins = new ArrayList<>();
        }
        logins.add(LoginEntity.builder().loginTime(time).build());
    }

    public OffsetDateTime getLastLogin() {
        if (logins.isEmpty()) {
            throw new RuntimeException("PROGRAMMING ERROR: at least one successful login must be known yet");
        }
        int size = logins.size();
        if (size == 1) {
            return null;
        }
        return logins.get(size - 1).getLoginTime();
    }

    @Data
    public static class SessionCookieValue {
        private final String fintechUserId;
        private final int hashedXsrfToken;
    }
}
