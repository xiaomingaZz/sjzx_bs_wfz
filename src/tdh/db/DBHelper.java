package tdh.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tdh.util.ClassLoaderUtil;
import tdh.util.CommUtil;

public class DBHelper {
	
	private static Log log = LogFactory.getLog(DBHelper.class);
	
	public static void closeConn(Connection conn){
		if(conn!=null){
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void closeStatement(Statement st){
		if(st!=null){
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void closeResultSet(ResultSet rs){
		if(rs!=null){
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	 /**
	   * 获取数据库连接
	   * @param db 
	   * @return
	   * @author 仇国庆 
	   * @date 2014-5-27
	   */
	  public static Connection getConn(String db) {
	    Connection conn = null;
	    InputStream in = null;
	    try {

	      String configFile = null;
	      try {
	        URL url = ClassLoaderUtil.getExtendResource("../config/jdbc.properties");
	        configFile = url.getPath();
	      } catch (MalformedURLException e1) {
	    	log.info("[读取jdbc配置文件错误----->] "  + e1.getMessage());
	        e1.printStackTrace();
	      }
	      in = new FileInputStream(configFile);
	      Properties p = new Properties();
	      p.load(in);
	      String className = p.getProperty(db + ".driverClassName");
	      String url = p.getProperty(db + ".url");
	      String name = p.getProperty(db + ".username");
	      String pass = p.getProperty(db + ".password");
	      Class.forName(className);
	      conn = DriverManager.getConnection(url, name, pass);
	    } catch (Exception e) {
	      e.printStackTrace();
	    } finally {
	      CommUtil.closeStream(in);
	    }
	    return conn;
	  }
	  
	
	  
	  /**
	   * 获取数据库连接
	   * @param db 
	   * @return
	   * @author 仇国庆 
	   * @date 2014-5-27
	   */
	  public static Connection getConn(String driver,String url,String user,String password) {
	    Connection conn = null;
	    InputStream in = null;
	    try {
	      Class.forName(driver).newInstance();
	      conn = DriverManager.getConnection(url, user, password);
	    } catch (Exception e) {
	      e.printStackTrace();
	    } finally {
	      CommUtil.closeStream(in);
	    }
	    return conn;
	  }
}
