import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

console.log("Hello from Functions!")

serve(async (req) => {
  // 1. 创建 Supabase Admin Client (使用 Service Role Key)
  // 此 Key 拥有数据库的完全访问权限，必须保密，仅限服务端使用
  const supabaseAdmin = createClient(
    Deno.env.get('SUPABASE_URL') ?? '',
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
    {
        auth: {
            autoRefreshToken: false,
            persistSession: false
        }
    }
  )

  try {
    // 2. 验证 Authorization Header (确保调用者已登录)
    // Edge Functions 默认会自动验证 JWT，并放入 context 中，或者我们可以手动验证
    // 这里我们直接从 header 获取 JWT 并获取用户信息，确保请求合法
    const authHeader = req.headers.get('Authorization')!
    const token = authHeader.replace('Bearer ', '')
    
    const { data: { user }, error: userError } = await supabaseAdmin.auth.getUser(token)

    if (userError || !user) {
      return new Response(
        JSON.stringify({ error: 'Unauthorized', details: userError }),
        { status: 401, headers: { "Content-Type": "application/json" } },
      )
    }

    const userId = user.id
    console.log(`Deleting user: ${userId}`)

    // 3. 执行物理删除 (admin.deleteUser)
    // 注意：这将级联删除该用户在 Auth 表中的记录
    // 如果你在 public schema 的表中有设置 ON DELETE CASCADE，也会被一并删除
    const { data, error: deleteError } = await supabaseAdmin.auth.admin.deleteUser(
      userId
    )

    if (deleteError) {
        console.error("Delete failed:", deleteError)
        return new Response(
            JSON.stringify({ error: deleteError.message }),
            { status: 400, headers: { "Content-Type": "application/json" } },
        )
    }

    return new Response(
      JSON.stringify({ message: "User deleted successfully", uid: userId }),
      { headers: { "Content-Type": "application/json" } },
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 400, headers: { "Content-Type": "application/json" } },
    )
  }
})

// To deploy:
// supabase functions deploy delete-user --no-verify-jwt
