package baichenguang.bing;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.win32.StdCallLibrary;
import net.sf.json.JSONObject;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;

public class BingDesktop {

    private static final Log LOG = LogFactory.getLog(BingDesktop.class);
    private static final String PROPERTIES = "bing-desktop.properties";

    private final String bingSiteWallpaperApi;
    private final String bingSiteWallpaperImageUrlPrefix;
    private final String bingSiteWallpaperDownloadPath;

    private final boolean isSetDesktopWallpaper;

    public BingDesktop() {
        try {
            Configuration config = new PropertiesConfiguration(PROPERTIES);
            this.bingSiteWallpaperApi = config.getString("bing.site.wallpaper.api");
            this.bingSiteWallpaperImageUrlPrefix = config.getString("bing.site.wallpaper.image.url.prefix");
            this.bingSiteWallpaperDownloadPath = config.getString("bing.site.wallpaper.download.path");
            this.isSetDesktopWallpaper = config.getBoolean("desktop.wallpaper.isset");
        } catch (ConfigurationException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private String pictureUrl() {
        HttpGet request = new HttpGet(this.bingSiteWallpaperApi);
        try {
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{tm}, null);
            httpClientBuilder.setSSLContext(ctx);

            HttpResponse response = httpClientBuilder.build().execute(request);
            String responseJsonString = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = JSONObject.fromObject(responseJsonString);
            String pictureUrl = this.bingSiteWallpaperImageUrlPrefix + ((JSONObject) jsonObject.getJSONArray("images").get(0)).getString("url");
            LOG.info("pictureUrl: " + pictureUrl);
            return pictureUrl;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private String downloadPicture(String pictureUrl) {
        String formattedDate = DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd");
        String filePath = this.bingSiteWallpaperDownloadPath + "BingWallpaper-" + formattedDate + ".jpg";
        File picture = new File(filePath);
        if (picture.exists()) {
            LOG.info("File is exist, filePath: " + filePath);
            return filePath;
        }

        try {
            URL url = new URL(pictureUrl);
            FileUtils.copyURLToFile(url, picture);
            LOG.info("Download success, filePath: " + filePath);
            return filePath;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void setDesktopWallpaper(String picturePath) {
        if (!isSetDesktopWallpaper) {
            LOG.info("Don't need set desktop wallpaper.");
            return;
        }

        switch (System.getProperty("os.name")) {
            case "Windows 10":
                setDesktopWallpaperForWindows(picturePath);
                break;
            case "Linux":
                setDesktopWallpaperForUbuntu(picturePath);
                break;
            case "Mac OS X":
                setDesktopWallpaperForMac(picturePath);
                break;
            default:
                LOG.warn("Unknown OS type.");
        }
    }

    private void setDesktopWallpaperForWindows(String picturePath) {
        LOG.info("Set desktop wallpaper for windows, picturePath: " + picturePath);
        String bmpPicturePath = this.transformJpgToBmp(picturePath);

        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, "Control Panel\\Desktop", "Wallpaper", bmpPicturePath);
        // WallpaperStyle = 10 (Fill), 6 (Fit), 2 (Stretch), 0 (Tile), 0 (Center)
        // For windows XP, change to 0
        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, "Control Panel\\Desktop", "WallpaperStyle", "10"); // fill
        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, "Control Panel\\Desktop", "TileWallpaper", "0"); // no tiling

        int SPI_SETDESKWALLPAPER = 0x14;
        int SPIF_UPDATEINIFILE = 0x01;
        int SPIF_SENDWININICHANGE = 0x02;
        boolean result = MyUser32.INSTANCE.SystemParametersInfoA(SPI_SETDESKWALLPAPER, 0, bmpPicturePath, SPIF_UPDATEINIFILE | SPIF_SENDWININICHANGE);
        if (result)
            LOG.info("Refresh desktop successful.");
        else
            LOG.error("Refresh desktop failed.");
    }

    private String transformJpgToBmp(String jpgPicturePath) {
        File jpgFile = new File(jpgPicturePath);
        File bmpFile = new File(this.bingSiteWallpaperDownloadPath + "desktop.bmp");
        try {
            BufferedImage image = ImageIO.read(jpgFile);
            ImageIO.write(image, "BMP", bmpFile);
            return bmpFile.getPath();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private interface MyUser32 extends StdCallLibrary {
        MyUser32 INSTANCE = (MyUser32) Native.loadLibrary("user32", MyUser32.class);

        boolean SystemParametersInfoA(int uiAction, int uiParam, String fnm, int fWinIni);
    }

    private void setDesktopWallpaperForUbuntu(String picturePath) {
        LOG.info("Set desktop wallpaper for ubuntu, picturePath: " + picturePath);
        try {
            Process process = Runtime.getRuntime().exec("gsettings set org.gnome.desktop.background picture-uri file://" + picturePath);
            String result = "";
            IOUtils.write(result, process.getOutputStream());
            LOG.info(result);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void setDesktopWallpaperForMac(String picturePath) {
        LOG.info("Set desktop wallpaper for mac, picturePath: " + picturePath);
        try {
//            String command = "osascript -e \"tell application \\\"Finder\\\" to set desktop picture to POSIX file \\\"" + picturePath + "\\\"\"";
            String command = "sh bing-desktop.sh " + picturePath;
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        BingDesktop bingDesktop = new BingDesktop();
        String pictureUrl = bingDesktop.pictureUrl();
        String picturePath = bingDesktop.downloadPicture(pictureUrl);
        bingDesktop.setDesktopWallpaper(picturePath);
    }
}
