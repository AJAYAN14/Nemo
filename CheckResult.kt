import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.declaredMemberProperties

fun main() {
    println("Checking NativeSignInResult.Success properties...")
    NativeSignInResult.Success::class.declaredMemberProperties.forEach { println(it.name) }
}
