package tdh.web;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.validation.Validator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tdh.bean.BzDm;
import tdh.bean.Fy;
import tdh.db.DBHelper;
import tdh.util.CommUtil;

/**
 *
 * @author chenjx
 * @version 创建时间：2015年8月19日 下午4:46:41
 */
public class WebContext {

	private static Log log = LogFactory.getLog(WebContext.class);
	/**
	 * 指定需要上报的法院高院的代码（6位）
	 */
	public static String FyCode;

	/**
	 * 指定需要上报的类型代码
	 */
	public static final List<String> SBAJLXDMLIST = new ArrayList<String>();

	public static String DIR = "C://ajxml";

	/**
	 * 指定排除不上报的法院
	 */
	public static String FILTER_FYDM = StringUtils.EMPTY;

	/**
	 * 5分钟收结接口地址
	 */
	public static String JKURL =  StringUtils.EMPTY;
	/**
	 * 接口命名空间
	 */
	public static String NAMESPACE = "http://webservice.spdt.thunisoft.com/";
	
	public static String METHOD = StringUtils.EMPTY;
	
	public static String PARAMETER = StringUtils.EMPTY;

	/**
	 * 旧接口地址
	 */
	public static String OLDURL =  StringUtils.EMPTY;

	/**
	 * 案件列表过滤条件
	 */
	public static String FILTER =  StringUtils.EMPTY;
	
	public static Map<String,String> lastupdateMap =  new HashMap<String, String>();
//	public static String LASTUPDATEFILTER =  StringUtils.EMPTY;

	/**
	 * 工作线程数量
	 */
	public static Integer THREADS_NUMS = 10;

	/**
	 * 长度为18、19位的案件列表是否转化为15位
	 */
	public static boolean AHDM_FLAG;
	/**
	 * 数据类型是审判，还是执行
	 */
	public static String TYPE;

	public static String VERSION  = "09";
	
	/**
	 * 广东地区报送删除时间记录
	 * */
	public static String LASTSCSJ = "";
	/**
	 * 存储临时序号
	 */
	public static Map<String, Integer> numMap = new HashMap<String, Integer>();
	
	/**
	 * 法院代码对应到最高法院规范DM
	 */
	public static Map<String, Fy> fyMap = new LinkedHashMap<String, Fy>();
	
	public static Map<String, Fy> allfyMap = new LinkedHashMap<String, Fy>();
	
	/**
	 * 标注代码的转换
	 */
	public static Map<String, BzDm> bzdmMap = new HashMap<String, BzDm>();
	
	/**
	 * T_AJLX_DMZH 表进行代码转换
	 */
	public static Map<String,String>  ajlxdmzhMap = new HashMap<String,String>();
	
	public static Map<String, Map<String, String>> fyConnDetialMap = new HashMap<String, Map<String, String>>();
	
	public static Map<String, Map<String, String>> sbConnDetialMap = new HashMap<String, Map<String, String>>();
	
	public static Map<String, Map<String, String>> tjConnDetialMap = new HashMap<String, Map<String, String>>();
	
	/**09案件类型代码转15案件类型标识用的*/
	public static Map<String,String> AjlxdmZh15Ajlxbs = new HashMap<String,String>();
	
	public static Map<String,Map<String,String>> AjlxZhMap = new HashMap<String,Map<String,String>>();
	
	public static Map<String,String> TS_AY = new HashMap<String,String>();
	
	public static Validator validator15 = null;
	
	private static String DBTYPE = StringUtils.EMPTY;
	
	/**TS_BZDM_15表CODE与MC*/
	public static Map<String, String> codeMcMap = new HashMap<String, String>();
	
	public static String getDbType() {
		return DBTYPE;
	}

	public static void setDbType(String dbType) {
		DBTYPE = dbType;
	}

	public static synchronized int getXh(String uuid) {
		int xh = numMap.get(uuid);
		xh++;
		numMap.put(uuid, xh);
		return xh;
	}

	public static String getBzdm(String bzdm) {
		if (CommUtil.isEmpty(bzdm))
			return "";
		BzDm dm = bzdmMap.get(bzdm);
		String value = (dm == null ? StringUtils.EMPTY : CommUtil.isEmpty(dm.getV2014dm()) ? bzdm : dm.getV2014dm());
		// 排除v2014code为‘不转’
		if (value.contains("不转"))
			value = bzdm;
		// value = CommUtil.removeCodePrefix(value);
		return value;
	}

	/**
	 * 根据法院分级码 获取相应法院的生产库数据库连接 当基层法院未配置链接时 返回市（中院）的数据库链接
	 * 
	 * @param fjm
	 * @return
	 * @author chenjx
	 * @date 2015年8月19日
	 */
	public static Connection getFyEcourtConn(String fydm) {
		Connection conn = null;
		Map<String, String> map = null;
		map = fyConnDetialMap.get(fydm);
		if (map == null) {
			map = fyConnDetialMap.get(fydm.substring(0, 4));
			if (map == null) {
				map = fyConnDetialMap.get(fydm.substring(0, 2));
				if (map == null) {
					return null;
				}
			}
		}
		log.debug(fydm+"开始进行数据库连接......");
		int count = 0;
		do{
			conn = DBHelper.getConn(map.get("CONN_DRIVER"), map.get("CONN_URL"), map.get("CONN_USER"), map.get("CONN_PASSWORD"));
			if(conn!=null){
				break;
			}else{count++;}
		}while(count<5);
		if(conn==null){
			log.error("获取链接5次此仍然异常:"+map);			
		}else{
			log.debug(fydm+"成功获得数据库连接......");
		}
		return conn;
	}
	
	public synchronized static Connection getSbEcourtConn(String fydm) {
		Connection conn = null;
		Map<String, String> map = null;
		map = sbConnDetialMap.get(fydm);
		if (map == null) {
			map = sbConnDetialMap.get(fydm.substring(0, 4));
			if (map == null) {
				map = sbConnDetialMap.get(fydm.substring(0, 2));
				if (map == null) {
					return null;
				}
			}
		}
		log.debug(fydm+"开始进行数据库连接......");
		int count = 0;
		do{
			conn = DBHelper.getConn(map.get("CONN_DRIVER"), map.get("CONN_URL"), map.get("CONN_USER"), map.get("CONN_PASSWORD"));
			if(conn!=null){
				break;
			}else{count++;}
		}while(count<5);
		if(conn==null){
			log.error("获取链接5次此仍然异常:"+map);			
		}else{
			log.debug(fydm+"成功获得数据库连接......");
		}
		return conn;
	}
	
	public synchronized static String getLastupdate(String fydm) {
		String lastupdate = null;
		lastupdate = lastupdateMap.get(fydm);
		if (lastupdate == null) {
			lastupdate = lastupdateMap.get(fydm.substring(0, 4));
			if (lastupdate == null) {
				lastupdate = lastupdateMap.get(fydm.substring(0, 2));
				if (lastupdate == null) {
					return "";
				}
			}
		}
		return lastupdate;
	}
	
	public static Connection getTjEcourtConn() {
		Connection conn = null;
		Map<String, String> map = null;
		map = tjConnDetialMap.get("SB");
		if (map == null) {
			return null;
		}
		log.debug("SB开始进行数据库连接......");
		int count = 0;
		do{
			conn = DBHelper.getConn(map.get("CONN_DRIVER"), map.get("CONN_URL"), map.get("CONN_USER"), map.get("CONN_PASSWORD"));
			if(conn!=null){
				break;
			}else{count++;}
		}while(count<5);
		if(conn==null){
			log.error("获取链接5次此仍然异常:"+map);			
		}else{
			log.debug("SB成功获得数据库连接......");
		}
		return conn;
	}
	
	public static String getAyzh(String sourceAy){
		String newAy = TS_AY.get(sourceAy);
		if(CommUtil.isEmpty(newAy)){
			return sourceAy;
		}else{
			return newAy;
		}
	}
}
