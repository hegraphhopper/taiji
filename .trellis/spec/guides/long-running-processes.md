# 长驻进程与工具调用检查清单

适用场景：需要启动后端、前端 dev server、数据库、消息队列等常驻进程，或在子代理中做运行态验证。

## 启动前

- [ ] 确认进程需要常驻，而不是一次性命令。
- [ ] 检查端口是否已被本项目占用：`Get-NetTCPConnection -LocalPort 8080,5173 -State Listen`。
- [ ] 检查 `target/app.pid`、`target/vite.pid` 是否指向存活且属于本项目的进程。
- [ ] 决定使用项目脚本：`scripts/start-demo.ps1`，不要临时拼 `Start-Process`。

## 启动方式

- 必须使用隐藏窗口并把 stdout/stderr 重定向到文件：

```powershell
Start-Process -FilePath 'java' `
  -ArgumentList '-jar', $jar, '--spring.profiles.active=demo' `
  -WorkingDirectory $root -WindowStyle Hidden `
  -RedirectStandardOutput target\app.out.log `
  -RedirectStandardError target\app.err.log -PassThru
```

- 禁止 `-NoNewWindow`：子进程会继承当前工具的 stdout 管道，工具等待管道关闭，调用会长时间不返回。
- 禁止在前台直接执行 `java -jar`、`mvn spring-boot:run`、`npm run dev`、`vite`。
- 启动命令与健康检查拆成两次工具调用；启动命令应自行退出。

## 验证与停止

- 健康检查使用独立短命令：`Invoke-WebRequest http://localhost:8080/api/v1/health`。
- 停止使用 `scripts/stop-demo.ps1`，按 PID 文件杀进程树；按端口兜底前先确认命令行归属。
- 停止后确认端口释放，再执行 `mvn clean/package`（Windows 下运行中的 jar 会被锁定）。

## 常见症状

| 症状 | 原因 | 处理 |
|------|------|------|
| 健康检查成功但工具调用不返回 | 子进程持有输出管道 | 停掉子进程；改用隐藏窗口 + 重定向 |
| 工具超时设置无效 | 等待管道 EOF，不是等待命令退出 | 同上；拆分调用 |
| 会话日志异常膨胀 | 长时间挂起 + 逐 token 记录 | 结束挂起调用；避免长驻前台进程 |
| `mvn clean` 删除 jar 失败 | 运行中的 Java 锁定文件 | 先停止服务再构建 |
| 停止脚本误杀其他服务 | 仅按端口杀进程 | 校验 PID/命令行归属后再杀 |

## 子代理提示要求

- 明确写出“禁止启动/停止常驻服务”或“使用 scripts/start-demo.ps1”。
- 要求运行态验证与构建分离；实现子代理只编译，不启动服务。
- 若必须启动，要求启动、轮询、停止各自独立调用，并在报告中附 PID 与端口。

## Wrong vs Correct

### Wrong

```powershell
Start-Process java -ArgumentList '-jar','target\app.jar' -NoNewWindow -PassThru
# 等待健康检查；工具一直不返回
```

### Correct

```powershell
$p = Start-Process java -ArgumentList '-jar', $jar -WindowStyle Hidden `
  -RedirectStandardOutput target\app.out.log `
  -RedirectStandardError target\app.err.log -PassThru
$p.Id | Set-Content target\app.pid
```

另起一次工具调用轮询健康检查。
