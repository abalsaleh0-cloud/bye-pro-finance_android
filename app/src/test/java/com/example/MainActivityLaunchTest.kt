package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.finance.auth.SupabaseAuthRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityLaunchTest {

    @Test
    fun mainActivityLaunchesWithoutStartupCrash() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
        controller.pause().stop().destroy()
    }

    @Test
    fun persistedRefreshTokenKeepsLocalSessionAvailableWhenAccessTokenExpires() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("bye_pro_supabase_auth", Context.MODE_PRIVATE)
            .edit()
            .putString("refresh_token", "stored-refresh-token")
            .putString("access_token", "expired-access-token")
            .putLong("expires_at", 1L)
            .commit()

        assertTrue(SupabaseAuthRepository(context).hasPersistedSession())
    }

    @Test
    fun missingTokensDoNotCreatePersistedSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("bye_pro_supabase_auth", Context.MODE_PRIVATE)
            .edit().clear().commit()

        assertFalse(SupabaseAuthRepository(context).hasPersistedSession())
    }

}
