package tdh.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import tdh.bean.BzDm;
import tdh.bean.Fy;
import tdh.db.DBHelper;
import tdh.frame.common.DBUtils;
import tdh.frame.common.UtilComm;
import tdh.frame.web.context.WebAppContext;
import tdh.framework.util.StringUtils;
import tdh.util.ClassLoaderUtil;
import tdh.util.CommUtil;
import tdh.util.xml.XMLDocument;
import tdh.util.xml.XMLNode;
import tdh.xsyj.AjlxConst;

public class WebContextInitListener implements ServletContextListener {

	private static Log log = LogFactory.getLog(WebContextInitListener.class);

	public void contextInitialized(ServletContextEvent event) {
		XMLDocument xml = new XMLDocument();
		String configFile = null;
		try {
			URL url = ClassLoaderUtil.getExtendResource("../config/CONF.xml");
			configFile = url.getPath();
		} catch (Exception e1) {
			log.info("[读取config配置文件错误----->] " + e1.getMessage());
			e1.printStackTrace();
		}

		log.info("[加载配置文件----->] " + configFile);

		String sbajlxdms = "";
		InputStream in = null;
		try {
			in = new FileInputStream(configFile);
			xml.loadFromInputStream(in);
			XMLNode root = xml.getRoot();
			WebContext.FyCode = root.getChildNode("FYDM").getNodeValue();
			WebContext.JKURL = root.getChildNode("JKURL").getNodeValue();
			WebContext.OLDURL = root.getChildNode("OLDURL").getNodeValue();
			WebContext.DIR = root.getChildNode("DIR").getNodeValue();
			WebContext.TYPE = root.getChildNode("TYPE").getNodeValue();
			WebContext.LASTSCSJ = root.getChildNode("LASTSCSJ").getNodeValue();
			WebContext.FILTER = UtilComm.trim(root.getChildNode("FILTER").getNodeValue());
			WebContext.NAMESPACE = UtilComm.trim(root.getChildNode("NAMESPACE").getNodeValue());
			WebContext.METHOD = UtilComm.trim(root.getChildNode("METHOD").getNodeValue());
			WebContext.PARAMETER = UtilComm.trim(root.getChildNode("PARAMETER").getNodeValue());
//			WebContext.LASTUPDATEFILTER = UtilComm.trim(root.getChildNode("LASTUPDATEFILTER").getNodeValue());
			String ahdm_flag = UtilComm.trim(root.getChildNode("FLAG_AHDM").getNodeValue());
			if ("false".equals(ahdm_flag)) {
				WebContext.AHDM_FLAG = false;
			} else {
				WebContext.AHDM_FLAG = true;
			}
			WebContext.FILTER_FYDM = UtilComm.trim(root.getChildNode("FILTER_FYDM").getNodeValue());
			sbajlxdms = root.getChildNode("SBAJLXDM").getNodeValue();
			XMLNode node = root.getChildNode("VERSION");
			if(node!=null){
				WebContext.VERSION = node.getNodeValue();
				if(WebContext.VERSION.equals("09Z15")){
					System.out.println(("09转15模式"));
				}
				if(WebContext.VERSION.equals("ZX")){
					WebContext.VERSION = "09Z15";
					System.out.println("ZX模式");
				}
				if(!WebContext.VERSION.equals("09") && !WebContext.VERSION.equals("09Z15") && !WebContext.VERSION.equals("15")
						&& !WebContext.VERSION.equals("HB") && !WebContext.VERSION.equals("HB09Z15")&& !WebContext.VERSION.equals("SBTJ")&& !WebContext.VERSION.equals("FJMS")
						){
					WebContext.VERSION = "09";
					log.warn("CONF.xml[VERSION] 配置错误.系统默认 设置为 09");
				}
			}
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
		}

		for (String sbajlxdm : sbajlxdms.split(",")) {
			if (UtilComm.isEmpty(sbajlxdm))
				continue;
			if (AjlxConst.DM_AJLX_MAP.containsKey(sbajlxdm)) {
				WebContext.SBAJLXDMLIST.add(sbajlxdm);
			} else {
				log.error("无效的案件类型代码:" + sbajlxdm);
			}
		}

		log.info("上报地区省法院代码:" + WebContext.FyCode);
		log.info("上报案件类型代码:" + sbajlxdms);
		log.info("上报案件类型代码种类有效:" + WebContext.SBAJLXDMLIST.size());
		log.info("上报服务接口地址（新）:" + WebContext.JKURL);
		log.info("上报接口命名空间:"+WebContext.NAMESPACE);
		log.info("上报服务接口地址（旧）:" + WebContext.OLDURL);
		log.info("上报临时文件存储地址:" + WebContext.DIR);
		log.info("上报过滤案件条件:" + WebContext.FILTER);
		log.info("上报是否启用案件标识转换15位:" + WebContext.AHDM_FLAG);
		log.info("上报排除上报法院:" + WebContext.FILTER_FYDM);
		log.info("上报数据类型(SP/ZX):" + WebContext.TYPE);
		log.info("上报接口版本:"+WebContext.VERSION);
		log.info("上报程序更新时间：20180703");
		load();
		loadFyConn();
		loadSbConn();
		loadAjConn();
		loadTjConn();
		loadDmzhAjlx();
		loadAjLxZh();
		if(WebContext.VERSION.equals("09Z15")||WebContext.VERSION.equals("HB")||WebContext.VERSION.equals("15")||WebContext.VERSION.equals("HB09Z15")||WebContext.VERSION.equals("SBTJ")||WebContext.VERSION.equals("FJMS")){
			loadAjlxdmZh15();
			loadTsAy();
			load15Xsd();
		}
		if(WebContext.VERSION.equals("09Z15") || WebContext.VERSION.equals("15")){
			loadTsbzdm15();
		}
	}

	public void contextDestroyed(ServletContextEvent event) {
		WebContext.fyMap.clear();
		WebContext.bzdmMap.clear();
		WebContext.ajlxdmzhMap.clear();
		WebContext.AjlxdmZh15Ajlxbs.clear();
	}
	
	public static void load() {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			conn = DBHelper.getConn("export");
			st = conn.createStatement();
			rs = st.executeQuery("select * from TS_BZDM");
			WebContext.bzdmMap.clear();
			while (rs.next()) {
				String v2014code = CommUtil.trim(rs.getString("V2014CODE"));
				BzDm bean = new BzDm();
				bean.setDm(rs.getString("CODE"));
				bean.setV2014dm(v2014code);
				WebContext.bzdmMap.put(bean.getDm(), bean);
			}
			log.info("加载V_ZH_BZDM:" + WebContext.bzdmMap.size() + "条");
			DBHelper.closeResultSet(rs);
			DBUtils.closeStatement(st);

			WebContext.fyMap.clear();
			st = conn.createStatement();
			String sql="";
			if(WebContext.FyCode.endsWith("0000")){
				sql = "select FYDM,FJM,DM,FYMC from TS_FYMC where FYDM like '"
						+ WebContext.FyCode.substring(0, 2) + "%' "
						//+ WebContext.FyCode + "%' "
						+ " and isnull(SFJY,'0')<>'1' order by FJM ";
			}else{
				sql = "select FYDM,FJM,DM,FYMC from TS_FYMC where FYDM like '"
						//+ WebContext.FyCode.substring(0, 2) + "%' "
						+ WebContext.FyCode + "%' "
						+ " and isnull(SFJY,'0')<>'1' order by FJM ";
			}
			
			rs = st.executeQuery(sql);
			while (rs.next()) {
				Fy bean = new Fy();
				bean.setFydm(rs.getString("FYDM"));
				bean.setFjm(rs.getString("FJM"));
				bean.setDm(rs.getString("DM"));
				bean.setFymc(rs.getString("FYMC"));
				
				//增加判断指定排除不需要上报的法院信息
				if(!UtilComm.isEmpty(WebContext.FILTER_FYDM)){
					if(WebContext.FILTER_FYDM.indexOf(bean.getFydm()) > -1){
						log.info("排除上报法院:"+bean.getFydm());
						continue;
					}
				}
				WebContext.fyMap.put(bean.getFydm(), bean);
			}
			log.info("加载TS_FYMC:" + WebContext.fyMap.size() + "条");
			DBHelper.closeResultSet(rs);
			DBUtils.closeStatement(st);
			
			WebContext.allfyMap.clear();
			st = conn.createStatement();
			rs = st.executeQuery("select FYDM,FJM,DM,FYMC from TS_FYMC where FYDM like '"
					+ WebContext.FyCode.substring(0, 2) + "%' "
					+ " order by FJM ");
			while (rs.next()) {
				Fy bean = new Fy();
				bean.setFydm(rs.getString("FYDM"));
				bean.setFjm(rs.getString("FJM"));
				bean.setDm(rs.getString("DM"));
				bean.setFymc(rs.getString("FYMC"));
				
				WebContext.allfyMap.put(bean.getFydm(), bean);
			}
			log.info("加载TS_FYMC:" + WebContext.allfyMap.size() + "条");
			DBHelper.closeResultSet(rs);
			DBUtils.closeStatement(st);
		} catch (Exception e) {
			log.error("load()",e);
		} finally {
			DBHelper.closeConn(conn);
		}
	}

	/**
	 * 加载个案件库的法院连接
	 */
	public static void loadFyConn() {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			conn = DBHelper.getConn("export");
			st = conn.createStatement();
			String id = "XSYJ";
			if("ZX".equals(WebContext.TYPE)){//执行，读取执行库信息
				id = "ZX";
			}
			log.info("开始读取TS_DB中ID1为 "+id+" 的生产库配置");
			if (WebContext.fyConnDetialMap.size() > 0) {
				WebContext.fyConnDetialMap.clear();
			}
			
			rs = st.executeQuery("SELECT * FROM TS_DB WHERE ID1 ='"+id+"' ");
			while (rs.next()) {
				String fydm = StringUtils.trim(rs.getString("ID2"));
				String driver = StringUtils.trim(rs.getString("CONN_DRIVER"));
				String url = StringUtils.trim(rs.getString("CONN_URL"));
				String user = StringUtils.trim(rs.getString("CONN_USER"));
				String password = StringUtils.trim(rs.getString("CONN_PASSWORD"));
				Map<String, String> map = new HashMap<String, String>();
				map.put("CONN_DRIVER", driver);
				map.put("CONN_URL", url);
				map.put("CONN_USER", user);
				map.put("CONN_PASSWORD", password);
				WebContext.fyConnDetialMap.put(fydm, map);
				//log.info("当前获取:"+fydm+",链接："+url+",数量:"+WebContext.fyConnDetialMap.size());
			}
			log.info("加载TS_DB 数据库连接:" + WebContext.fyConnDetialMap.size() + " 条库连接数");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}
	}
	
	public static void loadSbConn() {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			conn = DBHelper.getConn("export");
			st = conn.createStatement();
			String id = "SBTJ";
			log.info("开始读取TS_DB中ID1为 "+id+" 的生产库配置");
			if (WebContext.sbConnDetialMap.size() > 0) {
				WebContext.sbConnDetialMap.clear();
			}
			
			rs = st.executeQuery("SELECT * FROM TS_DB WHERE ID1 ='"+id+"' ");
			while (rs.next()) {
				String fydm = StringUtils.trim(rs.getString("ID2"));
				String driver = StringUtils.trim(rs.getString("CONN_DRIVER"));
				String url = StringUtils.trim(rs.getString("CONN_URL"));
				String user = StringUtils.trim(rs.getString("CONN_USER"));
				String password = StringUtils.trim(rs.getString("CONN_PASSWORD"));
				String lastupdate = rs.getString("LASTUPDATE");
				if(null==lastupdate){
					lastupdate="";
				}
				Map<String, String> map = new HashMap<String, String>();
				map.put("CONN_DRIVER", driver);
				map.put("CONN_URL", url);
				map.put("CONN_USER", user);
				map.put("CONN_PASSWORD", password);
				WebContext.sbConnDetialMap.put(fydm, map);
				WebContext.lastupdateMap.put(fydm, lastupdate);
			}
			log.info("加载TS_DB 数据库连接:" + WebContext.fyConnDetialMap.size() + " 条库连接数");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}
	}
	
	public static void loadAjConn() {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			for(Map.Entry<String, Map<String,String>> entry : WebContext.sbConnDetialMap.entrySet()){
				int count = 0;
				do{
					conn = DBHelper.getConn(entry.getValue().get("CONN_DRIVER"), entry.getValue().get("CONN_URL"), entry.getValue().get("CONN_USER"), entry.getValue().get("CONN_PASSWORD"));
					if(conn!=null){
						break;
					}else{count++;}
				}while(count<5);
				if(conn==null){
					log.error("获取链接5次此仍然异常:"+entry.getValue());	
					continue;
				}else{
					log.debug(entry.getKey()+"成功获得数据库连接，开始采集数据......");
				}
				st=conn.createStatement();
				String sql = "";
				sql="SELECT max(LASTUPDATE) LASTUPDATE FROM EAJ where LASTUPDATE>='" + WebContext.lastupdateMap.get(entry.getKey()) +"'";
				if(null!=WebContext.FILTER && !"".equals(WebContext.FILTER)){
					sql += " and " +WebContext.FILTER;
				}
				rs=st.executeQuery(sql);
				String d="";
				while(rs.next()){
					d=rs.getString("LASTUPDATE").substring(0, 19);
				}
				sql = "insert into AJ_SB_WFZ select AHDM,FYDM,'0','0' from EAJ where LASTUPDATE>='" + WebContext.lastupdateMap.get(entry.getKey()) +"' and LASTUPDATE<'" + d+"'";
				if(null!=WebContext.FILTER && !"".equals(WebContext.FILTER)){
					sql += " and " +WebContext.FILTER;
				}
				st.executeUpdate(sql);
				if(!"".equals(d)){
					conn=DBHelper.getConn("export");
					st=conn.createStatement();
					sql="update TS_DB set LASTUPDATE='"+d+"' where ID1='SBTJ' and ID2='"+entry.getKey()+"'";
					st.executeUpdate(sql);
					WebContext.lastupdateMap.put(entry.getKey(), d);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs); 
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}
	}
	
	public static void loadTjConn() {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			conn = DBHelper.getConn("export");
			st = conn.createStatement();
			String id = "SB";
			log.info("开始读取TS_DB中ID1为 "+id+" 的生产库配置");
			if (WebContext.tjConnDetialMap.size() > 0) {
				WebContext.tjConnDetialMap.clear();
			}
			
			rs = st.executeQuery("SELECT * FROM TS_DB WHERE ID1 ='"+id+"' ");
			while (rs.next()) {
				String driver = StringUtils.trim(rs.getString("CONN_DRIVER"));
				String url = StringUtils.trim(rs.getString("CONN_URL"));
				String user = StringUtils.trim(rs.getString("CONN_USER"));
				String password = StringUtils.trim(rs.getString("CONN_PASSWORD"));
				Map<String, String> map = new HashMap<String, String>();
				map.put("CONN_DRIVER", driver);
				map.put("CONN_URL", url);
				map.put("CONN_USER", user);
				map.put("CONN_PASSWORD", password);
				WebContext.tjConnDetialMap.put("SB", map);
			}
			log.info("加载TS_DB 数据库连接:" + WebContext.fyConnDetialMap.size() + " 条库连接数");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}
	}
	
	public static void loadAjLxZh() {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			WebContext.AjlxZhMap.clear();
			conn = DBHelper.getConn("export");
			st = conn.createStatement();
			rs = st.executeQuery("SELECT AJLX,AJZLX,AJLXBS,FL,QTDM,DZ FROM T_AJLX_DMZH");
			Map<String,String> map1=new HashMap<String,String>();
			Map<String,String> map2=new HashMap<String,String>();
			Map<String,String> map3=new HashMap<String,String>();
			Map<String,String> map4=new HashMap<String,String>();
			Map<String,String> map5=new HashMap<String,String>();
			Map<String,String> map6=new HashMap<String,String>();
			Map<String,String> map7=new HashMap<String,String>();
			Map<String,String> map8=new HashMap<String,String>();
			while (rs.next()) {
				String fl=rs.getString("FL"),ajlxbs="",ajlx="";
				if("1".equals(fl)){
					ajlx=rs.getString("AJLX");
					ajlxbs=rs.getString("AJLXBS");
					map1.put(ajlx, ajlxbs);
				}else if("2".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("AJZLX");
					ajlxbs=rs.getString("AJLXBS");
					map2.put(ajlx, ajlxbs);
				}else if("3".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("DZ");
					ajlxbs=rs.getString("AJLXBS");
					map3.put(ajlx, ajlxbs);
				}else if("4".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("AJZLX")+rs.getString("QTDM");
					ajlxbs=rs.getString("AJLXBS");
					map4.put(ajlx, ajlxbs);
				}else if("5".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("QTDM");
					ajlxbs=rs.getString("AJLXBS");
					map5.put(ajlx, ajlxbs);
				}else if("6".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("AJZLX")+rs.getString("QTDM");
					ajlxbs=rs.getString("AJLXBS");
					map6.put(ajlx, ajlxbs);
				}else if("7".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("AJZLX")+rs.getString("QTDM");
					ajlxbs=rs.getString("AJLXBS");
					map7.put(ajlx, ajlxbs);
				}else if("8".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("DZ")+rs.getString("QTDM");
					ajlxbs=rs.getString("AJLXBS");
					map8.put(ajlx, ajlxbs);
				}
			}
			WebContext.AjlxZhMap.put("1", map1);
			WebContext.AjlxZhMap.put("2", map2);
			WebContext.AjlxZhMap.put("3", map3);
			WebContext.AjlxZhMap.put("4", map4);
			WebContext.AjlxZhMap.put("5", map5);
			WebContext.AjlxZhMap.put("6", map6);
			WebContext.AjlxZhMap.put("7", map7);
			WebContext.AjlxZhMap.put("8", map8);
			log.info("加载T_AJLX_DMZH:" + WebContext.AjlxZhMap.size() + " 条案件类型");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}
	}

	/**
	 * 加载转换关系代码
	 */
	public static void loadDmzhAjlx() {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			WebContext.ajlxdmzhMap.clear();
			conn = DBHelper.getConn("export");
			st = conn.createStatement();
			rs = st.executeQuery("SELECT DM,AJLXDM FROM T_AJLX_DMZH");
			while (rs.next()) {
				String dm = rs.getString("DM");
				int ajlxdm = (Integer) rs.getInt("AJLXDM");
				WebContext.ajlxdmzhMap.put(String.valueOf(ajlxdm), dm);
			}
			log.info("加载T_AJLX_DMZH:" + WebContext.ajlxdmzhMap.size() + " 条案件类型");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}
	}
	
	private static void loadAjlxdmZh15(){
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			WebContext.AjlxdmZh15Ajlxbs.clear();
			conn = DBHelper.getConn("export");
			st = conn.createStatement();
			rs = st.executeQuery("SELECT AJLXDM,AJLXBS FROM T_AJLX_DMZH");
			while (rs.next()) {
				String ajlxdm = rs.getString("AJLXDM");
				String ajlxbs = rs.getString("AJLXBS");
				WebContext.AjlxdmZh15Ajlxbs.put(ajlxdm, ajlxbs);
			}
			log.info("加载T_AJLX_DMZH 15类型转换:" + WebContext.AjlxdmZh15Ajlxbs.size() + " 条案件类型");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}
	}
	
	private static void loadTsbzdm15(){
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			WebContext.codeMcMap.clear();
			conn = DBHelper.getConn("export");
			st = conn.createStatement();
			rs = st.executeQuery("SELECT CODE,MC FROM TS_BZDM_15");
			while (rs.next()) {
				String code = rs.getString("CODE");
				String mc = rs.getString("MC");
				WebContext.codeMcMap.put(code,mc);
			}
			log.info("加载TS_BZDM_15 15类型转换:" + WebContext.codeMcMap.size() + " 条案件类型");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}
	}
	
	private static void load15Xsd(){
		SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		String path = WebAppContext.getServletContextEx().getRealPath("/") + "/WEB-INF/xsd/15/";
		String fileName = "spdtAndAjList_all_15.xsd";//"9902_实时收结.xsd";
		File schemaFile = new File(path+fileName);
		if(schemaFile.exists()){
			Schema schema = null;
			try {
				schema = schemaFactory.newSchema(schemaFile);
			} catch (SAXException e) {
				log.error(e.getMessage(),e);
			}
			if(schema!=null){
				WebContext.validator15 = schema.newValidator();
				log.info("加载文件[spdtAndAjList_all_15.xsd]实体化成功");
			}else{
				log.error("文件[spdtAndAjList_all_15.xsd],Schema 对象加载失败");
			}
		}
	}
	private void loadTsAy(){
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			WebContext.TS_AY.clear();
			conn = DBHelper.getConn("export");
			st = conn.createStatement();
			rs = st.executeQuery("SELECT DM,V2014DM FROM TS_AY");
			while (rs.next()) {
				String dm = rs.getString("DM");
				String dm2014 = rs.getString("V2014DM");
				WebContext.TS_AY.put(dm, dm2014);
			}
			log.info("加载TS_AY 15类型转换:" + WebContext.TS_AY.size() + " 条");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}
	}
	//TS_AY
}
