package com.yangshan.sync.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class MysqlUtil {
	public static ComboPooledDataSource dataSource; 
	static{
		
		dataSource = new ComboPooledDataSource();
		try {
			
			Properties p = new Properties();
			p.load(MysqlUtil.class.getClassLoader().getResourceAsStream("mysqljdbc.properties"));
			
			dataSource.setDriverClass((String) p.get("jdbcdriver"));
			dataSource.setJdbcUrl((String) p.get("url"));
			dataSource.setUser((String) p.get("username"));
			dataSource.setPassword((String) p.get("password"));
			dataSource.setMaxPoolSize(Integer.parseInt((String)p.get("maxpoolsize")));
			dataSource.setInitialPoolSize(Integer.parseInt((String)p.get("initpoolsize")));
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	public static Connection getConnection() {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
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
