package com.minthive.media;

import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.config.JwtConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;

/** Purpose-separated HMAC capabilities, NOT login JWTs. Viewer 0 denotes anonymous public access. */
@Component
public class MediaTicketService {
    public static final long TTL_SECONDS = 300;
    private final byte[] key;
    private final Clock clock;

    @Autowired
    public MediaTicketService(JwtConfig config) { this(config, Clock.systemUTC()); }

    public MediaTicketService(JwtConfig config, Clock clock) {
        if (config.getSecret() == null || config.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Media tickets require a strong signing root");
        }
        // Domain-separated subkey: possession of a media ticket cannot authenticate to JWT endpoints.
        this.key = hmac(config.getSecret().getBytes(StandardCharsets.UTF_8), "minthive/media-ticket/key/v1");
        this.clock = clock;
    }

    public String issue(MediaLocator.Ref ref, Long viewer) {
        if (viewer != null && viewer <= 0) throw new IllegalArgumentException("Invalid viewer");
        long expires = clock.instant().getEpochSecond() + TTL_SECONDS;
        String claims = "m1." + (viewer == null ? 0 : viewer) + "." + expires;
        return claims + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(ref, claims));
    }

    public Long verify(MediaLocator.Ref ref, String ticket) {
        try {
            if (ticket == null || ticket.length() > 160) throw new IllegalArgumentException();
            String[] parts = ticket.split("\\.", -1);
            if (parts.length != 4 || !"m1".equals(parts[0]) || !parts[1].matches("0|[1-9][0-9]*")
                    || !parts[2].matches("[1-9][0-9]*") || !parts[3].matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException();
            long viewer = Long.parseLong(parts[1]);
            long expires = Long.parseLong(parts[2]);
            long now = clock.instant().getEpochSecond();
            String claims = String.join(".", parts[0], parts[1], parts[2]);
            byte[] signature = Base64.getUrlDecoder().decode(parts[3]);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(signature).equals(parts[3])
                    || !MessageDigest.isEqual(sign(ref, claims), signature)
                    || expires <= now || expires > now + TTL_SECONDS) throw new IllegalArgumentException();
            return viewer == 0 ? null : viewer;
        } catch (RuntimeException e) {
            throw new BusinessException(ResultCode.FORBIDDEN, "媒体票据无效或已过期");
        }
    }

    private byte[] sign(MediaLocator.Ref ref, String claims) {
        return hmac(key, "minthive/media-content/v1\n" + ref.canonical() + "\n" + claims);
    }

    private static byte[] hmac(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Media signing unavailable", e);
        }
    }
}
