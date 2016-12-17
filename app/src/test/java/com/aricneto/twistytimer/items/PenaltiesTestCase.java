package com.aricneto.twistytimer.items;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the {@link Penalties} class.
 *
 * @author damo
 */
public class PenaltiesTestCase {
    @Test
    public void testEncodeAndDecode() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        p = p.incurPreStartPenalty(Penalty.PLUS_TWO)
             .incurPreStartPenalty(Penalty.PLUS_TWO)
             .incurPreStartPenalty(Penalty.PLUS_TWO)
             .incurPreStartPenalty(Penalty.PLUS_TWO)
             .incurPostStartPenalty(Penalty.DNF)
             .incurPostStartPenalty(Penalty.PLUS_TWO)
             .incurPostStartPenalty(Penalty.PLUS_TWO);

        p = Penalties.decode(p.encode()); // Round trip through encode/decode.

        assertEquals(4, p.getPreStartPlusTwoCount());
        assertFalse(p.hasPreStartDNF());
        assertEquals(2, p.getPostStartPlusTwoCount());
        assertTrue(p.hasPostStartDNF());

        // And now for one with a pre-start DNF.
        p = Penalties.NO_PENALTIES
            .incurPreStartPenalty(Penalty.DNF)
            .incurPreStartPenalty(Penalty.PLUS_TWO)
            .incurPreStartPenalty(Penalty.PLUS_TWO)
            .incurPreStartPenalty(Penalty.PLUS_TWO);

        p = Penalties.decode(p.encode());

        assertEquals(3, p.getPreStartPlusTwoCount());
        assertTrue(p.hasPreStartDNF());
        assertEquals(0, p.getPostStartPlusTwoCount());
        assertFalse(p.hasPostStartDNF());
    }

    @Test
    public void testDecodeBad() throws Exception {
        // The encoding scheme is simple: bits 0-7 encode the pre-start
        // penalties (little-endian, I suppose) and bits 8-15 the post-start
        // penalties. An odd value flags a DNF and the value is incremented
        // by 2 for each "+2" penalty. For example, 7 => DNF + 3 x "+2".

        assertDecodeError(-1);      // Negative values are not allowed.
        assertDecodeError(0x1FFFF); // Stray 1's outside low 16 bits.

        // The following are indirect tests for the "obtain()" method:

        // Cannot have post-start penalties if there is a pre-start DNF.
        assertDecodeError(0x0101);
        assertDecodeError(0x0201);
        assertDecodeError(0x0301);
        assertDecodeError(0x0103);
        assertDecodeError(0x0203);
        assertDecodeError(0x0303);

        // Maximum number (4) of pre-start "+2" penalties exceeded.
        assertDecodeError(0x030A);
        // Maximum number (4) of post-start "+2" penalties exceeded.
        assertDecodeError(0x0A02);
        assertDecodeError(0x0B04); // Includes a post-start DNF
    }

    private static void assertDecodeError(int encodedPenalties) {
        try {
            Penalties.decode(encodedPenalties);
            fail("Expected an exception.");
        } catch (IllegalArgumentException ignore) {
            // This is expected.
        } catch (Throwable e) {
            fail("Expected an IllegalArgumentException: " + e);
        }
    }

    @Test
    public void testHasPreStartDNF() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertFalse(p.hasPreStartDNF());

        p = p.incurPreStartPenalty(Penalty.DNF);
        assertTrue(p.hasPreStartDNF());

        // Ensure a post-start DNF is not mistaken for a pre-start DNF.
        p = Penalties.NO_PENALTIES.incurPostStartPenalty(Penalty.DNF);
        assertFalse(p.hasPreStartDNF());
    }

    @Test
    public void testHasPostStartDNF() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertFalse(p.hasPostStartDNF());

        p = p.incurPostStartPenalty(Penalty.DNF);
        assertTrue(p.hasPostStartDNF());

        p = p.annulPostStartPenalty(Penalty.DNF);
        assertFalse(p.hasPostStartDNF());

        // Ensure a pre-start DNF is not mistaken for a post-start DNF.
        p = p.incurPreStartPenalty(Penalty.DNF);
        assertFalse(p.hasPostStartDNF());
    }

    @Test
    public void testHasDNF() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertFalse(p.hasDNF());

        p = p.incurPreStartPenalty(Penalty.DNF);
        assertTrue(p.hasDNF());

        p = Penalties.NO_PENALTIES;
        assertFalse(p.hasDNF());

        p = p.incurPostStartPenalty(Penalty.DNF);
        assertTrue(p.hasDNF());
    }

    @Test
    public void testHasPreStartPenalties() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertFalse(p.hasPreStartPenalties());

        p = p.incurPreStartPenalty(null);
        assertFalse(p.hasPreStartPenalties());

        p = p.incurPreStartPenalty(Penalty.NONE);
        assertFalse(p.hasPreStartPenalties());

        p = p.incurPreStartPenalty(Penalty.DNF);
        assertTrue(p.hasPreStartPenalties());

        p = Penalties.NO_PENALTIES;
        assertFalse(p.hasPreStartPenalties());

        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertTrue(p.hasPreStartPenalties());

        // Ensure post-start penalties are not mistaken for pre-start penalties.
        p = Penalties.NO_PENALTIES
            .incurPostStartPenalty(Penalty.DNF)
            .incurPostStartPenalty(Penalty.PLUS_TWO);
        assertFalse(p.hasPreStartPenalties());
    }

    @Test
    public void testHasPostStartPenalties() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertFalse(p.hasPostStartPenalties());

        p = p.incurPostStartPenalty(null);
        assertFalse(p.hasPostStartPenalties());

        p = p.incurPostStartPenalty(Penalty.NONE);
        assertFalse(p.hasPostStartPenalties());

        p = p.incurPostStartPenalty(Penalty.DNF);
        assertTrue(p.hasPostStartPenalties());

        p = Penalties.NO_PENALTIES;
        assertFalse(p.hasPostStartPenalties());
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertTrue(p.hasPostStartPenalties());

        // Ensure pre-start penalties are not mistaken for post-start penalties.
        p = Penalties.NO_PENALTIES
            .incurPreStartPenalty(Penalty.DNF)
            .incurPreStartPenalty(Penalty.PLUS_TWO);
        assertFalse(p.hasPostStartPenalties());
    }

    @Test
    public void testHasPenalties() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertFalse(p.hasPenalties());

        assertTrue(p.incurPreStartPenalty(Penalty.DNF).hasPenalties());
        assertTrue(p.incurPostStartPenalty(Penalty.DNF).hasPenalties());

        assertTrue(p.incurPreStartPenalty(Penalty.PLUS_TWO).hasPenalties());
        assertTrue(p.incurPostStartPenalty(Penalty.PLUS_TWO).hasPenalties());
    }

    @Test
    public void testGetPreStartPlusTwoCount() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertEquals(0, p.getPreStartPlusTwoCount());

        p = p.incurPreStartPenalty(Penalty.DNF);
        assertEquals(0, p.getPreStartPlusTwoCount());

        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(1, p.getPreStartPlusTwoCount());

        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(2, p.getPreStartPlusTwoCount());

        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(3, p.getPreStartPlusTwoCount());

        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(Penalties.MAX_PRE_START_PLUS_TWOS,
            p.getPreStartPlusTwoCount());
    }

    @Test
    public void testGetPostStartPlusTwoCount() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertEquals(0, p.getPostStartPlusTwoCount());

        p = p.incurPostStartPenalty(Penalty.DNF);
        assertEquals(0, p.getPostStartPlusTwoCount());

        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(1, p.getPostStartPlusTwoCount());

        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(2, p.getPostStartPlusTwoCount());

        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(3, p.getPostStartPlusTwoCount());

        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(Penalties.MAX_POST_START_PLUS_TWOS,
            p.getPostStartPlusTwoCount());
    }

    @Test
    public void testGetTimePenalty() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertEquals(0, p.getTimePenalty());

        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(2_000, p.getTimePenalty());
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(4_000, p.getTimePenalty());
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(6_000, p.getTimePenalty());
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(8_000, p.getTimePenalty());

        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(10_000, p.getTimePenalty());
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(12_000, p.getTimePenalty());
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(14_000, p.getTimePenalty());
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(16_000, p.getTimePenalty());

        // A DNF should not affect the timer penalties.
        p = p.incurPreStartPenalty(Penalty.DNF);
        assertEquals(16_000, p.getTimePenalty());
    }

    @Test
    public void testIncurPreStartPenalty() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertFalse(p.hasPreStartDNF());
        assertEquals(0, p.getPreStartPlusTwoCount());

        p = p.incurPreStartPenalty(Penalty.DNF);
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        // Once the maximum is reached, no more "+2" penalties can be incurred,
        // so these extra penalties should be ignored.
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO);

        assertTrue(p.hasPreStartDNF());
        assertEquals(Penalties.MAX_PRE_START_PLUS_TWOS,
            p.getPreStartPlusTwoCount());

        p = p.incurPreStartPenalty(null); // Should do nothing.
        assertTrue(p.hasPreStartDNF());
        assertEquals(Penalties.MAX_PRE_START_PLUS_TWOS,
            p.getPreStartPlusTwoCount());

        p = p.incurPreStartPenalty(Penalty.NONE); // Should do nothing.
        assertTrue(p.hasPreStartDNF());
        assertEquals(Penalties.MAX_PRE_START_PLUS_TWOS,
            p.getPreStartPlusTwoCount());

        assertTrue(p.hasPreStartDNF());
        assertEquals(Penalties.MAX_POST_START_PLUS_TWOS,
            p.getPreStartPlusTwoCount());

        // If there are post-start penalties, then a pre-start DNF cannot be
        // incurred. Post-start penalties do not bar pre-start "+2" penalties.
        p = Penalties.NO_PENALTIES;
        p = p.incurPreStartPenalty(Penalty.DNF);
        assertTrue(p.hasPreStartDNF());
        assertTrue(p.hasPreStartPenalties());

        p = Penalties.NO_PENALTIES.incurPostStartPenalty(Penalty.DNF);
        p = p.incurPreStartPenalty(Penalty.DNF); // Barred by post-start DNF.
        assertFalse(p.hasPreStartDNF());
        assertFalse(p.hasPreStartPenalties());

        p = Penalties.NO_PENALTIES.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPreStartPenalty(Penalty.DNF); // Barred by post-start "+2".
        assertFalse(p.hasPreStartDNF());
        assertFalse(p.hasPreStartPenalties());

        p = Penalties.NO_PENALTIES
            .incurPostStartPenalty(Penalty.DNF)
            .incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO); // "+2" still allowed.
        p = p.incurPreStartPenalty(Penalty.PLUS_TWO); // "+2" still allowed.
        assertFalse(p.hasPreStartDNF());
        assertEquals(2, p.getPreStartPlusTwoCount());
        p = p.incurPreStartPenalty(Penalty.DNF); // "DNF" still barred.
        assertFalse(p.hasPreStartDNF());
        assertEquals(2, p.getPreStartPlusTwoCount());
    }

    @Test
    public void testIncurPostStartPenalty() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertFalse(p.hasPostStartDNF());
        assertEquals(0, p.getPostStartPlusTwoCount());

        p = p.incurPostStartPenalty(Penalty.DNF);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        // Once the maximum is reached, no more "+2" penalties can be incurred,
        // so these extra penalties should be ignored.
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);

        assertTrue(p.hasPostStartDNF());
        assertEquals(Penalties.MAX_POST_START_PLUS_TWOS,
            p.getPostStartPlusTwoCount());

        p = p.incurPostStartPenalty(null); // Should do nothing.
        assertTrue(p.hasPostStartDNF());
        assertEquals(Penalties.MAX_POST_START_PLUS_TWOS,
            p.getPostStartPlusTwoCount());

        p = p.incurPostStartPenalty(Penalty.NONE); // Should do nothing.
        assertTrue(p.hasPostStartDNF());
        assertEquals(Penalties.MAX_POST_START_PLUS_TWOS,
            p.getPostStartPlusTwoCount());

        assertTrue(p.hasPostStartDNF());
        assertEquals(Penalties.MAX_POST_START_PLUS_TWOS,
            p.getPostStartPlusTwoCount());

        // If there is a pre-start DNF, then post-start penalties cannot be
        // incurred. Pre-start "+2" penalties do not bar post-start penalties.
        p = Penalties.NO_PENALTIES.incurPreStartPenalty(Penalty.DNF);
        p = p.incurPostStartPenalty(Penalty.DNF);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertFalse(p.hasPostStartPenalties());

        p = Penalties.NO_PENALTIES.incurPreStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.DNF);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertTrue(p.hasPostStartDNF());
        assertEquals(2, p.getPostStartPlusTwoCount());
    }

    @Test
    public void testAnnulPostStartPenalty() throws Exception {
        Penalties p = Penalties.NO_PENALTIES;

        assertFalse(p.hasPostStartDNF());
        assertEquals(0, p.getPostStartPlusTwoCount());

        p = p.incurPostStartPenalty(Penalty.DNF);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO);
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO); // > max, so ignored.
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO); // > max, so ignored.
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO); // > max, so ignored.
        p = p.incurPostStartPenalty(Penalty.PLUS_TWO); // > max, so ignored.

        assertTrue(p.hasPostStartDNF());
        assertEquals(Penalties.MAX_POST_START_PLUS_TWOS,
            p.getPostStartPlusTwoCount());

        p = p.annulPostStartPenalty(null); // Should do nothing.
        assertEquals(Penalties.MAX_POST_START_PLUS_TWOS,
            p.getPostStartPlusTwoCount());
        assertTrue(p.hasPostStartDNF());

        p = p.annulPostStartPenalty(Penalty.NONE); // Should do nothing.
        assertEquals(Penalties.MAX_POST_START_PLUS_TWOS,
            p.getPostStartPlusTwoCount());
        assertTrue(p.hasPostStartDNF());

        p = p.annulPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(3, p.getPostStartPlusTwoCount());
        assertTrue(p.hasPostStartDNF());

        p = p.annulPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(2, p.getPostStartPlusTwoCount());
        assertTrue(p.hasPostStartDNF());

        p = p.annulPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(1, p.getPostStartPlusTwoCount());
        assertTrue(p.hasPostStartDNF());

        p = p.annulPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(0, p.getPostStartPlusTwoCount());
        assertTrue(p.hasPostStartDNF());

        // Make sure annulling penalties does not make the count go negative.
        p = p.annulPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(0, p.getPostStartPlusTwoCount());
        assertTrue(p.hasPostStartDNF());

        p = p.annulPostStartPenalty(Penalty.DNF);
        assertEquals(0, p.getPostStartPlusTwoCount());
        assertFalse(p.hasPostStartDNF());

        // Annul the DNF again to make sure it does not cause a problem.
        p = p.annulPostStartPenalty(Penalty.DNF);
        assertEquals(0, p.getPostStartPlusTwoCount());
        assertFalse(p.hasPostStartDNF());
    }

    @Test
    public void testEquals() throws Exception {
        Penalties p1 = Penalties.NO_PENALTIES;
        Penalties p2 = Penalties.NO_PENALTIES;

        assertEquals(p1, p2);
        assertEquals(p2, p1);

        // DNFs must be in the same phase.
        p1 = p1.incurPreStartPenalty(Penalty.DNF);
        p2 = p2.incurPostStartPenalty(Penalty.DNF);
        assertNotEquals(p1, p2);
        assertNotEquals(p2, p1);

        p1 = Penalties.NO_PENALTIES.incurPreStartPenalty(Penalty.DNF);
        p2 = Penalties.NO_PENALTIES.incurPreStartPenalty(Penalty.DNF);
        assertEquals(p1, p2);
        assertEquals(p2, p1);

        p1 = Penalties.NO_PENALTIES.incurPostStartPenalty(Penalty.DNF);
        p2 = Penalties.NO_PENALTIES.incurPostStartPenalty(Penalty.DNF);
        assertEquals(p1, p2);
        assertEquals(p2, p1);

        // Similarly, count of "+2" penalties must match in each phase.
        p1 = Penalties.NO_PENALTIES.incurPreStartPenalty(Penalty.PLUS_TWO);
        p2 = Penalties.NO_PENALTIES;
        assertNotEquals(p1, p2);
        assertNotEquals(p2, p1);

        p2 = p2.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(p1, p2);
        assertEquals(p2, p1);

        p1 = p1.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertNotEquals(p1, p2);
        assertNotEquals(p2, p1);

        p2 = p2.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(p1, p2);
        assertEquals(p2, p1);

        p1 = p1.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertNotEquals(p1, p2);
        assertNotEquals(p2, p1);

        p2 = p2.incurPreStartPenalty(Penalty.PLUS_TWO);
        assertEquals(p1, p2);
        assertEquals(p2, p1);

        p1 = p1.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertNotEquals(p1, p2);
        assertNotEquals(p2, p1);

        p2 = p2.incurPostStartPenalty(Penalty.PLUS_TWO);
        assertEquals(p1, p2);
        assertEquals(p2, p1);

        p1 = p1.incurPostStartPenalty(Penalty.DNF);
        assertNotEquals(p1, p2);
        assertNotEquals(p2, p1);

        p2 = p2.incurPostStartPenalty(Penalty.DNF);
        assertEquals(p1, p2);
        assertEquals(p2, p1);
    }
}
