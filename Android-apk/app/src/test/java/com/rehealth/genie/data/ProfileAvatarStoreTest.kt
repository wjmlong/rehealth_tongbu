package com.rehealth.genie.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileAvatarStoreTest {
    @Test
    fun `storage key is deterministic and hides raw user identity`() {
        val first = profileAvatarStorageKey("user-12345")
        val second = profileAvatarStorageKey("user-12345")

        assertEquals(first, second)
        assertEquals(24, first.length)
        assertFalse(first.contains("user-12345"))
    }

    @Test
    fun `different users receive isolated storage keys`() {
        assertFalse(profileAvatarStorageKey("user-a") == profileAvatarStorageKey("user-b"))
    }
}
