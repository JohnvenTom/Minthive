package com.minthive.media;

import com.minthive.common.BusinessException;
import com.minthive.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class MediaTicketTest {
    private final MediaLocator.Ref ref = new MediaLocator.Ref("users/1/0123456789abcdef0123456789abcdef.mp4", false);

    private MediaTicketService service(long time) {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-only-root-key-at-least-32-bytes-long");
        return new MediaTicketService(config, Clock.fixed(Instant.ofEpochSecond(time), ZoneOffset.UTC));
    }

    @Test
    void ticketIsBoundToResourceViewerExpiryAndPurpose() {
        MediaTicketService signer = service(1000);
        String ticket = signer.issue(ref, 12L);
        assertEquals(12L, signer.verify(ref, ticket));
        assertNull(signer.verify(ref, signer.issue(ref, null)));
        MediaLocator.Ref other = new MediaLocator.Ref("users/2/0123456789abcdef0123456789abcdef.mp4", false);
        assertThrows(BusinessException.class, () -> signer.verify(other, ticket));
        assertThrows(BusinessException.class, () -> signer.verify(ref, ticket.replace("m1.12.", "m1.13.")));
        assertThrows(BusinessException.class, () -> signer.verify(ref, ticket.replace(".1300.", ".1299.")));
        assertThrows(BusinessException.class, () -> signer.verify(ref, "jwt." + ticket));
        assertThrows(BusinessException.class, () -> signer.verify(ref, ticket.substring(0, ticket.length() - 2) + "AA"));
        assertThrows(BusinessException.class, () -> signer.verify(ref, null));
    }

    @Test
    void fiveMinuteLifetimeRejectsExactExpiryAndFutureTickets() {
        String ticket = service(1000).issue(ref, 1L);
        assertEquals(1L, service(1299).verify(ref, ticket));
        assertThrows(BusinessException.class, () -> service(1300).verify(ref, ticket));
        assertThrows(BusinessException.class, () -> service(1301).verify(ref, ticket));
        assertThrows(BusinessException.class, () -> service(999).verify(ref, ticket));
    }

    @ParameterizedTest
    @CsvSource({"bytes=0-3,0,3", "bytes=4-,4,9", "bytes=-3,7,9", "bytes=5-999,5,9", "bytes=-999,0,9"})
    void parsesVideoSingleRanges(String header, long start, long end) {
        MediaContentService.ByteRange range = MediaContentService.ByteRange.parse(header, 10);
        assertEquals(start, range.start());
        assertEquals(end, range.end());
        assertEquals(end - start + 1, range.length());
        assertTrue(range.partial());
    }

    @ParameterizedTest
    @ValueSource(strings = {"bytes=-", "bytes=-0", "bytes=10-", "bytes=6-5", "bytes=0-1,4-5", "items=1-2", "bytes=99999999999999999999999-"})
    void rejectsUnsatisfiableMalformedAndMultiRanges(String header) {
        assertThrows(IllegalArgumentException.class, () -> MediaContentService.ByteRange.parse(header, 10));
    }

    @Test
    void absentRangeReturnsFullLengthIncludingEmptyObject() {
        assertEquals(10, MediaContentService.ByteRange.parse(null, 10).length());
        assertFalse(MediaContentService.ByteRange.parse(null, 10).partial());
        assertEquals(0, MediaContentService.ByteRange.parse(null, 0).length());
        assertThrows(IllegalArgumentException.class, () -> MediaContentService.ByteRange.parse("bytes=0-", 0));
    }
}
