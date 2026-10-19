package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import kotlinx.serialization.json.Json

object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    ) {
        defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            }
        )
        install(Postgrest)
        install(Auth)
    }
}