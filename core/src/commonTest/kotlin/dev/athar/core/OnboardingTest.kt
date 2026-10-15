package dev.athar.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingTest {
    @Test fun `a fresh install with nothing in the vault shows the tour`() = assertEquals(Welcome.Tour, Onboarding.decide("", false, false, "0.1.0"))
    @Test fun `an update from a version that stored nothing shows what is new, not the tour`() = assertEquals(Welcome.WhatsNew, Onboarding.decide("", false, true, "0.1.0"))
    @Test fun `the same version shows nothing`() = assertEquals(Welcome.Nothing, Onboarding.decide("0.1.0", true, true, "0.1.0"))
    @Test fun `a newer version shows what is new once`() = assertEquals(Welcome.WhatsNew, Onboarding.decide("0.1.0", true, true, "0.2.0"))
    @Test fun `a downgrade shows nothing`() = assertEquals(Welcome.Nothing, Onboarding.decide("0.2.0", true, true, "0.1.0"))
    @Test fun `a finished tour with no seen version and no data shows what is new`() = assertEquals(Welcome.WhatsNew, Onboarding.decide("", true, false, "0.1.0"))
    @Test fun `versions compare by number, not text`() { assertTrue(Onboarding.compare("0.10.0", "0.9.0") > 0); assertEquals(0, Onboarding.compare("1.0", "1.0.0")) }
    @Test fun `notes fall back to the newest older version and have both languages`() {
        assertTrue(ReleaseNotes.forVersion("0.1.0").isNotEmpty())
        assertEquals(ReleaseNotes.forVersion("0.1.1"), ReleaseNotes.forVersion("0.1.9"))
        assertTrue(ReleaseNotes.forVersion("0.1.1") != ReleaseNotes.forVersion("0.1.0"))
        assertTrue(ReleaseNotes.forVersion("0.1.1", "ar") != ReleaseNotes.forVersion("0.1.1", "en"))
        assertTrue(ReleaseNotes.forVersion("0.1.0", "ar") != ReleaseNotes.forVersion("0.1.0", "en"))
        assertTrue(ReleaseNotes.forVersion("0.0.1").isEmpty())
    }
}
