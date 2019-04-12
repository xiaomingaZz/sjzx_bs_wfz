package tdh.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;






import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import tdh.frame.common.DBUtils;
import tdh.bean.Fy;
import tdh.util.CommUtil;
import tdh.web.WebContext;

public class CommDAO {
	
	private static Log log = LogFactory.getLog(DBHelper.class);
	
	public JdbcTemplate courtJdbc;
	
	public JdbcTemplate exportJdbc;
	
	public void setCourtJdbc(JdbcTemplate courtJdbc){
		this.courtJdbc = courtJdbc;
	}
	
	public JdbcTemplate getCourtJdbc(){
		return courtJdbc;
	}
	
	public void setExportJdbc(JdbcTemplate exportJdbc){
		this.exportJdbc = exportJdbc;
	}
	
	public  JdbcTemplate getExportJdbc(){
		return exportJdbc;
	}


	/**
	 * 加载法院信息的代码cache
	 * @param conn
	 * @param map
	 * @throws Exception
	 */
	public static void loadFyDataMap(Connection conn,Map<String,Fy> map) throws Exception{
		Statement st = null;
		ResultSet rs = null;
		try{
			st = conn.createStatement();
			rs = st.executeQuery("select FYDM,FJM,DM,FYMC from V_ZH_FY where FYDM like '"+WebContext.FyCode+"%' order by FJM ");
			while(rs.next()){
				Fy bean = new Fy();
				bean.setFydm(rs.getString("FYDM"));
				bean.setFjm(rs.getString("FJM"));
				bean.setDm(rs.getString("DM"));
				bean.setFymc(rs.getString("FYMC"));
				map.put(bean.getFydm(), bean);
			}
		}catch(Exception e){
			throw e;
		}finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
		}
	}
	
	/**
	 * 加载法院信息的代码cache
	 * @param conn
	 * @param map
	 * @throws Exception
	 */
	public static void loadFyDataMap(Connection conn,Map<String,Fy> map, String fydm) throws Exception{
		Statement st = null;
		ResultSet rs = null;
		try{
			st = conn.createStatement();
			rs = st.executeQuery("select FYDM,FJM,DM,FYMC from V_ZH_FY where FYDM like '"+fydm+"%' order by FJM ");
			while(rs.next()){
				Fy bean = new Fy();
				bean.setFydm(rs.getString("FYDM"));
				bean.setFjm(rs.getString("FJM"));
				bean.setDm(rs.getString("DM"));
				bean.setFymc(rs.getString("FYMC"));
				map.put(bean.getFydm(), bean);
			}
		}catch(Exception e){
			throw e;
		}finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
		}
	}

}
