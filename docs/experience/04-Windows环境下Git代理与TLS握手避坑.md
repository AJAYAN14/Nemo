# Windows 环境下 Git 代理配置与 TLS 握手避坑

> 本文档记录在 Windows 环境下使用 Git 通过本地代理连接 GitHub 时的网络阻断问题分析、排查思路与永久避坑方案。

---

## 一、 问题背景与现象

在向 GitHub 远程仓库推送代码（`git push origin main`）时，连续遭遇了两次网络通信阻断：

### 1. 现象一：Schannel 握手阻断
```text
fatal: unable to access 'https://github.com/AJAYAN14/Nemo.git/': schannel: failed to receive handshake, SSL/TLS connection failed
```

### 2. 现象二：代理协议错配引发连接被切断
```text
fatal: unable to access 'https://github.com/AJAYAN14/Nemo.git/': TLS connect error: error:0A000126:SSL routines::unexpected eof while reading
```

---

## 二、 根因剖析

1. **Schannel 与本地代理兼容性缺陷**：
   - Windows 版 Git 默认使用 `http.sslbackend = schannel`（基于操作系统原生安全通道 CryptoAPI）。
   - 在通过本地 HTTP/SOCKS 代理隧道连接外部 HTTPS 服务时，Schannel 在 TLS 握手阶段（TLS 1.3 / SNI 扩展协商）极易因握手包封装或代理管道重定向问题而协商失败。
2. **代理协议类型错配（socks5h vs http）**：
   - 本地仓库配置中指定了 `socks5h://127.0.0.1:7897`，但本地代理客户端（如 Clash Verge、Mihomo 等）在 7897 端口上默认承载的是 HTTP/HTTPS 混合代理服务。
   - Git 发起 SOCKS5 握手帧到 HTTP 端口，代理服务端因协议不匹配立即主动关闭连接，导致 OpenSSL 抛出 `unexpected eof while reading`。
3. **Local 配置覆盖 Global 配置**：
   - 仓库本地配置（`.git/config`）优先级高于全局配置（`~/.gitconfig`）。本地仓库残留的失效配置导致全局正确配置无法生效。

---

## 三、 四列对照复盘表

| 坑是什么 (Pitfall) | 怎么避 (Avoidance) | 相关工具 / 文件 (Tools & Files) | 统一原则 (Unified Principle) |
|---|---|---|---|
| **Schannel 握手阻断**：Windows 原生安全通道在代理环境下与 GitHub 握手极易失败抛出 `schannel: failed to receive handshake` | 显式指定 `http.sslbackend = openssl`，利用 Git 内置 OpenSSL 引擎接管 TLS 握手，获得最高网络兼容性 | `git config http.sslbackend openssl` / `.git/config` | **加密引擎与传输隧道解耦原则**：走代理隧道优先选用 OpenSSL，杜绝操作系统原生 Schannel 握手异常。 |
| **代理协议错配**：本地端口为 HTTP/混合代理端口，Git 配置为 `socks5h://` 导致握手被代理端直接切断抛出 `unexpected eof` | 确认代理客户端监听协议，本地回环统一配置显式 `http://127.0.0.1:端口` 协议头 | `git config http.proxy http://127.0.0.1:7897` | **协议显式对齐原则**：代理 URI 必须精准匹配服务端协议类型，避免协议盲猜。 |
| **Local 覆盖 Global 陷阱**：全局虽已配置代理，但仓库本地配置有残留错误的 socks5h 导致全局配置失效 | 检查时优先使用 `git config --local --list` 排查仓库级残留脏配置并做覆盖修复 | `.git/config` / `git config --local` | **就近覆盖排查原则**：排查 Git 网络故障首查 Local 配置，确保无陈旧失效覆写。 |

---

## 四、 永久固化与验证命令

### 1. 本地仓库永久固化命令
```powershell
git config --local http.proxy http://127.0.0.1:7897
git config --local https.proxy http://127.0.0.1:7897
git config --local http.sslbackend openssl
```

### 2. 连通性校验命令
```powershell
# 测试端口是否处于监听状态
Test-NetConnection -ComputerName 127.0.0.1 -Port 7897

# 验证 Git 远程通信
git ls-remote https://github.com/AJAYAN14/Nemo.git
```
