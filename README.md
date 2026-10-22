# Simple Attendance by Seat Id — Render 部署包

这就是 `RenderLab.docx` 里那个 `attendance` 工程的完整文件，**已经改成"云端构建"**：
Render 拉取仓库后自己用 `javac` 编译 servlet、打成 `ROOT.war` 再启动 Tomcat，
所以你本机**不需要装 JDK、NetBeans、Tomcat、Docker**，只要能上网、有 GitHub 账号即可。

## 一、最快路径（网页版 GitHub，5 分钟拿到 URL）

0. 先找到要上传的文件夹：`...\default-workspace\attendance-upload`
   （这是去掉 `.git` 的干净副本；或者解压 `attendance-deploy.zip`）。
1. 打开 <https://github.com> → 右上角 **+ → New repository**：
   - Repository name：`attendance`
   - 选 **Public**
   - **不要**勾 Add a README / .gitignore / license
   - 点 **Create repository**
2. 在新仓库页面中间点蓝色链接 **uploading an existing file**，
   把 `attendance-upload` 里的**全部内容**拖进浏览器：
   `src/`、`web/`、`docs/`、`Dockerfile`、`entrypoint.sh`、`README.md`、`.gitattributes`
   （`.gitattributes` 是隐藏文件，资源管理器要开"查看 → 隐藏的项目"才看得到；
   不传也能构建，网页上传不会改换行符）
   等文件列表刷出来后点下面的 **Commit changes**。
3. 打开 <https://render.com> → **Sign up / Sign in with GitHub**（用 GitHub 登录并授权）。
4. **New + → Web Service** → 选 `attendance` 仓库 → 填：
   - **Name**：`attend` + 你学号后 5 位，例如学号 `25406040217` → `attend40217`
   - **Region**：Singapore（离国内最近）
   - **Runtime**：Docker（会自动识别 Dockerfile）
   - **Branch**：main，**Instance Type**：Free
5. 点 **Create Web Service**，等状态变成 **Live**（第一次构建 3–5 分钟）。
6. 打开 `https://attend40217.onrender.com/`，按下面"验收"走一遍，然后把地址填进第 1 题。

> 用 GitHub Desktop 的话：`File → Add Local Repository…` 选 `attendance` 文件夹 →
> **Publish repository** → 取消勾选 "Keep this code private" → **Publish**。

## 二、验收（和 docx 第 9 部分一致）

- 打开根地址 → 看到 Student Id / Seat Id 表单（不是 404，也不是 Tomcat 欢迎页）
- 提交 `001` / `A1` → 出现 `001:A1 is recorded:`
- 点 **Attendee List** → 列表里有 `001 : A1`
- 学号留空提交 → 出现 `Student Id can not be blank`
- 手机流量再开一次，确认外网能访问

## 三、几个关键点

- **为什么部署在根路径**：容器里把 war 复制成 `ROOT.war`，所以 `https://xxx.onrender.com/`
  直接就是提交页，和 docx 第 8、9 部分写的根地址一致。
  如果你按 docx 原始流程用 NetBeans 打出 `attendance.war`，地址会变成
  `https://xxx.onrender.com/attendance/`——两种都能用，但别混着来。
- **免费实例会休眠**：闲置约 15 分钟会睡，下一次访问要 30–60 秒冷启动，
  期间浏览器可能直接超时。交作业/演示前先自己打开一次"唤醒"。
- **数据只在内存里**：服务一重启、一重新部署，`sid2seat` 就清空（这是 docx 里
  故意保留的教学设计，第 2 题正好可以拿来说）。
- **`.gitattributes` 别删**：它保证 `entrypoint.sh` 是 LF 换行。用 Git 在 Windows
  推送时如果被转成 CRLF，容器里会报 `bad interpreter: /bin/bash^M`。
- **换镜像**：如果 Render 构建时报 `pull access denied` / 找不到基础镜像，把 `Dockerfile`
  里两处 `tomcat:9.0-jdk11-temurin` 改成 `tomcat:9.0-jdk17-temurin`（Tomcat 9 支持 Java 17，
  代码是 Java 11 写法，不用改）。

## 四、如果老师要求完全按 docx 走（NetBeans + Ant + 本地 war）

1. NetBeans 建 Java Web → Web Application，名字 `attendance`，不要选框架。
2. 把 `web/index.html`、`web/WEB-INF/web.xml`、`src/java/attendance/Attend.java` 覆盖进去。
3. F11 构建 → `jar -cvf attendance.war -C build\web .`
4. 用 docx 第 6 部分的 Dockerfile（`COPY attendance.war .../attendance.war`）+
   `entrypoint.sh` 推到 GitHub，Render 部署出来的地址是 `https://xxx.onrender.com/attendance/`。
5. 把 `attendance.war` 改名成 `ROOT.war`、Dockerfile 里也改成 `ROOT.war`，
   就能回到根路径。

## 五、第 2、3 题的参考答案

见 `docs/quiz-answers.md`。

## 六、本地验证记录（2026-10-22 实测）

用 Tomcat 9.0.123 + OpenJDK 11 在本机跑过一遍，确认：

- `javac` 编译 `Attend.java` 无错，`ROOT.war` 部署后 `http://localhost:18080/` 返回提交页；
- 提交 `25406040217` / `A1` → 回显 `is recorded:`；空学号 → `Student Id can not be blank`；
- 原始 `HashMap` 版本，按 236 → 217 → 230 → 227 → 263 的顺序提交，名单输出是
  `230, 263, 227, 217, 236` —— 完全不按学号，也不按插入顺序（这就是第 3 题要改的证据）；
- 换成 `TreeMap` 版本后，同样提交，名单输出 `217, 227, 230, 236, 263`，升序正确。
