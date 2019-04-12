package tdh.xsyj.executeSbtj;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tdh.bean.Fy;
import tdh.db.DBHelper;
import tdh.frame.common.UUID;
import tdh.util.CalendarUtil;
import tdh.util.ClassLoaderUtil;
import tdh.util.CommUtil;
import tdh.util.xml.XMLDocument;
import tdh.util.xml.XMLNode;
import tdh.web.WebContext;

public class CourtThreadSbtj implements Callable<Map<String,String>> {
	private static Log logger = LogFactory.getLog(CourtThreadSbtj.class);
	private Fy fy;
	private String dateTime;
	private static List<String> ajlxbsList = null;// 国家赔偿中的行政赔偿案件
	private static List<String> ajlxList = null;// 国家赔偿案件中司法赔偿案件
	static {
		ajlxbsList = Arrays.asList("0501,0502,0503,0504,0505,0506,0507,0508".split(","));
		ajlxList = Arrays.asList("0509,0510,0511,0512,0513,0514,0515".split(","));
	}

	public CourtThreadSbtj(Fy fy, String dateTime) {
		super();
		this.fy = fy;
		this.dateTime = dateTime;
	}

	@Override
	public Map<String, String> call() throws Exception {
		Thread.currentThread().setName("tdh-" + fy.getFydm());
		logger.info("[" + Thread.currentThread().getName() + "] 开始生成有效数据..");
		String rq = CalendarUtil.getGsSj(dateTime, "yyyyMMdd");
		Connection conn = WebContext.getSbEcourtConn(fy.getFydm());
		Map<String,String> map=new HashMap<String,String>();
		if (conn == null) {
			logger.error("[" + Thread.currentThread().getName() + "] 法院代码:" + fy.getFydm() + ",代码：" + fy.getDm()
			    + ",获取链接异常,.");
			return map;
		}
		while(true==initSbtj(conn,fy)){
			List<Map<String, String>> ajList = loadAjList(conn,fy);
			destorySbtj(conn, fy);
			if(null!=ajList && ajList.size()>0){
				insertAjList(conn,ajList,fy);
			}else{
				destSbtj(conn, fy);
				
			}
			String lastscsj = WebContext.LASTSCSJ;
			List<String> delList = loadDelList(conn,lastscsj);
			deleteDest(delList);
		}
		
		return map;
	}

	private void deleteDest(List<String> delList) {
		Connection destConn=WebContext.getTjEcourtConn();//目标库的conn
		Statement st=null;
		ResultSet rs = null;
		String t2 = "";
		InputStream in = null;
		try {
			//先获取数据库时间t2
			st = destConn.createStatement();
			rs = st.executeQuery("SELECT REPLACE(CHAR(current timestamp),'-','/') t2 FROM sysibm.dual");
			while(rs.next()){
				t2 = rs.getString("t2");
			}
			if(delList != null && delList.size()>0){
				for (String delAhdm : delList) {
					String delsql = "delete from SEND_9902_SSSJ where AJBS = '"+delAhdm+"'";
					st.execute(delsql);
				}
			}
			//处理完后回写t1
			//db2时间格式化成 sybase版本  2018/12/17/09.54.35.287000 ----->   2018/12/17/09:54:35
			t2 = t2.substring(0, 10)+" "+t2.substring(11,19).replace(".", ":");
			
			XMLDocument xml = new XMLDocument();
			String configFile = null;
			URL url = ClassLoaderUtil.getExtendResource("../config/CONF.xml");
			configFile = url.getPath();
			
			in = new FileInputStream(configFile);
			xml.loadFromInputStream(in);
			XMLNode root = xml.getRoot();
			root.getChildNode("LASTSCSJ").setText(t2);
			String xmlnr = xml.asXML();
			File file = new File(configFile);
			FileUtils.writeStringToFile(file, xmlnr, "UTF-8");
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			DBHelper.closeStatement(st); 
			DBHelper.closeConn(destConn); 
		}
		
	}

	private List<String> loadDelList(Connection conn,String lastscsj) {
		StringBuffer delsql = new StringBuffer();
		List<String> list = new ArrayList<String>();
		List<Map<String, Object>> eajList = queryForList(conn,"SELECT A.AHDM FLTAHDM  FROM TS_DELAJ A,EAJ B WHERE  A.AHDM = B.AHDM");
		List<String> filterList = new ArrayList<String>();
		for (Map<String, Object> map : eajList) {
			filterList.add((String)map.get("FLTAHDM"));
		}
		delsql.append("SELECT AHDM FROM TS_DELAJ WHERE DT >= '");
		delsql.append(lastscsj);
		delsql.append("'");
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			pst = conn.prepareStatement(delsql.toString());
			rs = pst.executeQuery();
			while (rs.next()) {
				String ahdm = rs.getString("AHDM");
				if(!filterList.contains(ahdm)){
					list.add(ahdm);
				}
			}
			}catch(Exception e){
				logger.error("查询异常。。", e);
			}
		return list;
	}

	private List<Map<String, String>> loadAjList(Connection conn,Fy fy) {
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT ");
		sql.append(" A.AHDM,A.ND, A.AJLXDM AS AJLXBS,A.AH,A.SARQ,A.JARQ,A.LARQ,A.SAAY,(SELECT EAJ_SJQK.JAFSN FROM EAJ_SJQK WHERE EAJ_SJQK.AHDM =A.AHDM) AS JAFS ,");
		sql.append(" B.AJLYN  AS  AJLY,B.XZGLFW,(SELECT EAJ_SJQK.AY FROM EAJ_SJQK WHERE EAJ_SJQK.AHDM =A.AHDM) AS AY,");
		sql.append("(SELECT MIN(XZXWZL) FROM EDSR_XZXW WHERE EDSR_XZXW.AHDM =A.AHDM  ) AS XZXWZL  ");
		sql.append(" FROM EAJ A, EAJ_SALA B");
		sql.append(" WHERE A.AHDM = B.AHDM");
		sql.append(" AND A.FYDM =? AND A.AHDM IN (SELECT AHDM FROM AJ_SB_WFZ WHERE CJZT='0' and CQZT='1')");
		List<Map<String, String>> ajList = new ArrayList<Map<String, String>>();
		if (null != WebContext.FILTER && !"".equals(WebContext.FILTER)) {
			sql.append(" AND " + WebContext.FILTER);
		}
		PreparedStatement pst = null;
		ResultSet rs = null;
		int sqlCount = 0;
		int count = 0;
		try {
			pst = conn.prepareStatement(sql.toString());
			pst.setString(1, fy.getFydm());
			rs = pst.executeQuery();
			while (rs.next()) {
				sqlCount++;
				
				
				String ajlxbs = StringUtils.trim(WebContext.AjlxdmZh15Ajlxbs.get(rs.getString("AJLXBS")));
				if (CommUtil.isEmpty(ajlxbs)) {
					continue;
				}

				String ajly = CommUtil.trim(rs.getString("AJLY"));
				// 2018-14221 广东 (其他执行案件)AJLXDM=197 ,AJBM=1010 去除过滤
				if (!"197".equals(ajlxbs) && !"1010".equals(ajlxbs) 
						&&  CommUtil.isEmpty(getCodeValue(ajly, false))) {
					logger.error("[" + Thread.currentThread().getName() + "异常数据过滤：AHDM=>" + rs.getString("AHDM")
							+ ",AJLY=>" + getCodeValue(ajly, false) + ",AJLXBS= "+ajlxbs+", " + fy.getFydm());
					continue;
				}
				String jafs = CommUtil.trim(rs.getString("JAFS"));

				// 行政案件才判断
				String xzglfw = CommUtil.trim(rs.getString("XZGLFW"));
				String xzxwzl = CommUtil.trim(rs.getString("XZXWZL"));
				if (ajlxbs.startsWith("04") || ajlxbsList.contains(ajlxbs)) {
					// ===========
					if (CommUtil.isEmpty(CommUtil.trim(getCodeValue(xzglfw, false)))) {
						logger.error(
								"异常数据过滤：AHDM=>" + rs.getString("AHDM") + ",XZGLFW=>" + xzglfw + "," + fy.getFydm());
						continue;
					}
					if (CommUtil.isEmpty(CommUtil.trim(getCodeValue(StringUtils.trimToEmpty(xzxwzl), true)))) {
						logger.error("异常数据过滤：AHDM=>" + rs.getString("AHDM") + ",XZXWZL=>"
								+ StringUtils.trimToEmpty(xzxwzl) + "," + fy.getFydm());
						continue;
					}
				}

				String saay = CommUtil.trim(rs.getString("SAAY"));
				String source_laays = getCodeValue(saay, true);
				String[] arr_laays = source_laays.split("\n");
				StringBuffer laays = new StringBuffer();
				for (int i = 0; i < arr_laays.length; i++) {
					String temp = WebContext.getAyzh(arr_laays[i]);
					if (StringUtils.isNotBlank(temp)) {
						laays.append(temp).append("\n");
					}
				}

				// 立案案由判断
				if (ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxList.contains(ajlxbs)
						|| ajlxbs.startsWith("10")) {
					if (StringUtils.isEmpty(laays.toString())) {
						logger.error("[" + Thread.currentThread().getName() + "] 异常数据过滤：AHDM=>" + rs.getString("AHDM")
								+ ",SAAY=>" + rs.getString("SAAY") + "," + fy.getFydm());
						continue;
					}
				}

				String ay = rs.getString("AY");
				if ((null != rs.getString("JARQ") && !"".equals(rs.getString("JARQ")))
						&& (null != rs.getString("JAFS") && !"".equals(rs.getString("JAFS")))) {
					if (ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxList.contains(ajlxbs)
							|| ajlxbs.startsWith("10")) {
						if (StringUtils.isEmpty(ay)) {
							logger.error("[" + Thread.currentThread().getName() + "] 异常数据过滤：AHDM=>"
									+ rs.getString("AHDM") + ",AY=>" + rs.getString("AY") + "," + fy.getFydm());
							continue;
						}
					}
				}

				count++;
				String dm4 = CommUtil.long4Dm(fy.getDm());
				String nd = StringUtils.trimToEmpty(rs.getString("ND"));
				String sourceAhdm = StringUtils.trimToEmpty(rs.getString("AHDM"));
				Map<String, String> map = new HashMap<String, String>();
				map.put("source", sourceAhdm);
				String ahdm18 = ahdmConvertAhdm18(dm4, nd, ajlxbs, sourceAhdm);
				map.put("ajbs", ahdm18);
				map.put("ajlx", ajlxbs);
				map.put("ah", StringUtils.trimToEmpty(rs.getString("AH")));
				map.put("jbfy", dm4);
				if (CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("LARQ")))) {
					map.put("sarq", rs.getString("LARQ"));//倪子标 要求调整为LARQ
				}
				if (CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("JARQ")))
						) {//DJLARQ 语句是 CASE WHEN EAJ_SJQK.JAFSN IN('09_05080-7','09_05080-8') THEN '' ELSE EAJ.LARQ END
					if("09_05080-8".equals(jafs)){
						map.put("bydjlarq", rs.getString("JARQ"));
					}else if("09_05080-7".equals(jafs)){
						map.put("byslcdrq", rs.getString("JARQ"));
					}
				}
				if (CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("LARQ")))
						&& !"09_05080-7".equals(jafs) && !"09_05080-8".equals(jafs)) {
					map.put("djlarq", rs.getString("LARQ"));
				}

				if (StringUtils.isNotBlank(laays.toString())) {
					map.put("laay", laays.toString());
				}
				map.put("saly", getCodeValue(CommUtil.trim(rs.getString("AJLY")), false));
				if (CommUtil.isNotEmpty(getCodeValue(CommUtil.trim(rs.getString("XZGLFW")), false))) {
					map.put("laayztlx", getCodeValue(CommUtil.trim(rs.getString("XZGLFW")), false));
				}
				if (StringUtils.isNotBlank(xzxwzl)) {
					map.put("laayxzxwzl", getCodeValue(StringUtils.trimToEmpty(xzxwzl), true));
				}
				if (CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("JARQ")))) {
					map.put("jarq", rs.getString("JARQ"));
				}
				if (CommUtil.isNotEmpty(StringUtils.trimToEmpty(ay))) {
					map.put("jaay", getCodeValue(StringUtils.trimToEmpty(ay), true));
				}
				if (CommUtil.isNotEmpty(StringUtils.trimToEmpty(jafs))) {
					map.put("jafs", getCodeValue(jafs, false));
				}
				ajList.add(map);
			}
			logger.info("查询sql：" + sql.toString() + ",库中实际数量:" + sqlCount + ",实际xml中数量:" + count);
		} catch (Exception e) {
			logger.error("查询异常。。", e);
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(pst);
		}
		return ajList;
	}
	
	private void insertAjList(Connection sbconn,List<Map<String, String>> ajList,Fy fy) {
		Connection destConn=WebContext.getTjEcourtConn();//目标库的conn
		Statement st=null;
		//Date start = new Date();
		//String tjsj =CommUtil.foramtDate(start, "yyyy-MM-dd HH:mm:ss");
		int cnt=0;
		try {
			st = destConn.createStatement();
			for(Map<String,String> map:ajList){
				String id=StringUtils.trim(map.get("jbfy"));
				String ajbs = StringUtils.trim(map.get("ajbs"));
				String ajlx = StringUtils.trim(map.get("ajlx"));
				String ah = StringUtils.trim(map.get("ah"));
				String sarq = StringUtils.trim(CommUtil.convertRq8(map.get("sarq")));
				String djlarq = StringUtils.trim(CommUtil.convertRq8(map.get("djlarq")));
				String bydjlarq = map.get("bydjlarq");
				String byslcdrq = map.get("byslcdrq");
				if(null != bydjlarq && !"".equals(bydjlarq)){
					bydjlarq=StringUtils.trim(CommUtil.convertRq8(bydjlarq));
					byslcdrq=StringUtils.trim(CommUtil.convertRq8(byslcdrq));
				}
				String laay = StringUtils.trim(map.get("laay"));
				String saly = StringUtils.trim(map.get("saly"));
				String laayztlx = StringUtils.trimToEmpty(map.get("laayztlx"));
				String jarq = StringUtils.trim(CommUtil.convertRq8(map.get("jarq")));
				String jaay = StringUtils.trim(map.get("jaay"));
				String jafs = StringUtils.trim(map.get("jafs"));
				String laayxzxwzl = StringUtils.trimToEmpty(map.get("laayxzxwzl"));
				String jbfy=StringUtils.trim(map.get("jbfy"));
				String delSql = "delete from SEND_9902_SSSJ where AJBS='"+ajbs+"'";
				try {
					logger.debug(delSql);
					st.executeUpdate(delSql);
				} catch (SQLException e) {
					logger.error("执行删除sql异常："+delSql,e);
					continue;
				}
				//String insertsql="";
				StringBuffer insertSql = new StringBuffer();//ID, AJBS, AH, SARQ, BYDJLARQ, BYSLCDRQ, DJLARQ, LAAY, SALY, LAAY_ZTLX, LAAY_XZXWZL, JARQ, JAAY, JAFS, SJLY, MODI_DATE, AJLX, JBFY
				insertSql.append("INSERT INTO SEND_9902_SSSJ(ID, AJBS, AH, LAAY, SALY, LAAY_ZTLX, LAAY_XZXWZL, JAAY, JAFS, AJLX, JBFY, SJLY, MODI_DATE");
				StringBuffer insertParam =new StringBuffer();
				insertParam.append(" VALUES('").append(id).append("','").append(ajbs).append("','").append(ah).append("','").append(laay)
				.append("','").append(saly).append("','").append(laayztlx).append("','").append(laayxzxwzl).append("','").append(jaay)
				.append("','").append(jafs).append("','").append(ajlx).append("','").append(jbfy).append("','2',current timestamp");
				if(StringUtils.isNotBlank(sarq)){
					insertSql.append(",SARQ");
					insertParam.append(",'").append(sarq).append("'");
				}
				if(StringUtils.isNotBlank(bydjlarq)){
					insertSql.append(",BYDJLARQ");
					insertParam.append(",'").append(bydjlarq).append("'");
				}
				if(StringUtils.isNotBlank(byslcdrq)){
					insertSql.append(",BYSLCDRQ");
					insertParam.append(",'").append(byslcdrq).append("'");
				}
				if(StringUtils.isNotBlank(djlarq)){
					insertSql.append(",DJLARQ");
					insertParam.append(",'").append(djlarq).append("'");
				}
				if(StringUtils.isNotBlank(jarq)){
					insertSql.append(",JARQ");
					insertParam.append(",'").append(jarq).append("'");
				}
				insertSql.append(")").append(insertParam.append(")"));
				/*if((null!=jarq && !"".equals(jarq))//历史遗留问题。sarq 为空
						&& (null!=jafs && !"".equals(jafs))){
					if(null != bydjlarq && !"".equals(bydjlarq)){
						insertsql = "INSERT INTO SEND_9902_SSSJ(ID,AJBS,AH,SARQ,BYDJLARQ,BYSLCDRQ,LAAY,SALY,LAAY_ZTLX,LAAY_XZXWZL,JARQ,JAAY,JAFS,SJLY,AJLX,JBFY,MODI_DATE) "
								+ "VALUES('" + id +"','" + ajbs + "','" + ah + "','" + sarq + "','" + bydjlarq + "','"
								+ byslcdrq  + "','" + laay+ "','" + saly+ "','" + laayztlx + "','" + laayxzxwzl + "','"+jarq + "','"+jaay + "','"+jafs + "','2','" + ajlx + "','" + jbfy + "',current timestamp)";
					}else{
						insertsql = "INSERT INTO SEND_9902_SSSJ(ID,AJBS,AH,SARQ,DJLARQ,LAAY,SALY,LAAY_ZTLX,LAAY_XZXWZL,JARQ,JAAY,JAFS,SJLY,AJLX,JBFY,MODI_DATE) "
								+ "VALUES('" + id +"','" + ajbs + "','" + ah + "','" + sarq + "','" 
								 + djlarq + "','" + laay+ "','" + saly+ "','" + laayztlx + "','" + laayxzxwzl + "','"+jarq + "','"+jaay + "','"+jafs + "','2','" + ajlx + "','" + jbfy + "',current timestamp)";
					}
				}else{
					insertsql = "INSERT INTO SEND_9902_SSSJ(ID,AJBS,AH,SARQ,DJLARQ,LAAY,SALY,LAAY_ZTLX,LAAY_XZXWZL,SJLY,AJLX,MODI_DATE,JBFY) "
							+ "VALUES('" + id +"','" + ajbs + "','" + ah + "','" + sarq + "','" + djlarq + "','"
						     + laay+ "','" + saly+ "','" + laayztlx + "','" + laayxzxwzl + "','2','" + ajlx + "',current timestamp,'" + jbfy + "')";
				}*/
				try {
					logger.debug(insertSql);
					st.executeUpdate(insertSql.toString());
				} catch (SQLException e) {
					logger.error("执行插入语句异常:"+insertSql,e);
					catchAj(sbconn,map,1,e.getMessage());
					continue;
				}
				cnt++;
				catchAj(sbconn,map,0,"");
			}
			destoryAj(sbconn);
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally{
			DBHelper.closeStatement(st); 
			DBHelper.closeConn(destConn); 
		}
	}
	
	private void catchAj(Connection sbconn,Map<String,String> map,int zt,String msg){
		Statement st = null;
		String sql="";
		try {
			st=sbconn.createStatement();
			String id=UUID.genUuid().toString().replaceAll("-", "");
			Date date=new Date();
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if(0==zt){
				sql="INSERT INTO AJ_SB_WFZ_LOG(LSH,FYDM,AHDM,NEWAHDM,ZT,TJSJ) "
						+ "VALUES('" + id +"','" + fy.getFydm() + "','" + StringUtils.trim(map.get("source")) + "','" + StringUtils.trim(map.get("ajbs")) + "','0','" + sdf.format(date) + "')";
				st.executeUpdate(sql);
			}else{
				sql="update AJ_SB_WFZ set CJZT='2' where AHDM='"+StringUtils.trim(map.get("source"))+"' and FYDM='"+fy.getFydm()+"'";
				st.executeUpdate(sql);
				sql="INSERT INTO AJ_SB_WFZ_LOG(LSH,FYDM,AHDM,NEWAHDM,ZT,TJSJ,MSG) "
						+ "VALUES('" + id +"','" + fy.getFydm() + "','" + StringUtils.trim(map.get("source")) + "','" + StringUtils.trim(map.get("ajbs")) + "','1','" + sdf.format(date) + "','"+msg+"')";
				st.executeUpdate(sql);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeStatement(st);
		}
	}
	
	private void destoryAj(Connection sbconn){
		Statement st = null;
		String sql="";
		try {
			st=sbconn.createStatement();
			sql="delete from AJ_SB_WFZ where CJZT='1' and CQZT='1' and FYDM='"+fy.getFydm()+"'";
			st.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeStatement(st);
		}
	}

	private void destorySbtj(Connection conn, Fy fy) {
		Statement st = null;
		String sql = "";
		try {
			st = conn.createStatement();
			sql = "update top 1000 AJ_SB_WFZ set CJZT='1' where CJZT='0' and CQZT='1' and FYDM='" + fy.getFydm() + "'";
			st.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeStatement(st);
		}
	}
	
	private void destSbtj(Connection conn, Fy fy) {
		Statement st = null;
		String sql = "";
		try {
			st = conn.createStatement();
			sql = "delete from AJ_SB_WFZ where CJZT='1' and CQZT='1' and FYDM='" + fy.getFydm() + "'";
			st.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeStatement(st);
		}
	}
	
	private boolean initSbtj(Connection conn, Fy fy) {
		Statement st = null;
		ResultSet rs = null;
		try {
			int count = 0;
			st = conn.createStatement();
			rs = st.executeQuery("select count(*) from AJ_SB_WFZ where CJZT='0' and CQZT='0' and FYDM='" + fy.getFydm() + "'");
			if (rs.next()) {
				count = rs.getInt(1);
			}
			st.executeUpdate("update top 1000 AJ_SB_WFZ set CQZT='1' where CJZT='0' and FYDM='" + fy.getFydm() + "'");
			if (count > 0) {
				return true;
			} else {
				DBHelper.closeResultSet(rs);
				DBHelper.closeStatement(st);
				DBHelper.closeConn(conn); 
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
		}
		return false;
	}

	/**
	 * 处理 ,09_xxxx-y, 或者 ,15_xxxx-y, 或 xx_xxxx-y
	 * 
	 * @param code
	 *            代码值
	 * @param isfx
	 *            是否复选 复选时,如果 库内置为 xx_xxxx-a,xx_xxxx-b,xx_xxxx-c 则返回 a\nb\nc
	 * @return 返回y
	 */
	private String getCodeValue(String code, boolean isfx) {
		String newCode = code;
		StringBuffer code_cdx = new StringBuffer();
		if (StringUtils.isNotEmpty(code)) {
			if (code.indexOf(",") != -1) {
				String[] _temp = code.split(",");
				for (int i = 0; i < _temp.length; i++) {
					if (StringUtils.isNotEmpty(StringUtils.trim(_temp[i]))) {
						if (isfx) {
							code_cdx.append(getValidValue(_temp[i])).append("\n");
						} else {
							newCode = _temp[i];
							break;
						}
					}
				}
				if (isfx) {
					newCode = code_cdx.toString();
				}
			}
		}
		newCode = getValidValue(newCode);
		return StringUtils.isNotEmpty(newCode) ? newCode : StringUtils.EMPTY;
	}

	/**
	 * 针对 xx_xxxx-y 类型的值，取 y值
	 * 
	 * @param code
	 *            原值
	 * @return y值
	 */
	private String getValidValue(String code) {
		if (code.startsWith("09_") || code.startsWith("15_")) {
			String[] _temp = code.split("-");
			if (_temp.length == 2) {
				return _temp[1];
			}
		}
		return code;
	}

	private String ahdmConvertAhdm18(String dm, String nd, String ajbm, String ahdm) {
		// getDm()+nd+getAjbm()+getAhdm().substring(getAhdm().length()-6)
		// TS_FYMC.DM+EAJ.ND+TS_AJCXBM2015.AJLXBS+AHDM
		if (ahdm.length() > 18) {
			return ahdm.substring(1);
		} else if (ahdm.length() < 18) {
			return dm + nd + ajbm + ahdm.substring(ahdm.length() - 6);// 15转16
		} else {
			return ahdm;
		}
	}
	
	public List<Map<String,Object>> queryForList(Connection conn,String sql){
		List<Map<String,Object>> list =new ArrayList<Map<String,Object>>();
		
		ResultSet rs = null;
		PreparedStatement pst =null;
		try {
			pst  = conn.prepareStatement(sql);
			rs =  pst.executeQuery();
			ResultSetMetaData r  = rs.getMetaData();
			int columnNum =  r.getColumnCount();
			while(rs.next()){
				Map<String,Object> map =new HashMap<String,Object>();
				for (int i = 1; i <= columnNum; i++) {
					String columnName = r.getColumnLabel(i);
					map.put(columnName, rs.getObject(columnName));
				}
				list.add(map);
			}
		} catch (SQLException e) {
			logger.error("[SQL ERROR]查询List<Map>语句:"+sql,e);
		}finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(pst);
		}
		return list;
	}

}
