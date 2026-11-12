package com.minthive.media;

import com.minthive.config.MinioConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Only local canonical paths and the two configured storage origins are recognized. No URL is fetched. */
@Component
@RequiredArgsConstructor
public class MediaLocator {
    public static final String PREFIX = "/api/file/content/";
    private static final Pattern PRIVATE = Pattern.compile("users/[1-9][0-9]*/[a-f0-9]{32}\\.(jpg|jpeg|png|gif|webp|mp4)");
    private static final Pattern LEGACY = Pattern.compile("[0-9]{4}/[0-9]{2}/[0-9]{2}/[a-f0-9]{32}\\.(?i:jpg|jpeg|png|gif|webp|mp4)");
    private final MinioConfig config;

    public record Ref(String objectName, boolean legacy) {
        public String token() {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(objectName.getBytes(StandardCharsets.UTF_8));
        }
        public String canonical() { return PREFIX + token(); }
    }

    public Ref fromToken(String token) {
        if (token == null || token.length() > 512 || !token.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid media path");
        }
        String name = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        Ref ref = fromName(name);
        if (!ref.token().equals(token)) throw new IllegalArgumentException("Non-canonical media path");
        return ref;
    }

    public Ref fromName(String name) {
        if (PRIVATE.matcher(name).matches()) return new Ref(name, false);
        if (LEGACY.matcher(name).matches()) return new Ref(name, true);
        throw new IllegalArgumentException("Unsupported media object path");
    }

    /** Returns null for external/unrecognized URLs, but rejects malformed controlled application paths. */
    public Ref parse(String value) {
        if (value == null) return null;
        if (value.startsWith(PREFIX)) {
            int query = value.indexOf('?');
            return fromToken(value.substring(PREFIX.length(), query < 0 ? value.length() : query));
        }
        for (String origin : origins()) {
            for (boolean legacy : new boolean[]{true, false}) {
                String prefix = origin + "/" + config.getBucket() + (legacy ? "" : "-private") + "/";
                if (!value.startsWith(prefix)) continue;
                String name = value.substring(prefix.length());
                int query = name.indexOf('?');
                if (query >= 0) name = name.substring(0, query);
                try {
                    Ref ref = fromName(name);
                    return ref.legacy() == legacy ? ref : null;
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Finite exact database values; no LIKE, substring, or host-suffix authorization. */
    public List<String> aliases(Ref ref) {
        Set<String> values = new LinkedHashSet<>();
        values.add(ref.canonical());
        for (String origin : origins()) {
            values.add(origin + "/" + config.getBucket() + (ref.legacy() ? "" : "-private") + "/" + ref.objectName());
        }
        return List.copyOf(values);
    }

    private Set<String> origins() {
        Set<String> values = new LinkedHashSet<>();
        for (String value : new String[]{config.getPublicUrl(), config.getEndpoint()}) {
            if (value != null) values.add(value.replaceAll("/+$", ""));
        }
        return values;
    }
}
