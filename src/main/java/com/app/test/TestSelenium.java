package com.app.test;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class TestSelenium {

	static Logger log = Logger.getLogger(TestSelenium.class);

	public static Connection conn;

	// 数据库url
	public static String dbUrl = "jdbc:mysql://localhost:3306/dianxin?useUnicode=true&characterEncoding=UTF-8";

	// 数据库账号
	public static String dbUser = "root";

	// 数据库密码
	public static String dbPw = "123456";

	// oa账号
	public static String oaUn = "";

	// oa密码
	public static String oaPw = "";

	// driver路径
	public static String driverPath = "/home/web/dxcj/chromedriver";

	// chrome模式
	public static boolean silent = true;

	static List<String> clicked = new ArrayList<String>();

	public static List<Map<String, String>> getContactUrl() {

		LinkedList<Map<String, String>> r = new LinkedList<Map<String, String>>();

		try {

			PreparedStatement ps = conn.prepareStatement("SELECT * FROM gd_contact_url ORDER BY id DESC");
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {

				Map<String, String> item = new HashMap<String, String>();

				ResultSetMetaData md = rs.getMetaData();
				for (int i = 1; i <= md.getColumnCount(); i++) {
					String cn = md.getColumnName(i);
					item.put(cn, rs.getString(i));
				}
				r.add(item);
			}

			return r;

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static void main(String[] args) throws InterruptedException, SQLException, UnsupportedEncodingException {

		// dirver路径设置
		System.setProperty("webdriver.chrome.driver", driverPath);

		conn = DriverManager.getConnection(dbUrl, dbUser, dbPw);

		List<Map<String, String>> list = getContactUrl();

		List<Map<String, String>> listPart = new LinkedList<Map<String, String>>();

		List<Thread> listThread = new ArrayList<Thread>();

		int total = 0;

		// 多线程干起
		for (Map<String, String> i : list) {
			listPart.add(i);
			total++;

			if (listPart.size() == 1500 || total == list.size()) {
				Thread f = new Thread(new GdContact(listPart));
				listThread.add(f);
				listPart = null;
				listPart = new LinkedList<Map<String, String>>();

				Thread.sleep(3000);

				f.start();

			}
		}

		// 目录数采集，请勿多线程采集，一个任务是采全部的

		/*
		 * Thread f = new Thread(new GdPart()); f.start();
		 */

	}

}
