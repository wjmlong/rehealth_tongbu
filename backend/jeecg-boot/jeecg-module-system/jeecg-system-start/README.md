# 这个是单体启动项目
- 项目： jeecg-module-system/jeecg-system-start
- 启动类：jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/JeecgSystemApplication.java

- 端口：8080
- 访问地址：http://localhost:8080/jeecg-boot
- 账号密码：admin/123456

## 本地 ReHealth 联调

本地环境使用 `dev,local` profile：`application-dev.yml` 提供 JeecgBoot 基础配置，
机器上的 `src/main/resources/application-local.yml` 覆盖 MySQL `rehealth_software`
和带密码的 Redis 连接，并关闭未使用的硬件库。该 local 配置文件包含本机开发凭据，
已加入 `.gitignore`，不要提交到远程仓库。

先确保 MySQL/Redis 容器已启动，再直接运行单体启动类或已打包 JAR：

```powershell
$env:JAVA_HOME = 'E:\codeDownload\jdk-17.0.20'
java -jar target\jeecg-system-start-3.9.2.jar --spring.profiles.active=dev,local
```

服务地址仍为 `http://127.0.0.1:8080/jeecg-boot`，官网 FastAPI 为
`http://127.0.0.1:8090`。


# 微服务启动项目在这里
- 项目： jeecg-server-cloud/jeecg-system-cloud-start
- 启动类：jeecg-server-cloud/jeecg-system-cloud-start/src/main/java/org/jeecg/JeecgSystemCloudApplication.java
