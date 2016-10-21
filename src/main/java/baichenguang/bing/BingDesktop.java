package baichenguang.bing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

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
import org.apache.http.impl.client.HttpClients;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.win32.StdCallLibrary;

public class BingDesktop {

	private static final Log LOG = LogFactory.getLog(BingDesktop.class);
	private static final String PROPERTIES = "bing-desktop.properties";

	private final String bingSite;
	private final String bingSiteEncoding;
	private final String bingSiteWallpaperRegex;
	private final String bingSiteWallpaperDownloadPath;

	private final boolean isSetDesktopWallpaper;

	public BingDesktop() {
		try {
			Configuration config = new PropertiesConfiguration(PROPERTIES);
			this.bingSite = config.getString("bing.site.url");
			this.bingSiteEncoding = config.getString("bing.site.encode");
			this.bingSiteWallpaperRegex = config.getString("bing.site.wallpaper.regex");
			this.bingSiteWallpaperDownloadPath = config.getString("bing.site.wallpaper.download.path");
			this.isSetDesktopWallpaper = config.getBoolean("desktop.wallpaper.isset");
		} catch (ConfigurationException e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private String pictureUrl() {
		HttpGet request = new HttpGet(this.bingSite);
		try {
			HttpResponse response = HttpClients.createDefault().execute(request);
			List<String> lines = IOUtils.readLines(response.getEntity().getContent(), this.bingSiteEncoding);
			Pattern pattern = Pattern.compile(this.bingSiteWallpaperRegex);
			for (String line : lines) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.find())
					return matcher.group();
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			request.releaseConnection();
		}

		String errorMsg = "Picture not found.";
		LOG.error(errorMsg);
		throw new RuntimeException(errorMsg);
	}

	private String downloadPicture(String pictureUrl) {
		String formatedDate = DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd");
		String filePath = this.bingSiteWallpaperDownloadPath + "BingWallpaper-" + formatedDate + ".jpg";
		File picture = new File(filePath);
		try {
			URL url = new URL(pictureUrl);
			FileUtils.copyURLToFile(url, picture);
			LOG.info("Download success: " + filePath);
			return filePath;
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private void setDesktopWallpaper(String picturePath) {
		if (!isSetDesktopWallpaper)
			return;

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

	public static void main(String[] args) {
		BingDesktop bingDesktop = new BingDesktop();
		String pictureUrl = bingDesktop.pictureUrl();
		String picturePath = bingDesktop.downloadPicture(pictureUrl);
		bingDesktop.setDesktopWallpaper(picturePath);
	}

}
