# 打包说明

## Windows 可执行程序打包

### 前置要求
- JDK 17 或更高版本
- Maven 3.6+

### 打包步骤

1. 确保已安装Java 17+和Maven
2. 在项目根目录运行打包脚本：
   ```bash
   cd installer
   package.bat
   ```

3. 打包完成后，安装程序位于：`installer/output/RaceChipCheck-1.0.1.exe`

### 打包产物

- **RaceChipCheck-1.0.0.exe** - Windows安装程序
- 包含Java运行时（无需用户安装Java）
- 支持选择安装目录
- 自动创建开始菜单快捷方式
- 自动创建桌面快捷方式

### 安装程序特性

- 应用名称：RaceChipCheck
- 版本：1.0.0
- 描述：赛事选手芯片核验系统脱机版
- 厂商：Race Systems

### 注意事项

- 打包过程需要几分钟时间
- 确保有足够的磁盘空间（至少500MB）
- 打包后的安装程序大小约为200-300MB（包含Java运行时）
