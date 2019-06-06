package com.app.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class GdPart implements Runnable {

	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	List<String> clicked = new ArrayList<String>();

	public static void addContactUrl(String url) {

		Connection conn = TestSelenium.conn;
		try {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO  gd_contact_url (url)VALUES(?)");
			ps.setString(1, url);
			ps.execute();

		} catch (SQLException e) {
			System.out.println("插入失败:" + e.getMessage());
		}

		return;
	}

	public void log(String msg) {

		msg = Thread.currentThread().getName() + ":" + sdf.format(new Date()) + ":" + msg + "\n";
		System.out.println(msg);

	}

	public void searchIt(WebElement ele, int lv) {

		List<WebElement> alist = ele.findElements(By.tagName("a"));

		for (WebElement v : alist) {

			String id = v.getAttribute("id");

			if (id.matches("^OrgTreeForAddress1_trvOrgListn\\d+$")) {

				if (clicked.indexOf(id) == -1) {

					clicked.add(id);

					if ("OrgTreeForAddress1_trvOrgListn0".equals(id)) {
						return;
					}

					log("id:" + id);

					String num = id.substring("OrgTreeForAddress1_trvOrgListn".length(), id.length());

					String nodesIds = "OrgTreeForAddress1_trvOrgListn" + num + "Nodes";

					log("nodesIds:" + nodesIds);

					WebElement nodes = null;

					while (null == nodes) {

						try {
							v.click();
							Thread.sleep(2000);
							nodes = ele.findElement(By.id(nodesIds));
							log("戳爆" + id + "的子节点");
							searchIt(nodes, lv + 1);
						} catch (Exception e) {
							nodes = null;
							log(nodesIds + "戳不开，再戳一遍直到戳爆");
						}

					}

				}

			}

		}

	}

	@Override
	public void run() {

		ChromeOptions co = new ChromeOptions();
		co.setHeadless(TestSelenium.silent);

		WebDriver driver = new ChromeDriver(co);

		String url = "http://gdeiac-sso.gdtel.com/address/AddressTree.aspx";

		driver.get(url);

		WebElement un = driver.findElement(By.name("txtUserName"));
		WebElement pw = driver.findElement(By.name("txtPassword"));

		un.sendKeys(TestSelenium.oaUn);
		pw.sendKeys(TestSelenium.oaPw);

		WebElement bt = driver.findElement(By.id("Button1"));

		bt.click();

		int end = 0;
		while (true) {
			try {
				WebElement nodes = driver.findElement(By.id("OrgTreeForAddress1_trvOrgListn0Nodes"));
				log("戳爆OrgTreeForAddress1_trvOrgListn0的子节点");
				searchIt(nodes, 0);
				end = 1;
			} catch (Exception e) {
				log("没东西戳，赶紧登录");
			}

			if (end == 1) {
				break;
			}
		}

		List<WebElement> alist = driver.findElements(By.tagName("a"));

		alist.forEach((v) -> {

			String href = v.getAttribute("href");
			log(href);

			if (null != href && !"".equals(href)
					&& href.matches("^http\\://gdeiac-sso.gdtel.com/address/AddressView.aspx\\?namevalue\\=.*$")) {
				addContactUrl(href);
				log("url:" + href);
			}

		});

		driver.close();

	}

}
