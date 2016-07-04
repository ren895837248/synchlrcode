package com.yangshan.sync.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import oracle.jdbc.driver.OracleDriver;

import org.apache.commons.dbcp2.BasicDataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class OraclelUtil {
	public static BasicDataSource dataSource; 
	static{
		
		dataSource = new BasicDataSource();
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			
			/*Properties p = new Properties();
			p.load(OraclelUtil.class.getClassLoader().getResourceAsStream("oraclejdbc.properties"));
			dataSource.setDriverClassName((String) p.get("jdbcdriver"));
			dataSource.setUrl((String) p.get("url"));
			dataSource.setUsername((String) p.get("username"));
			dataSource.setPassword((String) p.get("password"));
			dataSource.setMaxTotal(Integer.parseInt((String)p.get("maxpoolsize")));
			dataSource.setInitialSize(Integer.parseInt((String)p.get("initpoolsize")));*/
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	public static Connection getConnection() {
		try {
			Properties p = new Properties();
			p.load(OraclelUtil.class.getClassLoader().getResourceAsStream("oraclejdbc.properties"));
			return DriverManager.getConnection((String) p.get("url"), (String) p.get("username"), (String) p.get("password"));
			/*return dataSource.getConnection();*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void closeConnection(Connection conn){
		if(conn!=null){
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
}
