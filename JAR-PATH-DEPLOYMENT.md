# 2.11.12.1 自定义 JAR 全路径移植

- 官方基线：dromara/Jpom beta，34dd258c8f428f35b76d4ae7bf2e674c41b8bb30（commit beta 2.11.12.1）。
- 分支：codex/jar-path-2.11.12.1。
- Jar 模式新增可选 jarPath；使用绝对路径，留空继续扫描项目目录。现有校验不允许空格，文件须为 .jar。
- 构建：前端 Vite、目标 ESLint、JDK 21 Maven Server/Agent package 均通过。Maven 构建跳过测试；另用构建后 Agent 的实际类验证绝对路径优先、trim 和原目录回退，均通过。

## 部署记录

2026-10-05 通过 Termius 更新 ssh.pnkx.top。仅新增运行包并修改 lib/run.bin，保留原脚本、配置、数据库和数据目录；H2 保持 2.1.214。

- Server：/vol1/docker/jpom/jpom-server/lib/Server-2.11.12.1-jar-path.jar
- Agent：/vol1/docker/jpom/jpom-agent/lib/Agent-2.11.12.1-jar-path.jar
- 停机完整备份：/vol1/docker/jpom/backups/20261005-jar-path（Btrfs reflink 独立副本）。数据库和 Agent 项目配置逐字节比较通过。
- Server /check-system 返回 200；Agent 授权项目查询返回 200，23 个项目；project.json 和 agent_authorize.json 与备份一致；两个 systemd 服务 active；原业务 PID 15270、1742984 未变化。
- 未修改任何现有项目的运行配置，未启动/停止业务应用来测试新字段。

## 回滚运行包

先停 systemd 服务，随后用原 Server.sh / Agent.sh stop -s 停止实际 Jpom 进程（当前单元没有 ExecStop，不能只依赖 systemctl stop）。确认进程已停止，将备份中对应 lib/run.bin 复制回原安装目录，再启动 jpom-agent、jpom-server。旧 JAR 仍在原 lib 目录。若需恢复数据库，请先停服务并另外备份当前数据，勿直接覆盖运行中的 H2 文件。

## 包校验 SHA-256

Server：902d52026780ee2d5ffdd67c5af82ab97a2b54aa9fe6247fc4f8d040f5f65dc9

Agent：87877c658561f83050e4f2c03c69b4ff73b636da57fd1025d0d00510da95c389
