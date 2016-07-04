package com.yangshan.sync.domain;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Calendar;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.yangshan.sync.util.MysqlUtil;
import com.yangshan.sync.util.OraclelUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;



public class SyncHlrCode {

	Logger logger = Logger.getLogger(SyncHlrCode.class);

	private void start(String interval) {
		Calendar currentTime = Calendar.getInstance();
		int currtHour = currentTime.get(Calendar.HOUR_OF_DAY);
		int initDelay = Math.abs((currtHour - 7));
		if (StringUtils.isNotBlank(interval)) {
			logger.debug("同步hlr开始执行，同步间隔：" + interval + "小时");
			ScheduledExecutorService ses = (ScheduledExecutorService) Executors
					.newSingleThreadExecutor();
			ses.scheduleWithFixedDelay(new SyncHandle(), initDelay, 12,
					TimeUnit.HOURS);

		} else {
			/*logger.debug("同步hlr开始执行，同步间隔：" + interval + "小时");
			ScheduledExecutorService ses = (ScheduledExecutorService) Executors
					.newSingleThreadExecutor();
			ses.scheduleWithFixedDelay(new SyncHandle(), initDelay, 12,
					TimeUnit.HOURS);
*/
			
			 logger.debug("同步hlr开始执行，单次同步"); Thread syncTh = new Thread(new
			 SyncHandle()); 
			 syncTh.start();
			 
		}
	}

	private class SyncHandle implements Runnable {

		public void run() {
			logger.debug("获取hlr源数据...");
			Map<String, String> sourceMap = getHlrDataSource();
			logger.debug("获取hlr目标数据...");
			Map<String, String> destMap = getHlrDataDest();
			logger.debug("对比源数据和目标数据...");
			boolean syncFlag = comparaData(sourceMap, destMap);

			if (syncFlag) {
				logger.debug("源数据和目标数据有差异，需要进行缓存刷新同步...");
				logger.debug("修改目标数据...");
				updateDestHlrData();
				logger.debug("同步服务缓存...");
				syncHlr();// 同步缓存
			} else {
				logger.debug("源数据和目标数据相同，不需要进行缓存刷新同步...");
			}

		}

		/**
		 * 获取源数据
		 * 
		 * @return
		 */
		private Map<String, String> getHlrDataSource() {
			Map<String, String> sourceMap = new HashMap<String, String>();

			String sql = "select a.serialnumber_s startnum,a.serialnumber_e endnum,a.switch_id hlr from td_m_moffice a";

			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				conn = OraclelUtil.getConnection();
				ps = conn.prepareStatement(sql);
				rs = ps.executeQuery();
				while (rs.next()) {
					sourceMap.put(rs.getString(1), rs.getString(3));
					sourceMap.put(rs.getString(2), rs.getString(3));
				}

			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				OraclelUtil.closeConnection(conn);
			}
			return sourceMap;
		}

		/**
		 * 获取目标数据
		 * 
		 * @return
		 */
		private Map<String, String> getHlrDataDest() {
			Map<String, String> descMap = new HashMap<String, String>();

			String sql = "select a.serialnumber_s startnum,a.serialnumber_e endnum,a.switch_id hlr from td_m_moffice a limit 10000;";

			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				conn = MysqlUtil.getConnection();
				ps = conn.prepareStatement(sql);
				rs = ps.executeQuery();
				while (rs.next()) {
					descMap.put(rs.getString(1), rs.getString(3));
					descMap.put(rs.getString(2), rs.getString(3));
				}

			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				MysqlUtil.closeConnection(conn);
			}
			return descMap;
		}

        /**
         *
         * @param sourceMap
         * @param descMap
         * @return
         */
		private boolean comparaData(Map<String, String> sourceMap,
				Map<String, String> descMap) {
			boolean returnFlag = false;
			
			if(sourceMap.size()!=descMap.size()){
				return true;
				
			}
			Set<Entry<String, String>> sourceEntrySet = sourceMap.entrySet();

			for (Entry<String, String> sourceData : sourceEntrySet) {
				String descValue = descMap.get(sourceData.getKey());
				if (!sourceData.getValue().equals(descValue)) {
					returnFlag = true;
					break;
				}
			}
			return returnFlag;
		}

		/**
		 * 修改目标数据
		 * 
		 * @return
		 */
		private Map<String, String> updateDestHlrData() {

			String sql = "select a.serialnumber_s startnum,a.serialnumber_e endnum,a.switch_id hlr from td_m_moffice a";

			Connection connOra = null;
			PreparedStatement psOra = null;
			ResultSet rsOra = null;
			
			Connection connMysql= null;
			PreparedStatement psMysql = null;

			try {
				logger.debug("删除mysql数据库td_m_moffice表..");
				connMysql = MysqlUtil.getConnection();
				connMysql.setAutoCommit(false);
				psMysql = connMysql.prepareStatement("delete from td_m_moffice");
				psMysql.executeUpdate();


				connOra = OraclelUtil.getConnection();
				psOra = connOra.prepareStatement(sql);
				rsOra = psOra.executeQuery();


				String inserSql = "insert into td_m_moffice values(?,?,?)";
				psMysql = connMysql.prepareStatement(inserSql);

				int i = 0;
				while (rsOra.next()) {
					psMysql.setString(1, rsOra.getString(1));
					psMysql.setString(2, rsOra.getString(2));
					psMysql.setString(3, rsOra.getString(3));
					psMysql.addBatch();
					i++;
					if (i % 100 == 0) {
						psMysql.executeBatch();
						psMysql.clearBatch();
					}
				}

				psMysql.executeBatch();
				connMysql.commit();
				logger.debug("批量插入完成...");
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				OraclelUtil.closeConnection(connOra);
				MysqlUtil.closeConnection(connMysql);
			}

			return null;
		}

		private void syncHlr() {

			Properties p = new Properties();
			try {
				InputStream in = this.getClass().getClassLoader().getResourceAsStream("syncIpAndPort.properties");
				p.load(in);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			String ips = (String) p.get("ips");
			String ports = (String) p.get("port");
			logger.debug("需要同步的服务ip：" + ips);
			if (StringUtils.isNotBlank(ips)) {
				String[] ipArr = ips.split(",");
				String[] portArr = ports.split(",");
				for (int i = 0; i < ipArr.length; i++) {
					for (int portcount = 0; portcount < portArr.length; portcount++) {
						String ip = ipArr[i];
						String port = portArr[portcount];
						logger.debug("开始对服务" + ip + ":" + port + "进行同步...");
						StringBuffer urlStr = new StringBuffer();
						urlStr.append("http://");
						urlStr.append(ip);
						urlStr.append(":");
						urlStr.append(port);
						urlStr.append("/nas/nas/exception/exceptionmsg/dummy/reFreshHlrCache");
						try {
							URL url = new URL(urlStr.toString());
							HttpURLConnection conn =  (HttpURLConnection) url.openConnection();
							conn.setReadTimeout(20000);
							conn.setRequestMethod("GET");
							conn.connect();
							InputStream in = conn.getInputStream();

							byte[] readb = new byte[1024];
							int len = 0;
							StringBuffer returnStr = new StringBuffer();
							while ((len = in.read(readb)) != -1) {
								returnStr.append(new String(readb, 0, len));
							}
							logger.debug("服务["+ip+":"+port+"]同步结果：" + ("success".equals(returnStr.toString())?"成功":"失败"));
						} catch (Exception e) {
							logger.error("服务["+ip+":"+port+"]同步结果：失败");
							e.printStackTrace();
						}
					}
				}
			}

		}

	}

	public static void main(String[] args) {
		String interval = "";

		if (args.length > 0) {
			interval = args[0];
		}
		SyncHlrCode sync = new SyncHlrCode();
		sync.start(interval);
	}

}
