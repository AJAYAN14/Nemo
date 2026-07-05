import io.github.jan.supabase.auth.Auth
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.declaredMemberExtensionFunctions
import kotlin.reflect.full.extensionReceiverParameter
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken

fun main() {
    println("Methods on Auth:")
    val methods = Auth::class.memberFunctions.map { it.name }
    println(methods.filter { it.contains("link", true) })
    
    // Check if IDToken has any link identity extension
}
