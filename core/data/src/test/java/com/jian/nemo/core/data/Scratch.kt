fun checkSupabaseMethods() {
    val supabase = io.github.jan.supabase.createSupabaseClient("", "") {
        install(io.github.jan.supabase.auth.Auth)
    }
    // Just for autocomplete checks
    val user = supabase.auth.currentUserOrNull()
    val identities = user?.identities
}
