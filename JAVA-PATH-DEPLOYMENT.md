# 自定义 Java 路径：需求更正与部署

## 2026-10-05 启动前校验修复

Jar/JarWar 启动前不再在 Agent JVM 中 loadClass 应用主类，改为验证 ZIP、Manifest、Main-Class 和对应类文件存在；应用字节码兼容性由项目所选 Java 的实际启动负责。避免 Java 11 Agent 在执行 Java 21 项目命令之前抛出 UnsupportedClassVersionError。

- 4 项 JUnit 回归测试在 Java 8 运行通过，覆盖新字节码（61/65/100）、缺失 Manifest/主类属性/主类文件、损坏归档和 Java 路径命令回归。
- 服务器 Java 11.0.2 使用修复后实际 Agent 类只读校验 pnkx-admin.jar，返回 PASS；没有执行应用主类或启动业务进程。
- 只更新 Agent：`/vol1/docker/jpom/jpom-agent/lib/Agent-2.11.12.1-jar-preflight.jar`，SHA-256 `de0c7afa2f22ecf877247a21b7c2172054ccd215be176b84f50724f963084c19`。
- 切换前停止 Agent 并完整备份到 `/vol1/docker/jpom/backups/20261005-jar-preflight/jpom-agent`。Server/H2 未停机、未替换。
- 部署后 Agent/Server 健康接口返回 200，23 个项目保留；project.json 和 agent_authorize.json 与备份逐字节一致；业务进程 1184181（pnkx-admin）、1742984 未变化。
- 最终两个 systemd 单元均 active；运行中的 Agent PID 1216825 确认使用 jar-preflight 包；workspace_env_var.json 和 conf 目录文件与备份逐字节一致。只读 probe 已移到备份目录保存。
- 回滚：停止 jpom-agent 单元及 Agent.sh stop -s，确认 Agent 进程停止，复制本次备份的 lib/run.bin 回原目录，再启动 jpom-agent。不要恢复或覆盖业务数据。

以下为较早一次 Java 路径功能部署记录，其运行包名称与业务 PID 不代表最新部署。

2026-10-05 将原“自定义 JAR 全路径”更正为“自定义 Java 可执行文件全路径”。官方源码基线和服务器版本仍为 2.11.12.1（34dd258c8f428f35b76d4ae7bf2e674c41b8bb30）。

## 使用

编辑项目的 Java 运行模式时，在“Java 路径”填写完整可执行文件路径，例如 `/usr/local/java/jdk1.8.0_202/bin/java`，不是 `bin/jar`，也不是应用包路径。留空恢复系统默认 `java`（Windows 原逻辑为 `javaw`）。

输入时已有运行命令展示立即更新 Java 路径；保存后 Agent 实际构造的启动命令使用同一配置。JAR 查找方式恢复官方原逻辑。支持含空格路径并添加引号；保存时检查绝对路径、文件存在、执行权限和危险 shell 字符。

## 验证

- 前端目标 ESLint、Vite 构建和 JDK 21 Maven Server/Agent 构建通过。
- 构建默认跳过 Maven 测试，随后以构建后实际 Agent 类运行 JUnit `JavaPathCommandTest` 通过：完整 Unix/Windows 命令、自定义 Java 优先、trim、默认回退、空格引号和原 JAR 扫描。
- 对实际 Vue computed 方法执行 Node 断言，通过实时替换、空格引号、清空回退测试。
- 服务器仅预检（previewData=true）现有项目：Java 8、21 可执行文件路径返回 200；bin/jar、相对路径返回 405。逐字节确认项目文件未被修改。
- Server、Agent 健康接口均返回 200；23 个项目保留；项目和授权文件与本次备份一致；前端 HTTP 资源包含实时 Java 路径展示；两个 systemd 单元 active；业务 PID 15270、1742984 未变化。

未修改或重启任何业务项目。使用 Java 8 前需自行确认应用字节码和依赖兼容性。

## 运行包与备份

- Server：`/vol1/docker/jpom/jpom-server/lib/Server-2.11.12.1-java-path.jar`
- Agent：`/vol1/docker/jpom/jpom-agent/lib/Agent-2.11.12.1-java-path.jar`
- 停机完整备份：`/vol1/docker/jpom/backups/20261005-java-path`（Btrfs reflink 副本）；H2 数据库备份比较通过，H2 版本保持 2.1.214。
- 原配置、数据库、数据、启动脚本保持原位，仅新增包和切换 lib/run.bin。

SHA-256：

Server：c7236e49f6c17186de432626021362082d866da3abb4d95597e320d7bb93153d

Agent：f5fcb41fac7673f340cde00a6649cefaccc5427c3e2c2cce168c0cfd05781fa7

回滚时先停 systemd，再运行原 Server.sh/Agent.sh stop -s，确认实际 Jpom 进程停止。将本次备份对应 lib/run.bin 复制回原目录并重新启动单元，即可回到上一运行包；不需恢复数据库。若要回到最初官方包，可使用第一次备份 `20261005-jar-path` 的 run.bin。勿覆盖运行中的 H2 文件。
