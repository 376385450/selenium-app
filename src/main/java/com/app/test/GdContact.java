package com.app.test;

import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class GdContact implements Runnable {

	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	List<Map<String, String>> list = null;

	public GdContact(List<Map<String, String>> list) {
		this.list = list;
	}

	public void log(String msg) {

		msg = Thread.currentThread().getName() + ":" + sdf.format(new Date()) + ":" + msg + "\n";
		System.out.println(msg);

	}

	private void addContact(String user, String name, String tel, String short_tel, String ph, String email,
			String gid) {

		try {
			Connection conn = TestSelenium.conn;
			PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO gd_oa_contact (user,name,tel,short_tel,ph,email,gid)VALUES(?,?,?,?,?,?,?)");
			ps.setString(1, user);
			ps.setString(2, name);
			ps.setString(3, tel);
			ps.setString(4, short_tel);
			ps.setString(5, ph);
			ps.setString(6, email);
			ps.setString(7, gid);
			ps.execute();

		} catch (Exception e) {
			System.out.println("插入失败:" + gid + "|" + e.getMessage());
		}

		return;

	}

	private void handlePage(WebDriver driver, String gid) {

		WebElement userlist = driver.findElement(By.id("GridViewUserList"));

		List<WebElement> trs = userlist.findElements(By.tagName("tr"));

		for (WebElement vv : trs) {

			String cn = vv.getAttribute("class");

			if ("tableTitleblue".equals(cn)) {
				continue;
			}

			List<WebElement> tds = vv.findElements(By.tagName("td"));

			String[] item = new String[6];
			int col = 0;
			for (WebElement vvv : tds) {
				item[col] = vvv.getText().trim();
				col++;
			}

			addContact(item[0], item[1], item[2], item[3], item[4], item[5], gid);
		}
	}

	private String[] getPage(WebDriver driver) {

		WebElement pageBar = driver.findElement(By.id("UCPaging1_lbPageComment"));
		List<WebElement> sps = pageBar.findElements(By.tagName("span"));

		String[] pages = new String[2];

		int i = 0;
		for (WebElement v : sps) {
			String text = v.getText();

			if (null == text || "".equals(text)) {
				throw new RuntimeException("空page:" + i);
			}

			pages[i] = text;
			i++;

			if (i > 1) {
				break;
			}
		}

		return pages;
	}

	@Override
	public void run() {

		ChromeOptions co = new ChromeOptions();
		co.setHeadless(TestSelenium.silent);

		WebDriver driver = new ChromeDriver(co);

		String url = "http://gdeiac-sso.gdtel.com/address/AddressView.aspx";

		driver.get(url);

		WebElement un = driver.findElement(By.name("txtUserName"));
		WebElement pw = driver.findElement(By.name("txtPassword"));

		// 账号
		un.sendKeys(TestSelenium.oaUn);

		// 密码
		pw.sendKeys(TestSelenium.oaPw);
		
		WebElement bt = driver.findElement(By.id("Button1"));
		bt.click();

		for (Map<String, String> v : list) {
			try {

				String tmpUrl = v.get("url");

				if (null == tmpUrl || "".equals(tmpUrl)) {
					throw new RuntimeException("空url");
				}

				tmpUrl = URLDecoder.decode(tmpUrl, "UTF-8");

				String nv = tmpUrl.substring(tmpUrl.indexOf("=") + 1, tmpUrl.length());
				String[] nvp = nv.split(",");
				String gid = nvp[1];

				if (null == gid || "".equals(gid)) {
					throw new RuntimeException("空gid:" + nv);
				}

				log("戳爆页面开始：" + tmpUrl);

				WebElement pageNum = null;
				List<WebElement> pageBtns = null;

				int page = 0;
				int total = 0;

				int ok = 0;

				while (ok == 0) {

					try {

						driver.get(tmpUrl);

						String[] pages = getPage(driver);

						if (pages.length < 2) {
							throw new RuntimeException("页码异常：" + pages);
						}

						if (0 == Integer.valueOf(pages[1]).intValue()) {
							page = 0;
							break;
						}

						total = Integer.valueOf(pages[1]).intValue();

						handlePage(driver, gid);

						WebElement pageNum1 = driver.findElement(By.id("UCPaging1_tbPage"));
						pageNum = pageNum1;

						page = 1;

						ok = 1;

					} catch (Exception e) {
						e.printStackTrace();
						log("页面初次进入失败：" + tmpUrl + ":" + e.getMessage() + "继续戳爆");
					}
				}

				if (page < 1) {
					throw new RuntimeException("无数据");
				}

				int over = 0;

				while (over == 0) {

					if (page >= total) {
						log("url:" + gid + "戳完了");
						over = 1;
						break;
					}

					if (null == pageNum) {
						throw new RuntimeException("无效pageNum");
					}

					pageBtns = pageNum.findElements(By.tagName("a"));

					for (WebElement vv : pageBtns) {

						String pStr = vv.getText();

						if (null == pStr) {
							throw new RuntimeException("空pStr");
						}

						pStr = pStr.trim();

						if ("下一页".equals(pStr)) {

							try {

								vv.click();

								String[] pages = getPage(driver);

								if (Integer.valueOf(pages[0]).intValue() != (page + 1)) {
									throw new RuntimeException("页码不对");
								}

								handlePage(driver, gid);

								WebElement pageNum1 = driver.findElement(By.id("UCPaging1_tbPage"));

								pageNum = pageNum1;

								page = page + 1;

								log("第" + page + "页，完成");

							} catch (Exception e) {
								log("戳第" + page + "页，失败继续戳");
							}

							break;
						}

					}

				}

			} catch (Exception e) {
				e.printStackTrace();
				log("@出错了，继续戳:" + v.get("url") + "|" + e.getMessage());
				continue;
			}

		}

		driver.close();
	}

}
