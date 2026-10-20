package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClient {

    val customJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    ) {
        defaultSerializer = KotlinXSerializer(customJson)
        install(Postgrest)
        install(Auth) {
            sessionManager = SettingsSessionManager(json = customJson)
        }
    }
}
