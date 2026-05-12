import subprocess
import webbrowser
import time
import os
import sys

def start_dashboard():
    # 获取脚本所在目录，即 web 目录
    web_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(web_dir)

    print("正在启动 Nemo Web 后台管理系统...")
    
    # 启动 Next.js 开发服务器
    # 使用 Popen 以便不阻塞脚本执行
    try:
        # shell=True 在 Windows 上是必须的，以执行 npm 命令
        process = subprocess.Popen(["npm", "run", "dev"], shell=True)
        
        print("等待服务器初始化 (约 3 秒)...")
        time.sleep(3)
        
        # 打开浏览器
        url = "http://localhost:3000"
        print(f"正在浏览器中打开: {url}")
        
        # webbrowser.open 会使用系统默认浏览器
        # 如果用户明确要求 Chrome，可以尝试指定，但通常默认就是 Chrome
        webbrowser.open(url)
        
        print("\n-------------------------------------------")
        print("后台系统已启动。")
        print("按 Ctrl+C 可以停止 Python 脚本，但服务器可能仍在运行。")
        print("建议直接在终端管理 npm 进程。")
        print("-------------------------------------------\n")
        
        # 让 Python 脚本保持运行，直到手动停止，以便观察日志
        process.wait()
        
    except KeyboardInterrupt:
        print("\n停止脚本...")
        if process:
            process.terminate()
    except Exception as e:
        print(f"启动失败: {e}")

if __name__ == "__main__":
    start_dashboard()
