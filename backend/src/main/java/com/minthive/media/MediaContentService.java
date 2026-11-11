package com.minthive.media;

import com.minthive.common.BusinessException;
import com.minthive.util.MinioUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/** Native img/video access uses only purpose-bound tickets; login JWTs never appear in URLs. */
@Service
@RequiredArgsConstructor
public class MediaContentService {
    private final MediaLocator locator;
    private final MediaTicketService tickets;
    private final MediaAccessService access;
    private final MinioUtil storage;

    public void serve(String token, String ticket, HttpServletRequest request, HttpServletResponse response) throws IOException {
        secureHeaders(response);
        if (!"GET".equals(request.getMethod())) {
            response.setHeader("Allow", "GET");
            response.setStatus(405);
            return;
        }
        try {
            MediaLocator.Ref ref = locator.fromToken(token);
            Long viewer = tickets.verify(ref, ticket);
            // Recheck active account AND current references for every fetch, even during the ticket lifetime.
            MinioUtil.Metadata meta = access.requireReadable(ref, viewer);
            String contentType = safeContentType(ref.objectName());
            ByteRange range;
            try {
                // Without an ETag validator, If-Range must fall back to the full representation.
                range = ByteRange.parse(request.getHeader("If-Range") == null ? request.getHeader("Range") : null, meta.size());
            } catch (IllegalArgumentException e) {
                response.setStatus(416);
                response.setHeader("Content-Range", "bytes */" + meta.size());
                return;
            }
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "inline");
            if (range.partial()) {
                response.setStatus(206);
                response.setHeader("Content-Range", "bytes " + range.start() + "-" + range.end() + "/" + meta.size());
            }
            response.setContentLengthLong(range.length());
            if (range.length() == 0) return;
            try (InputStream input = storage.open(ref, range.start(), range.length())) {
                input.transferTo(response.getOutputStream());
            }
        } catch (BusinessException e) {
            int code = e.getCode();
            fail(response, code == 404 ? 404 : (code == 401 || code == 403 ? 403 : 503));
        } catch (IllegalArgumentException e) {
            fail(response, 403);
        } catch (Exception e) {
            if (response.isCommitted()) throw new IOException("Media stream interrupted", e);
            fail(response, 503);
        }
    }

    private void fail(HttpServletResponse response, int status) throws IOException {
        if (response.isCommitted()) throw new IOException("Media stream authorization/storage failure");
        response.reset();
        secureHeaders(response);
        response.setStatus(status);
    }

    private void secureHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Content-Security-Policy", "default-src 'none'; sandbox");
    }

    private String safeContentType(String objectName) {
        return switch (objectName.substring(objectName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            default -> throw new IllegalArgumentException("Unsupported media type");
        };
    }

    record ByteRange(long start, long end, boolean partial) {
        long length() { return end - start + 1; }

        static ByteRange parse(String value, long size) {
            if (size < 0) throw new IllegalArgumentException();
            if (value == null) return new ByteRange(0, size - 1, false);
            if (size == 0 || !value.matches("bytes=[0-9]*-[0-9]*")) throw new IllegalArgumentException();
            String[] parts = value.substring(6).split("-", -1);
            long start;
            long end;
            if (parts[0].isEmpty()) {
                long suffix = Long.parseLong(parts[1]);
                if (suffix <= 0) throw new IllegalArgumentException();
                start = Math.max(0, size - suffix);
                end = size - 1;
            } else {
                start = Long.parseLong(parts[0]);
                end = parts[1].isEmpty() ? size - 1 : Math.min(Long.parseLong(parts[1]), size - 1);
            }
            if (start >= size || end < start) throw new IllegalArgumentException();
            return new ByteRange(start, end, true);
        }
    }
}
