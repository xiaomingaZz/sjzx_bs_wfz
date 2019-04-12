package tdh.xsyj;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import tdh.frame.common.UUID;
import tdh.frame.web.context.WebAppContext;
import tdh.framework.util.StringUtils;
import tdh.web.WebContext;

public class LogService15 {
	
	
	private static final String LOG_XSYJ = "LOG_XSYJ";
	
	private static final String LOG_ZX_XSYJ = "LOG_ZX_XSYJ";
	
	private static final String LOG_XSYJ_TEMP_LA_15 = "LOG_XSYJ_TEMP_LA_15";
	
	private static final String LOG_XSYJ_TEMP_JA_15 = "LOG_XSYJ_TEMP_JA_15";
	
	private static final String LOG_XSYJ_ERR = "LOG_XSYJ_ERR";
	
	private static final String LOG_ZX_XSYJ_ERR = "LOG_ZX_XSYJ_ERR";
	
	private static Log log = LogFactory.getLog(LogService15.class);
	
	
	public static void insetLog(String fydm, String dateTime, int las,int jas){
		JdbcTemplate jdbc =(JdbcTemplate) WebAppContext.getBeanEx("ExportJdbcTemplate");
		Object[] args = new Object[]{
				fydm,
				dateTime.replace("-","").replace(" ","").replace(":",""),
				las,
				jas
		};
		jdbc.update("INSERT INTO " + LOG_XSYJ + " (FYDM,DT,LAS,JAS) VALUES(?,?,?,?)",
				args);
	}
	
	public static void insetZxLog(String fydm, String dateTime, int las,int jas){
		JdbcTemplate jdbc =(JdbcTemplate) WebAppContext.getBeanEx("ExportJdbcTemplate");
		Object[] args = new Object[]{
				fydm,
				dateTime.replace("-","").replace(" ","").replace(":",""),
				las,
				jas
		};
		jdbc.update("INSERT INTO " + LOG_ZX_XSYJ + " (FYDM,DT,LAS,JAS) VALUES(?,?,?,?)",
				args);
	}
	
	
	public static void insLaLog(String sbrq,String jbfy,String ajlx,String ajbs,String ah,String sarq,
			String bydjlarq,String byslcdrq,String djlarq,String laay,String saly,String laayztlx,String laayxzxwzl,String fydm){
		try{
			JdbcTemplate jdbc =(JdbcTemplate) WebAppContext.getBeanEx("ExportJdbcTemplate");
			int cnt = jdbc.queryForInt("SELECT COUNT(1) FROM " + LOG_XSYJ_TEMP_LA_15 + " WHERE SBRQ = ? AND AJBS = ?",
					 new Object[]{sbrq, ajbs});
			
			if(cnt == 0){
				Object[] args = new Object[]{
					sbrq,
					jbfy,
					ajlx,
					ajbs,
					ah,
					StringUtils.trim(sarq),
					StringUtils.trim(bydjlarq),
					StringUtils.trim(byslcdrq),
					djlarq,
					StringUtils.trim(laay),
					StringUtils.trim(saly),
					StringUtils.trim(laayztlx),
					StringUtils.trim(laayxzxwzl),
					fydm
				};
				
//				jdbc.update("INSERT INTO " + LOG_XSYJ_TEMP_LA_15
//							+ " (SBRQ,JBFY,AJLX,AJBS,AH,SARQ,BYDJLARQ,BYSLCDRQ,DJLARQ,LAAY,SALY,LAAYZTLX,LAAYXZXWZL,FYDM)"
//							+ " VALUES('"+sbrq+"', '"+jbfy+"', '"+ajlx+"', '"+ajbs+"', '"
//							+ah+"', '"+StringUtils.trim(sarq)+"','"+StringUtils.trim(bydjlarq)+"', '"+StringUtils.trim(byslcdrq)+"', '"
//							+djlarq+"','"+StringUtils.trim(laay)+"','"+StringUtils.trim(saly)+"','"+StringUtils.trim(laayztlx)+"','"+StringUtils.trim(laayxzxwzl)+"','"+fydm+"')");
				
				jdbc.update("INSERT INTO " + LOG_XSYJ_TEMP_LA_15
						+ " (SBRQ,JBFY,AJLX,AJBS,AH,SARQ,BYDJLARQ,BYSLCDRQ,DJLARQ,LAAY,SALY,LAAYZTLX,LAAYXZXWZL,FYDM)"
						+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",args);
			}
		}catch(Exception e){
			log.error("insLaLog", e);
			log.error(sbrq + "," + jbfy + "," + ajlx + "," + ajbs + "," + ah + "," + sarq
					+ "," + djlarq + "," + laay + "," + fydm);
		}
		
	}
	
	
	public static void insJaLog(String sbrq,String jbfy,String ajlx,String ajbs,String ah,String sarq,
			String bydjlarq,String byslcdrq,String djlarq,String laay,String saly,String laayztlx,String laayxzxwzl,String jarq,String jaay,String fydm,String jafs){
		try{
			JdbcTemplate jdbc =(JdbcTemplate) WebAppContext.getBeanEx("ExportJdbcTemplate");
			int cnt = jdbc.queryForInt("SELECT COUNT(1) FROM " + LOG_XSYJ_TEMP_JA_15 + " WHERE SBRQ = ? AND AJBS = ?",
					new Object[]{sbrq, ajbs});
			if(cnt == 0){
				Object[] args = new Object[]{
					sbrq,
					jbfy,
					ajlx,
					ajbs,
					ah,
					StringUtils.trim(sarq),
					StringUtils.trim(bydjlarq),
					StringUtils.trim(byslcdrq),
					djlarq,
					StringUtils.trim(laay),
					StringUtils.trim(saly),
					StringUtils.trim(laayztlx),
					StringUtils.trim(laayxzxwzl),
					jarq,
					StringUtils.trim(jaay),
					fydm,
					StringUtils.trim(jafs)
				};
				
//				jdbc.update("INSERT INTO " + LOG_XSYJ_TEMP_JA_15
//							+ " (SBRQ,JBFY,AJLX,AJBS,AH,SARQ,BYDJLARQ,BYSLCDRQ,DJLARQ,LAAY,SALY,LAAYZTLX,LAAYXZXWZL,JARQ,JAAY,FYDM,JAFS)"
//							+ " VALUES('"+sbrq+"',"+"'"+jbfy+"',"+"'"+ajlx+"',"+"'"+ajbs+"',"+"'"+ah+"',"+"'"+sarq+"',"
//							+"'"+bydjlarq+"',"+"'"+byslcdrq+"',"+"'"+djlarq+"',"+"'"+laay+"',"+"'"+saly+"',"+"'"+laayztlx+"',"+"'"+laayxzxwzl+"',"+"'"+jarq+"',"+"'"+jaay+"',"+"'"+fydm+"',"+"'"+jafs+"')");
				
				jdbc.update("INSERT INTO " + LOG_XSYJ_TEMP_JA_15
						+ " (SBRQ,JBFY,AJLX,AJBS,AH,SARQ,BYDJLARQ,BYSLCDRQ,DJLARQ,LAAY,SALY,LAAYZTLX,LAAYXZXWZL,JARQ,JAAY,FYDM,JAFS)"
						+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",args);
			}
		}catch(Exception e){
			log.error("insJaLog", e);
			log.error(sbrq + "," + jbfy + "," + ajlx + "," + ajbs + "," + ah + "," + jarq
					+ "," + jaay + "," + djlarq + "," + fydm + "," + jafs);
		}
		
	}
	
//	public static void tauncateTempTable(){
//		String day = new SimpleDateFormat("yyyyMMdd").format(new Date());
//		if(day.equals(currentDay)){
//			
//		}else{
//			currentDay = day;
//			JdbcTemplate jdbc =(JdbcTemplate) WebAppContext.getBeanEx("ExportJdbcTemplate");
//			jdbc.execute("TRUNCATE TABLE " + LOG_XSYJ_TEMP_LA);
//			jdbc.execute("TRUNCATE TABLE " + LOG_XSYJ_TEMP_JA);
//		}
//	}
	
	/**
	 * 
	 * @param fydm
	 * @param lx 1:数据库异常
	 * @param errMsg
	 */
	public static void insLogErr(String fydm, String lx, String errMsg){
		JdbcTemplate jdbc =(JdbcTemplate) WebAppContext.getBeanEx("ExportJdbcTemplate");
		String uuid = UUID.genUuid();
		Object[] args = new Object[]{
				uuid,
				fydm,
				lx,
				errMsg	
		};
		if("oracle".equals(WebContext.getDbType())){
			jdbc.update("INSERT INTO " + LOG_XSYJ_ERR
					+ " (UUID, FYDM, LX, ERRMSG, LASTUPDATE)"
					+ " VALUES(?, ?, ?, ?, sysdate)",
					args);
		}else{
			jdbc.update("INSERT INTO " + LOG_XSYJ_ERR
					+ " (UUID, FYDM, LX, ERRMSG, LASTUPDATE)"
					+ " VALUES(?, ?, ?, ?, getdate())",
					args);
		}
		
	}
	
	public static void insLogZxErr(String fydm, String lx, String errMsg){
		JdbcTemplate jdbc =(JdbcTemplate) WebAppContext.getBeanEx("ExportJdbcTemplate");
		String uuid = UUID.genUuid();
		Object[] args = new Object[]{
				uuid,
				fydm,
				lx,
				errMsg	
		};
		if("oracle".equals(WebContext.getDbType())){
			jdbc.update("INSERT INTO " + LOG_ZX_XSYJ_ERR
					+ " (UUID, FYDM, LX, ERRMSG, LASTUPDATE)"
					+ " VALUES(?, ?, ?, ?, sysdate)",
					args);
		}else{
			jdbc.update("INSERT INTO " + LOG_ZX_XSYJ_ERR
					+ " (UUID, FYDM, LX, ERRMSG, LASTUPDATE)"
					+ " VALUES(?, ?, ?, ?, getdate())",
					args);
		}
		
	}

}
