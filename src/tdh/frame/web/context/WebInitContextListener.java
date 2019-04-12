package tdh.frame.web.context;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;




import tdh.frame.Release;
import tdh.frame.cache.CacheManager;
import tdh.frame.common.JVMUtils;
import tdh.frame.common.UtilComm;
import tdh.frame.web.core.Console;
import tdh.web.WebContext;

public class WebInitContextListener implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent sce) {
		
	}

	public void contextInitialized(ServletContextEvent sce) {
		
		JVMUtils.getJVMINFO();
        WebAppContext.bindServletContext(sce.getServletContext());
		//开始加载缓存数据
//		if(CacheManager.getInstance().startup()){
//			Console.print("服务器：缓存对象加载完成...");
//		}
		//数据解析相关
		try{
			String dbType = getDbType();
			Console.print("dbType:" + dbType);
			WebContext.setDbType(dbType);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	

	private String getDbType() throws Exception {

		String configFile = WebAppContext.getServletContextEx()
				.getRealPath("/") + "/WEB-INF/config/jdbc.properties";
		Properties p = new Properties();
		String driver = "", dbtype = "sybase";
		try {
			InputStream in = new FileInputStream(configFile);
			p.load(in);
			driver = UtilComm.trim(p.getProperty("export.url"));
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (driver.toLowerCase().startsWith("jdbc:oracle:"))
			dbtype = "oracle";
		if (driver.toLowerCase().startsWith("jdbc:db2:"))
			dbtype = "db2";
		if (driver.toLowerCase().startsWith("jdbc:mysql:"))
			dbtype = "mysql";
		return dbtype;
	}
}
