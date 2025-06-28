@echo off
echo Starting Misoto Codebase Indexer...
echo.

REM Suppress Java warnings and Spring Boot startup info
java ^
  --enable-native-access=ALL-UNNAMED ^
  --add-opens java.base/sun.nio.ch=ALL-UNNAMED ^
  --add-opens java.base/java.io=ALL-UNNAMED ^
  --add-opens java.base/java.util=ALL-UNNAMED ^
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED ^
  --add-opens java.base/java.lang=ALL-UNNAMED ^
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
  --add-opens java.base/java.text=ALL-UNNAMED ^
  --add-opens java.desktop/java.awt.font=ALL-UNNAMED ^
  -Dspring.main.banner-mode=off ^
  -Dlogging.level.org.springframework=WARN ^
  -Dlogging.level.org.springframework.boot=WARN ^
  -Dlogging.level.org.springframework.boot.StartupInfoLogger=OFF ^
  -Dlogging.level.io.grpc=OFF ^
  -Dlogging.level.io.netty=OFF ^
  -jar target/indexer-0.0.1-SNAPSHOT.jar

pause
