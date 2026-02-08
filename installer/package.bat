@echo off
echo ========================================
echo 赛事选手芯片核验系统 - Windows 打包工具
echo 版本: 1.0.0
echo ========================================
echo.

REM 设置Java路径（使用系统Java）
set JAVA_HOME=%JAVA_HOME%

REM 检查Java环境
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到Java环境，请确保Java 17+已安装
    pause
    exit /b 1
)

echo 步骤1: 清理之前的构建...
if exist target rmdir /s /q target
if exist installer\output rmdir /s /q installer\output

echo.
echo 步骤2: 使用Maven编译并打包...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo 错误: Maven打包失败
    pause
    exit /b 1
)

echo.
echo 步骤3: 使用jpackage创建Windows安装程序...
jpackage --version >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到jpackage工具，请确保使用Java 17+
    pause
    exit /b 1
)

REM 创建输出目录
if not exist installer\output mkdir installer\output

REM 使用jpackage打包
jpackage ^
  --input target ^
  --name "RaceChipCheck" ^
  --main-jar race-chipcheck-1.0.0.jar ^
  --main-class com.race.chipcheck.RaceChipCheckApp ^
  --type exe ^
  --dest installer\output ^
  --app-version 1.0.0 ^
  --description "赛事选手芯片核验系统" ^
  --vendor "Race Systems" ^
  --copyright "Copyright 2026" ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut

if %errorlevel% neq 0 (
    echo 错误: jpackage打包失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo 打包完成！
echo 安装程序位置: installer\output\
echo ========================================
pause
