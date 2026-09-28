package com.MADproject.quoraforuniversities.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth

object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = "https://orlipyztlvvvqzxvqipc.supabase.co",
        supabaseKey = "sb_publishable__kCCm6Jd_w8X_9NDu2Twpg_39c3-KPs"
    ) {
        install(Postgrest)
        install(Auth)
    }
}