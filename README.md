# bing-desktop
## 功能
下载必应图片，并设置为操作系统桌面
## 需求
JDK 1.7.x  
Apache Maven 3.3.x
## 测试
Windows 7, 10 下运行正常  
***未针对 Windows XP, 8 做测试***
## 配置
src/main/resources/bing-desktop.properties  

    #图片下载地址
    bing.site.wallpaper.download.path
    #是否将下载的图片设置成桌面
    desktop.wallpaper.isset

setenv.bat  
自行修改 JAVA_HOME 和 M2_HOME
## 使用方法
运行 run-main.bat
