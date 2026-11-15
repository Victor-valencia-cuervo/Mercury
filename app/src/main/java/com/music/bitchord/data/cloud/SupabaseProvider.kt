package com.music.bitchord.data.cloud

import com.music.bitchord.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Mercurio's single Supabase client.
 *
 * Created lazily, so a build with no URL/key (see `SUPABASE_URL` and
 * `SUPABASE_KEY` in app/build.gradle.kts) never touches the library and the app
 * simply runs local-only.
 *
 * There is no sign-in screen: [CloudSync] signs in anonymously, which gives each
 * install a real user id that Row Level Security can bind rows to.
 */
object SupabaseProvider {

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY,
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
