# bing-desktop
功能：

  下载必应图片，并设置为操作系统桌面

需求：

  JDK 7

  Apache Maven 3.3.x

测试：

  Windows 7下运行正常

  未针对Windows xp, 8, 10做测试

配置：

  bing-desktop.properties

    bing.site.wallpaper.download.path：图片下载地址

    desktop.wallpaper.isset：是否将下载的图片设置成桌面

  setenv.bat

    自行修改JAVA_HOME和M2_HOME

使用方法：

  运行run-main.bat
