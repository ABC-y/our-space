# 用 Vercel + Railway 将“我们之间”正式部署到线上

完成本教程后，你们两个人可以在任意地点、不同 Wi-Fi 或手机流量下，打开同一个网址使用这个项目。

部署结构如下：

```text
手机浏览器
    |
    v
Vercel：网页、HTTPS
    |
    | /api 请求转发
    v
Railway：Java Spring Boot 后端
    |                         |
    v                         v
Railway MySQL 数据库       Railway 持久化磁盘
账号、故事、悄悄话         上传的照片
```

你们平时只需要打开 Vercel 的网址。Vercel 会把 `/api` 请求转发给 Railway，因此登录 Cookie 仍然属于同一个网页地址，手机登录会更稳定。

## 0. 开始前需要准备

请先注册并登录下面三个账号：

1. GitHub 账号。
2. Railway 账号，并授权它访问 GitHub。
3. Vercel 账号，并授权它访问 GitHub。

请将 GitHub 仓库设为 **Private（私有）**。不要把密码、MySQL 连接信息或 Railway 的环境变量上传到 GitHub。

项目使用 Java 21。Railway 会通过项目中已有的 `Dockerfile` 自动构建，不需要你在 Railway 手动安装 JDK。

Railway 的 MySQL 和持久化磁盘通常属于付费云资源。创建资源前请在 Railway 后台确认你当前套餐的价格和额度。

## 1. 将项目上传到 GitHub

1. 在浏览器打开 `https://github.com/new`。
2. Repository name 填写 `belong-us`。
3. 选择 **Private**。
4. 不要勾选 “Add a README”、`.gitignore` 或 License。
5. 点击 **Create repository**。
6. 复制 GitHub 页面显示的 HTTPS 仓库地址，格式类似：

```text
https://github.com/你的GitHub用户名/belong-us.git
```

7. 在项目根目录打开 PowerShell，执行下面的命令。最后一行中的地址要换成你刚复制的地址：

```powershell
cd "C:\Users\21123\Documents\belong us"
git add .
git commit -m "Prepare Vercel and Railway deployment"
git branch -M main
git remote add origin https://github.com/你的GitHub用户名/belong-us.git
git push -u origin main
```

如果 Git 提示不知道你的姓名或邮箱，请先执行下面两行，再重新执行 `git commit`：

```powershell
git config --global user.name "你的名字"
git config --global user.email "你的邮箱@example.com"
```

第一次推送时，GitHub 可能会弹出浏览器授权窗口。完成授权后回到 PowerShell 即可。

## 2. 在 Railway 创建 MySQL 数据库

1. 打开 Railway 并登录。
2. 点击 **New Project**。
3. 点击 **Add a Service** 或 **New**，选择 **Database**，再选择 **MySQL**。
4. 等待 MySQL 服务状态变为健康或可用。
5. 服务名称请保留为 `MySQL`。如果你修改了它的名字，后面的变量中每一处 `MySQL` 都要改成新名称。

不要为 MySQL 服务创建公网域名。Java 后端会通过 Railway 内部网络连接数据库，更安全。

## 3. 在 Railway 部署 Java 后端

1. 保持在同一个 Railway 项目中，点击 **New** 或 **Add a Service**。
2. 选择 **GitHub Repo** 或 **Deploy from GitHub repo**。
3. 选择刚才创建的私有 `belong-us` 仓库。
4. 如果 Railway 要求 GitHub 权限，只授权这个仓库即可。
5. 点击新建的 Java 服务，进入 **Settings**。
6. 在构建设置中确认：

```text
Root Directory：留空，表示仓库根目录
Builder：Dockerfile
Dockerfile Path：Dockerfile
```

7. 不需要填写 Start Command，项目中的 Dockerfile 已经定义了启动 Java 的方式。
8. 如果 Railway 允许选择部署地区，请选择你们两个人都较近的地区。对于亚洲用户，优先选择后台提供的亚洲或亚太地区。

### 为照片添加持久化磁盘

1. 在 Java 服务的 **Settings** 中找到 **Volumes**。
2. 点击 **Add Volume**。
3. Mount Path（挂载路径）填写：

```text
/data
```

4. 保存。

项目会把上传照片放在 `/data/uploads`。因为 `/data` 是 Railway 的持久化磁盘，正常重新部署后照片不会被删除。

### 添加环境变量

打开 Java 服务的 **Variables** 页面。可以使用 **Raw Editor** 一次性粘贴，也可以逐条添加。

如果 MySQL 服务名称保持为 `MySQL`，请填写下面内容：

```text
SPRING_PROFILES_ACTIVE=mysql
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
STORAGE_LOCATION=/data/uploads
COOKIE_SECURE=true
CORS_ALLOWED_ORIGIN_PATTERNS=https://*.vercel.app
```

注意：

- `${{MySQL.MYSQLHOST}}` 是 Railway 对 MySQL 服务变量的引用，不要替换成你电脑的 `localhost`。
- 如果数据库服务改名了，将三条数据库变量中的 `MySQL` 全部改为那个实际名称。
- Railway 的变量编辑器中不要额外添加英文双引号。
- `CORS_ALLOWED_ORIGIN_PATTERNS` 先用 `https://*.vercel.app`，第 5 节中会收紧成你自己的 Vercel 网址。

### 添加健康检查和后端公网域名

1. 仍然在 Java 服务的 **Settings** 中，找到 **Healthcheck Path**。
2. 填写：

```text
/api/health
```

3. 找到 **Networking**，点击 **Generate Domain**。
4. 复制生成的后端网址，格式类似：

```text
https://belong-us-production-abcd.up.railway.app
```

5. 等待最新部署变成绿色或成功状态。
6. 在浏览器打开：

```text
https://你的Railway域名.up.railway.app/api/health
```

正确时会显示：

```json
{"status":"ok"}
```

如果不能显示这个结果，请先打开 Java 服务的 **Deployments**，选中最新一次部署并查看日志。健康检查成功前，不要继续部署 Vercel。

## 4. 将 Vercel 网页连接到 Railway 后端

用 IntelliJ IDEA 打开 [frontend/vercel.json](frontend/vercel.json)。

找到：

```json
"destination": "https://REPLACE-WITH-YOUR-RAILWAY-DOMAIN.up.railway.app/api/:path*"
```

将整段目标地址改为你真实的 Railway 域名。例如：

```json
"destination": "https://belong-us-production-abcd.up.railway.app/api/:path*"
```

只替换域名部分，最后的 `/api/:path*` 必须保留。

保存文件，然后在 PowerShell 执行：

```powershell
cd "C:\Users\21123\Documents\belong us"
git add frontend/vercel.json
git commit -m "Configure production API proxy"
git push
```

**不要**在 Vercel 设置 `VITE_API_BASE_URL`。前端正式环境会使用 `/api`，并交给 Vercel 转发。若直接设置 Railway API 地址，手机上的登录 Cookie 可能会失效。

## 5. 在 Vercel 部署前端网页

1. 打开 Vercel 并登录。
2. 点击 **Add New...**，选择 **Project**。
3. 导入 `belong-us` GitHub 仓库。
4. 在点击 Deploy 前，打开 Root Directory 旁边的 **Edit**。
5. Root Directory 填写：

```text
frontend
```

6. 确认构建配置如下：

```text
Framework Preset：Vite
Install Command：npm ci
Build Command：npm run build
Output Directory：dist
```

7. Environment Variables（环境变量）保持为空。
8. 点击 **Deploy**。
9. 部署完成后，点击 **Visit**，复制稳定的网址，格式类似：

```text
https://belong-us.vercel.app
```

### 将 CORS 收紧为真实网页地址

回到 Railway：

1. 打开 Java 服务。
2. 进入 **Variables**。
3. 把 `CORS_ALLOWED_ORIGIN_PATTERNS` 从：

```text
https://*.vercel.app
```

改为你的真实 Vercel 网址，例如：

```text
https://belong-us.vercel.app
```

4. Railway 会因变量修改而重新部署，等待它再次变为成功状态。

之后，两个人都只使用 Vercel 的网址，不要使用 Railway 的网址。

## 6. 第一次异地真实测试

请使用两台不同的手机，或者至少使用两个不同浏览器。最好有一台手机使用移动数据网络。

1. A 打开 Vercel 网址。
2. A 注册账号，创建双人空间，记下邀请码。
3. A 将 Vercel 网址和邀请码发给 B。
4. B 在自己的手机打开同一个 Vercel 网址，注册另一个账号，选择“加入空间”，输入邀请码。
5. 两人各新增一条回忆，上传一张照片，并写一段悄悄话。
6. 两人都刷新网页，确认仍保持登录，并都能看到相同内容。
7. 在 Railway 中对 Java 服务点击一次 **Redeploy**。等后端恢复健康后，确认照片、故事、悄悄话仍然存在。

最后一步会验证两类长期保存：

- MySQL 保存账号、空间、故事、悄悄话和回复。
- Railway Volume 保存上传的照片文件。

## 7. 以后如何更新项目

以后正常修改代码后，在项目根目录执行：

```powershell
cd "C:\Users\21123\Documents\belong us"
git add .
git commit -m "描述这次修改"
git push
```

推送到 `main` 分支会自动触发：

- Railway 重新部署后端；
- Vercel 重新部署前端。

不要删除 Railway 的 MySQL 服务和 Volume，也不要将 Volume 的挂载路径从 `/data` 改掉。

## 8. 安全与备份

持久化磁盘可以避免正常重新部署导致的数据丢失，但它不是完整备份。

1. GitHub 仓库保持私有。
2. 不要将 Railway 密码或数据库连接信息提交到 GitHub。
3. 不要公开 Vercel 网址和双人邀请码。
4. 重要改动前，导出一份 MySQL 数据，并把照片额外保存到其他位置。
5. Railway 是否提供数据库或磁盘备份取决于当前套餐，请在后台确认后再依赖它。

项目中的 `scripts/backup-production.sh` 是给 `README.md` 中“自建 Docker 服务器”方案使用的，不能直接用于本 Vercel + Railway 方案。

## 9. 常见问题排查

### Vercel 网页能打开，但提示无法打开空间

1. 打开 `https://你的Railway域名.up.railway.app/api/health`。
2. 如果不是 `{"status":"ok"}`，先看 Railway 的部署日志。
3. 检查 [frontend/vercel.json](frontend/vercel.json)，Railway 域名必须正确，并且结尾必须是 `/api/:path*`。
4. 确认 Vercel 的 Root Directory 是 `frontend`。

### 注册或登录后立刻又回到登录页

1. 确认 Railway 变量中有 `COOKIE_SECURE=true`。
2. 确认你打开的是 HTTPS 的 Vercel 网址。
3. 确认 Vercel 中没有设置 `VITE_API_BASE_URL`。
4. 确认前端请求的是 `/api`，而非直接请求 Railway 域名。

### Railway 提示无法连接 MySQL

1. 确认 `SPRING_PROFILES_ACTIVE=mysql`。
2. 确认所有数据库引用变量中的服务名都与 MySQL 服务名相同。
3. `DB_URL` 中不能出现 `localhost`。
4. 保存变量后重新部署 Java 服务。

### 上传的新照片在重新部署后消失

1. 确认 Java 服务创建了挂载在 `/data` 的 Railway Volume。
2. 确认 `STORAGE_LOCATION=/data/uploads`。
3. 如果照片是在创建 Volume 之前上传的，它们可能还在临时容器磁盘中；Volume 配好后请重新上传这些照片。

### 刷新网页后 Vercel 显示 404

确认 [frontend/vercel.json](frontend/vercel.json) 中仍保留了第二条转发规则，即所有前端页面回到 `/index.html`，交给 React 继续处理。

## 10. 遇到问题时发给我什么

请发：

1. Railway 或 Vercel 相关页面的截图。
2. 完整的错误提示。
3. `/api/health` 是否能打开。
4. Java 服务在报错附近的部署日志。

不要发送密码、数据库变量、登录 Cookie 或双人邀请码。
