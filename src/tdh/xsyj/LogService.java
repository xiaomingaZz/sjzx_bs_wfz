package tdh.xsyj;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import tdh.frame.common.UUID;
import tdh.frame.web.context.WebAppContext;
import tdh.web.WebContext;

public class LogService {
	
	
	private static final String LOG_XSYJ = "LOG_XSYJ";
	
	private static final String LOG_ZX_XSYJ = "LOG_ZX_XSYJ";
	
	private static final String LOG_XSYJ_TEMP_LA = "LOG_XSYJ_TEMP_LA";
	
	private static final String LOG_XSYJ_TEMP_JA = "LOG_XSYJ_TEMP_JA";
	
	private static final String LOG_XSYJ_ERR = "LOG_XSYJ_ERR";
	
	private static final String LOG_ZX_XSYJ_ERR = "LOG_ZX_XSYJ_ERR";
	
	private static Log log = LogFactory.getLog(LogService.class);
	
	
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
	
	
	public static void insLaLog(String rq, String fygfdm, String ajbs, String ah, String ajlx, 
			String sarq, String larq, String laay, String qsbd, String ajzlx, 
			String tqxzpc){
		try{
			JdbcTemplate jdbc =(JdbcTemplate) WebAppContext.getBeanEx("ExportJdbcTemplate");
			int cnt = jdbc.queryForInt("SELECT COUNT(1) FROM " + LOG_XSYJ_TEMP_LA + " WHERE RQ = ? AND AJBS = ?",
					 new Object[]{rq, ajbs});
			if(cnt == 0){
				Object[] args = new Object[]{
					rq,
					ajbs,
					fygfdm,
					ah,
					ajlx,
					sarq,
					larq,
					laay,
					qsbd,
					ajzlx,
					tqxzpc
				};
				
				jdbc.update("INSERT INTO " + LOG_XSYJ_TEMP_LA
							+ " (RQ, AJBS, FYGFDM, AH, AJLX, SARQ,"
							+ " LARQ, LAAY, QSBD, AJZLX, TQXZPC)"
							+ " VALUES(?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?)",
							args);
			}
		}catch(Exception e){
			log.error("insLaLog", e);
			log.error(rq + "," + ajbs + "," + fygfdm + "," + ah + "," + ajlx + "," + sarq
					+ "," + larq + "," + laay + "," + qsbd + "," + ajzlx + "," + tqxzpc);
		}
		
	}
	
	public static void insJaLog(String rq, String fygfdm, String ajbs, String ah, String ajlx, 
			String jarq, String jaay, String jabd, String jafs, String larq, 
			String ajzlx, String tqxzpc){
		try{
			JdbcTemplate jdbc =(JdbcTemplate) WebAppContext.getBeanEx("ExportJdbcTemplate");
			int cnt = jdbc.queryForInt("SELECT COUNT(1) FROM " + LOG_XSYJ_TEMP_JA + " WHERE RQ = ? AND AJBS = ?",
					new Object[]{rq, ajbs});
			if(cnt == 0){
				Object[] args = new Object[]{
					rq,
					ajbs,
					fygfdm,
					ah,
					ajlx,
					jarq,
					jaay,
					jabd,
					jafs,
					larq,
					ajzlx,
					tqxzpc
				};
				
				jdbc.update("INSERT INTO " + LOG_XSYJ_TEMP_JA
							+ " (RQ, AJBS, FYGFDM, AH, AJLX, JARQ,"
							+ " JAAY, JABD, JAFS, LARQ, AJZLX,"
							+ "TQXZPC)"
							+ " VALUES(?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?)",
							args);
			}
		}catch(Exception e){
			log.error("insJaLog", e);
			log.error(rq + "," + ajbs + "," + fygfdm + "," + ah + "," + ajlx + "," + jarq
					+ "," + jaay + "," + jabd + "," + jafs + "," + larq + "," + ajzlx + "," + tqxzpc);
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
