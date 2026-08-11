# 我们之间

一个只给两个人使用的私密网页空间：保存照片、记录故事、写悄悄话并回复。

当前项目已经包含：

- Java 21 + Spring Boot 后端
- React + Vite 手机网页
- 注册、登录、HttpOnly 会话 Cookie
- 创建双人空间、8 位邀请码加入、每个空间最多两人
- 回忆、照片上传、悄悄话和回复
- MySQL 长期存储、私密图片访问校验
- Docker + Caddy 自动 HTTPS 的生产部署配置
- 数据库和照片备份脚本

## 先理解数据会保存在哪里

本地开发时，默认使用 H2 内存数据库。程序停止后，测试账号、回忆和照片记录会消失，这是正常的。

正式部署时，数据会分成两部分：

| 内容 | 保存位置 | 是否会随容器重建消失 |
| --- | --- | --- |
| 账号、情侣空间、故事、悄悄话、回复 | MySQL Docker 持久卷 `mysql-data` | 不会 |
| 上传的照片文件 | Docker 持久卷 `uploads-data` | 不会 |

照片不能被公开链接直接访问。每次读取图片时，后端都会确认当前登录账号是不是该双人空间的成员。

注意：持久卷能防止普通的容器重建丢数据，但不能防止服务器硬盘损坏、误删服务器或账号被回收。所以正式上线后一定要运行本项目的备份脚本，并将备份下载到本地或同步到云盘/对象存储。

## 本地启动

### 1. 启动 Java 后端

在项目根目录打开 PowerShell：

```powershell
mvn -s .mvn\settings.xml -gs .mvn\settings.xml spring-boot:run
```

看到 `Tomcat started on port 8080` 就表示后端已启动。

### 2. 启动前端

再打开一个 PowerShell：

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

在电脑浏览器打开：

```text
http://127.0.0.1:5173
```

第一次使用时：

1. 第一个人点击“第一次来”，创建自己的昵称、账号和私密口令。
2. 创建双人空间，填写空间名字与关系开始日期。
3. 首页会出现 8 位邀请码，复制后发给对方。
4. 第二个人在自己的浏览器打开同一个网址，创建自己的身份后选择“加入空间”，输入邀请码。
5. 两个人都可以写回忆、上传照片、写悄悄话和回复。

### 3. 同一 Wi-Fi 下让手机体验

前端已经会自动跟随你打开网页时使用的 IP 地址访问后端，不需要再改前端代码。

1. 在 Windows PowerShell 输入 `ipconfig`，记下无线网卡的 IPv4 地址，例如 `192.168.1.8`。
2. 保持前端和后端两个窗口都在运行。
3. 手机和电脑连接同一个 Wi-Fi。
4. 手机浏览器打开 `http://192.168.1.8:5173`。
5. Windows 防火墙弹出提示时，允许 Java 与 Node.js 通过“专用网络”。

这只是局域网体验。电脑关机、换网络或离开同一个 Wi-Fi 后，手机就无法访问。要让两个人异地使用，需要完成下面的正式部署。

## 正式上线：让两部手机在任何地方都能使用

正式架构如下：

```text
两部手机
    |
https://你的域名
    |
Caddy 自动 HTTPS
    |
React 网页 + Nginx
    |
/api
    |
Spring Boot
    |------------------|
MySQL 持久卷       照片持久卷
```

页面和接口使用同一个域名，因此登录 Cookie 可以正常工作，不需要将数据库端口或 Java 端口暴露到公网。

### 第 1 步：准备你需要购买/开通的东西

你需要自己完成这些与身份、付款有关的操作：

1. 一台 Linux 云服务器，建议 Ubuntu 24.04、2 核 CPU、2 GB 内存、40 GB 磁盘起步。
2. 一个域名，例如 `us.example.com`。
3. 云服务器安全组放行 TCP `80` 和 `443`；SSH 的 `22` 端口建议只允许你的 IP 登录。
4. 在域名 DNS 控制台增加一条 `A` 记录，让你的域名指向云服务器的公网 IPv4 地址。

如果你准备选择中国大陆地区的云服务器，请在购买前向云厂商确认当前备案要求；这一步需要你本人实名认证和提交材料。

### 第 2 步：把项目上传到服务器

下面以服务器 IP 为 `203.0.113.10`、服务器账号为 `root` 举例。请替换成你自己的信息。

先在 Windows PowerShell 把项目打包。不要上传 `node_modules`、`target`、`.m2`、`uploads`、`.idea` 和 `frontend/dist`：

```powershell
cd "C:\Users\21123\Documents\belong us"
tar.exe -a -c -f "$env:USERPROFILE\Desktop\belong-us.zip" `
  --exclude=node_modules `
  --exclude=frontend/node_modules `
  --exclude=target `
  --exclude=.m2 `
  --exclude=uploads `
  --exclude=.idea `
  --exclude=frontend/dist `
  .
```

上传压缩包：

```powershell
scp "$env:USERPROFILE\Desktop\belong-us.zip" root@203.0.113.10:/opt/
```

登录服务器并解压：

```bash
ssh root@203.0.113.10
apt update
apt install -y unzip
mkdir -p /opt/belong-us
unzip -q /opt/belong-us.zip -d /opt/belong-us
cd /opt/belong-us
```

### 第 3 步：在 Ubuntu 服务器安装 Docker

在服务器执行下面命令。它使用 Docker 官方 apt 仓库安装 Docker Engine 和 Docker Compose 插件：

```bash
apt update
apt install -y ca-certificates curl
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
docker run hello-world
docker compose version
```

### 第 4 步：填写正式环境密码和域名

仍在服务器的 `/opt/belong-us` 目录：

```bash
cp .env.production.example .env.production
openssl rand -base64 36
openssl rand -base64 36
nano .env.production
```

将 `.env.production` 改成类似下面的内容：

```text
DOMAIN=us.example.com
ACME_EMAIL=your-real-email@example.com
MYSQL_PASSWORD=把第一条随机密码粘贴到这里
MYSQL_ROOT_PASSWORD=把第二条随机密码粘贴到这里
```

要求：

- `DOMAIN` 必须是刚刚完成 A 记录解析的真实域名，不要写 `http://` 或 `https://`。
- `ACME_EMAIL` 写可收邮件的真实邮箱，用于 HTTPS 证书通知。
- 两个 MySQL 密码必须不同、足够长，并且不能发给任何人。
- `.env.production` 已在 `.gitignore` 中，绝对不要提交到公开仓库。

### 第 5 步：启动正式服务

先检查 Docker Compose 配置是否正确：

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml config
```

没有报错后启动：

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=100 caddy
```

第一次启动时，Caddy 会在你的 DNS 已解析且服务器 `80`、`443` 可从公网访问的前提下，自动申请 HTTPS 证书。成功后，用手机打开：

```text
https://us.example.com
```

若打不开，按这个顺序排查：

1. 确认 DNS 的 A 记录已指向正确公网 IP。
2. 确认云服务器安全组已经放行 `80`、`443`。
3. 运行 `docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 caddy`。
4. 运行 `docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 api`。

### 第 6 步：确认两个人都能用

1. 第一个人用手机打开正式域名，点击“第一次来”，注册自己的账号。
2. 创建一个空间，复制首页的邀请码。
3. 将正式网址和邀请码发给对方。
4. 第二个人在自己的手机打开正式网址，注册自己的账号，进入“加入空间”，输入邀请码。
5. 两个人分别写一条回忆和一封悄悄话，确认可以互相看到。

邀请码只能让第二个成员加入；空间满两个人后，第三个账号无法加入。

## 长期保存与备份

项目提供了 [`scripts/backup-production.sh`](scripts/backup-production.sh)。它会导出 MySQL 数据库，并打包所有上传照片。

第一次使用时，在服务器执行：

```bash
cd /opt/belong-us
chmod +x scripts/backup-production.sh
./scripts/backup-production.sh
ls -lh backups
```

每天凌晨 3 点自动备份：

```bash
crontab -e
```

加入这一行：

```cron
0 3 * * * cd /opt/belong-us && ./scripts/backup-production.sh >> /var/log/belong-us-backup.log 2>&1
```

脚本默认保留 30 天备份。至少每月将 `backups/` 中最新的 `.sql.gz` 和 `.tar.gz` 下载到你的电脑、移动硬盘或另一个云存储中。只保留在同一台服务器上的备份，不能防止整台服务器损坏或误删。

从 Windows 下载某个备份的示例：

```powershell
scp root@203.0.113.10:/opt/belong-us/backups/belong-us-mysql-YYYYMMDD-HHMMSS.sql.gz "$env:USERPROFILE\Downloads\"
scp root@203.0.113.10:/opt/belong-us/backups/belong-us-uploads-YYYYMMDD-HHMMSS.tar.gz "$env:USERPROFILE\Downloads\"
```

## 日后更新项目

把新代码重新上传到 `/opt/belong-us` 后，在服务器执行：

```bash
cd /opt/belong-us
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

不要运行会删除 Docker 卷的命令，例如 `docker compose down -v`。`-v` 会删除数据库和照片持久卷。

## 验证命令

前端生产构建：

```powershell
cd frontend
npm.cmd run build
```

后端集成测试：

```powershell
mvn -s .mvn\settings.xml -gs .mvn\settings.xml test
```
