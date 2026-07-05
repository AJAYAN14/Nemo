package com.jian.nemo.feature.user

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google

suspend fun test(c: SupabaseClient) {
    c.auth.linkIdentity(Google)
}
