package tdh.xsyj.executeHB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import tdh.bean.Fy;
import tdh.db.DBHelper;
import tdh.frame.common.UtilComm;
import tdh.util.CalendarUtil;
import tdh.util.CommUtil;
import tdh.web.WebContext;
import tdh.xsyj.LogService15;

public class CourtThreadHB  implements Callable<Map<String,Integer>>{
	private static Log logger = LogFactory.getLog(CourtThreadHB.class);
	private static List<String> ajlxbsList=null;
	static{
		ajlxbsList =Arrays.asList("0500,0501,0502,0503,0504,0505,0506,0507".split(","));
	}
	private Fy fy;
	private int id ;
	private String dateTime;
	private Element root;
	
	public CourtThreadHB(Fy fy,String dateTime, int id,Element root ) {
		super();
		this.fy = fy;
		this.dateTime = dateTime;
		this.id = id;
		this.root = root;
	}

	
	@Override
	public Map<String,Integer> call() throws Exception {
		Thread.currentThread().setName("法院代码:"+fy.getFydm());
		/*Document  doc = DocumentHelper.createDocument();
		doc.setXMLEncoding("UTF-8");*/
		logger.info("["+Thread.currentThread().getName()+"] 开始生成有效数据..");
		Element element = root.addElement("R");
		element.addElement("id").setText(/*String.valueOf(id)*/fy.getDm());
		Element sajls = element.addElement("drxsajsl");//当日新收案件数量
		Element jajls = element.addElement("dryjajsl");//当日已结案件数量
		String rq = CalendarUtil.getGsSj(dateTime, "yyyyMMdd");
		
		Element lalist = element.addElement("lalist");
		Map<String,Integer> lacount = loadLaElement(lalist,rq,fy);
		//map.put("LA_SJLX", sjlxycsl);
		/*map.put("LA_"+fy.getFydm()+"_SALY", saycsl);
		map.put("LA_"+fy.getFydm()+"_LAAY", ayycsl);
		map.put("LA_LAAYZTLX", xzxwfwsl);
		map.put("LA_LAAYXZXWZL", xzxwzlsl);
		map.put("LA_COUNT", count);
		map.put("LA_YC_COUNT", countycsl);*/
		//removeEmptyNode(lalist);
		sajls.setText(String.valueOf(lacount.get("LA_"+fy.getFydm()+"_COUNT")));
		
		Element jalist = element.addElement("jalist");
		Map<String,Integer> jacount = loadJaElement(jalist,rq,fy);
		//removeEmptyNode(jalist);
		jajls.setText(String.valueOf(jacount.get("JA_"+fy.getFydm()+"_COUNT")));
		if(lacount.get("LA_"+fy.getFydm()+"_COUNT").equals(0)  && jacount.get("JA_"+fy.getFydm()+"_COUNT")==0){
			root.remove(element);
		}
		/*if(lacount==0 ){
			element.remove(lalist);
		}
		if(jacount==0){
			element.remove(jalist);
		}*/
		
		Map<String,Integer> map =new HashMap<String,Integer>();
		map.put("XSSL", lacount.get("LA_"+fy.getFydm()+"_COUNT"));
		map.putAll(lacount);
		map.putAll(jacount);
		map.put("YJSL", jacount.get("JA_"+fy.getFydm()+"_COUNT"));
		map.put("FYDM", Integer.parseInt(fy.getFydm()));
		return map;
	}

	private Map<String,Integer> loadLaElement(Element root,String rq,Fy fy){
		StringBuffer sql = new StringBuffer();
		String dm=fy.getFydm().substring(0,2);
		sql.append(" SELECT ");
		sql.append(" ajbs AJBS,sjlx SJLX,jbfy JBFY,sarq SARQ,ah AH,bydjlarq BYDJLARQ,byslcdrq BYSLCDRQ,case when isnull(djlarq,'')<>''  then djlarq else sarq end as DJLARQ,saly SALY,laay LAAY,laayztlx LAAYZTLX,laayxzxwzl LAAYXZXWZL");
		sql.append(" FROM aj_info ");
		sql.append(" WHERE jbfy=? AND case when isnull(djlarq,'')<>''  then djlarq else sarq end=?");
		Connection conn = WebContext.getFyEcourtConn(fy.getFydm());
		System.out.println(sql);
		if(!UtilComm.isEmpty(WebContext.FILTER)){
			sql .append(" AND " +WebContext.FILTER);
		}
		
		PreparedStatement pst = null;
		ResultSet rs= null;
		int count = 0;
		logger.info("["+Thread.currentThread().getName()+"] 立案查询立案sql:"+sql.toString()+"\n参数："+fy.getDm()+", "+CommUtil.convertRq8(rq));
		Map<String,Integer> map = new HashMap<String,Integer>();
		try {
			if(conn==null){
				logger.info("["+Thread.currentThread().getName()+"] 法院代码:"+fy.getFydm()+",代码："+fy.getDm()+",获取链接异常,.");
				return map;
			}
			pst = conn.prepareStatement(sql.toString());
			pst.setString(1, fy.getDm());
			pst.setString(2, CommUtil.convertRq8(rq));
			rs = pst.executeQuery();
			int sqlCount = 0;
			int sjlxycsl = 0;//数据类型异常数量
			int saycsl = 0;//收案异常数量
			int ayycsl = 0;//立案案由异常数量
			int xzxwfwsl = 0;//行政管理范围
			int xzxwzlsl = 0;//行政行为种类
			while(rs.next()) {
				sqlCount++;
				String ajlxbs = StringUtils.trim(rs.getString("SJLX"));
				if(CommUtil.isEmpty(ajlxbs)){
					logger.error("["+Thread.currentThread().getName()+"] 数据类型异常数据过滤：AJBS=>"+rs.getString("AJBS")+",SJLX=>"+CommUtil.trim(rs.getString("SJLX"))+","+fy);
					sjlxycsl++;
					continue;
				}
				//---开始针对必填项 异常数据跳过操作
				if(CommUtil.isEmpty(getCodeValue(CommUtil.trim(rs.getString("SALY")),false))){
					logger.error("["+Thread.currentThread().getName()+"] 收案来源异常数据过滤：AJBS=>"+rs.getString("AJBS")+",SALY=>"+getCodeValue(CommUtil.trim(rs.getString("SALY")),false)+","+fy);
					saycsl++;
					continue;
				}

				//判断立案案由
				if(ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxbs.startsWith("05") || ajlxbs.startsWith("10")){
					if(StringUtils.isEmpty(CommUtil.trim(rs.getString("LAAY")))){
						logger.error("["+Thread.currentThread().getName()+"] ["+ajlxbs+"]立案案由异常数据过滤：AJBS=>"+rs.getString("AJBS")+",LAAY=>"+rs.getString("LAAY")+","+fy);
						ayycsl++;
						continue;
					}
				}
				//行政案件才判断
				if(ajlxbs.startsWith("04")||ajlxbsList.contains(ajlxbs)){
					if(CommUtil.isEmpty(getCodeValue(CommUtil.trim(rs.getString("LAAYZTLX")),false))){
						logger.error("["+Thread.currentThread().getName()+"] ["+ajlxbs+"]行政管理范围异常数据过滤：AJBS=>"+rs.getString("AJBS")+",LAAYZTLX=>"+getCodeValue(CommUtil.trim(rs.getString("LAAYZTLX")),false)+","+fy);
						xzxwfwsl++;
						continue;
					}
					if(CommUtil.isEmpty(StringUtils.trimToEmpty(rs.getString("LAAYXZXWZL")))){
						logger.error("["+Thread.currentThread().getName()+"] ["+ajlxbs+"]行政行为种类异常数据过滤：AJBS=>"+rs.getString("AJBS")+",LAAYXZXWZL=>"+StringUtils.trimToEmpty(rs.getString("LAAYXZXWZL"))+","+fy);
						xzxwzlsl++;
						continue;
					}
				}
				count++;
				Element element = root.addElement("R");
				element.addElement("ajbs").setText(rs.getString("AJBS"));
				element.addElement("ajlx").setText(ajlxbs);
				element.addElement("ah").setText( StringUtils.trimToEmpty(rs.getString("AH")));
				element.addElement("jbfy").setText(rs.getString("JBFY"));
				if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("SARQ")))){
					element.addElement("sarq").setText(rs.getString("SARQ"));
				}else{
					element.addElement("sarq").setText(rs.getString("DJLARQ"));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("BYDJLARQ")))){
					element.addElement("bydjlarq").setText(CommUtil.convertRq8(rs.getString("BYDJLARQ")));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("BYSLCDRQ")))){
					element.addElement("byslcdrq").setText(CommUtil.convertRq8(rs.getString("BYSLCDRQ")));
				}
				element.addElement("djlarq").setText(rs.getString("DJLARQ"));
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("LAAY")))){
					element.addElement("laay").setText(rs.getString("LAAY"));//cdx
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("SALY")))){
					element.addElement("saly").setText(getCodeValue(rs.getString("SALY"),false));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("LAAYZTLX")))){
					element.addElement("laayztlx").setText(getCodeValue(rs.getString("LAAYZTLX"),false));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("LAAYXZXWZL")))){
					element.addElement("laayxzxwzl").setText( StringUtils.trimToEmpty(rs.getString("LAAYXZXWZL")));//cdx
				}
				LogService15.insLaLog(rq,fy.getDm(),ajlxbs,rs.getString("AJBS"),rs.getString("AH"),rs.getString("SARQ"),rs.getString("BYDJLARQ"),rs.getString("BYSLCDRQ"), 
						rs.getString("DJLARQ"),rs.getString("LAAY"),getCodeValue(CommUtil.trim(rs.getString("SALY")),false),getCodeValue(CommUtil.trim(rs.getString("LAAYZTLX")),false),rs.getString("LAAYXZXWZL"), fy.getFydm());
			}
			logger.info("["+Thread.currentThread().getName()+"] 库中数量:"+sqlCount+",实际xml中数量:"+count+" ,立案sql:"+sql.toString());
			int countycsl = sjlxycsl+saycsl+ayycsl+xzxwfwsl+xzxwzlsl;//全部异常数量
			map.put("LA_"+fy.getFydm()+"_SJLX", sjlxycsl);
			map.put("LA_"+fy.getFydm()+"_SALY", saycsl);
			map.put("LA_"+fy.getFydm()+"_LAAY", ayycsl);
			map.put("LA_"+fy.getFydm()+"_LAAYZTLX", xzxwfwsl);
			map.put("LA_"+fy.getFydm()+"_LAAYXZXWZL", xzxwzlsl);
			map.put("LA_"+fy.getFydm()+"_COUNT", count);
			map.put("LA_"+fy.getFydm()+"_YC_COUNT", countycsl);
			if(countycsl>0){
				logger.error("["+Thread.currentThread().getName()+"]立案共过滤了"+countycsl+"条异常数据,其中数据类型异常："+sjlxycsl+",收案异常数量:"+saycsl+",立案案由异常数量:"+ayycsl+",行政管理范围:"+xzxwfwsl+",行政行为种类:"+xzxwzlsl);
			}else{
				logger.info("["+Thread.currentThread().getName()+"]立案本次无异常数据");
			}
		} catch (SQLException e) {
			logger.info("["+Thread.currentThread().getName()+"] 查询立案异常。。",e);
		}finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(pst);
			DBHelper.closeConn(conn);
		}
		return map;
	}
	
	private Map<String,Integer> loadJaElement(Element root,String rq,Fy fy){
		StringBuffer sql = new StringBuffer();
		String dm=fy.getFydm().substring(0,2);
		sql.append("SELECT ");
		sql.append(" ajbs AJBS,sjlx SJLX,jbfy JBFY,sarq SARQ,ah AH,bydjlarq BYDJLARQ,byslcdrq BYSLCDRQ,case when isnull(djlarq,'')<>''  then djlarq else sarq end as DJLARQ,saly SALY,laay LAAY,laayztlx LAAYZTLX,laayxzxwzl LAAYXZXWZL,jaay JAAY,jarq JARQ,jafs JAFS");
		sql.append(" FROM aj_info ");
		sql.append(" WHERE jbfy=? AND jarq=?");
		Connection conn = WebContext.getFyEcourtConn(fy.getFydm());
		if(!UtilComm.isEmpty(WebContext.FILTER)){
			sql .append(" AND " +WebContext.FILTER);
		}
		PreparedStatement pst = null;
		ResultSet rs= null;
		int count = 0;
		logger.info("结案查询结案sql："+sql.toString() +"\n参数："+fy.getDm()+","+ CommUtil.convertRq8(rq));
		Map<String,Integer> map = new HashMap<String, Integer>();
		try {
			if(conn==null){
				logger.info("["+Thread.currentThread().getName()+"] 法院代码:"+fy.getFydm()+",代码："+fy.getDm()+",获取链接异常,.");
				return map;
			}
			pst = conn.prepareStatement(sql.toString());
			pst.setString(1, fy.getDm());
			pst.setString(2, CommUtil.convertRq8(rq));
			rs = pst.executeQuery();
			int sqlCount = 0;
			int sjlxsl = 0;//		数据类型
			int salysl = 0;//		收案案由
			int jarqsl = 0;//		结案日期
			int jafssl = 0;//		结案方式
			int laayztlxsl = 0;//	行政行为范围
			int laayxzxwzlsl = 0;//	行政行为种类
			int laaysl = 0;//		立案案由
			int aysl = 0;//			案由	
			while(rs.next()) {
				sqlCount++;
				String ajlxbs = StringUtils.trim(rs.getString("SJLX"));
				if(CommUtil.isEmpty(ajlxbs)){
					logger.error("["+Thread.currentThread().getName()+"] 结案数据类型异常数据过滤：AJBS=>"+rs.getString("AJBS")+",SJLX=>"+CommUtil.trim(rs.getString("SJLX"))+","+fy);
					sjlxsl++;
					continue;
				}
				//---开始针对必填项 异常数据跳过操作
				if(CommUtil.isEmpty(getCodeValue(CommUtil.trim(rs.getString("SALY")),false))){
					logger.error("结案收案案由异常数据过滤：AJBS=>"+rs.getString("AJBS")+",SALY=>"+getCodeValue(CommUtil.trim(rs.getString("SALY")),false)+","+fy);
					salysl++;
					continue;
				}
				if(CommUtil.isEmpty(CommUtil.convertRq8(CommUtil.trim(rs.getString("JARQ"))))){
					logger.error("结案结案日期异常数据过滤：AJBS=>"+rs.getString("AJBS")+",JARQ=>"+getCodeValue(CommUtil.trim(rs.getString("JARQ")),false)+","+fy);
					jarqsl++;
					continue;
				}
				if(CommUtil.isEmpty(getCodeValue(CommUtil.trim(rs.getString("JAFS")),false))){
					logger.error("结案结案方式异常数据过滤：AJBS=>"+rs.getString("AJBS")+",JAFS=>"+getCodeValue(CommUtil.trim(rs.getString("JAFS")),false)+","+fy);
					jafssl++;
					continue;
				}
				//行政案件才判断
				String xzglfw = CommUtil.trim(rs.getString("LAAYZTLX"));
				String xzxwzl = CommUtil.trim(rs.getString("LAAYXZXWZL"));
				if(ajlxbs.startsWith("04")||ajlxbsList.contains(ajlxbs)){ 
					//===========
					if(CommUtil.isEmpty(getCodeValue(xzglfw,false))){
						logger.error("结案行政管理范围异常数据过滤：AJBS=>"+rs.getString("AJBS")+",LAAYZTLX=>"+xzglfw+","+fy);
						laayztlxsl++;
						continue;
					}
					if(CommUtil.isEmpty( getCodeValue(StringUtils.trimToEmpty(xzxwzl),true))){
						logger.error("结案行政管理种类异常数据过滤：AJBS=>"+rs.getString("AJBS")+",LAAYXZXWZL=>"+StringUtils.trimToEmpty(xzxwzl)+","+fy);
						laayxzxwzlsl++;
						continue;
					}
				}

				//立案案由判断
				if(ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxbs.startsWith("05") || ajlxbs.startsWith("10")){
					if(StringUtils.isEmpty(CommUtil.trim(rs.getString("LAAY")))){
						logger.error("["+Thread.currentThread().getName()+"] 结案立案案由异常数据过滤：AJBS=>"+rs.getString("AJBS")+",LAAY=>"+rs.getString("LAAY")+","+fy);
						laaysl++;
						continue;
					}
				}
				String ay = CommUtil.trim(rs.getString("JAAY"));
				//结案案由过滤
				if(ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxbs.startsWith("05") || ajlxbs.startsWith("10")){
					if(StringUtils.isEmpty(ay)){
						logger.error("["+Thread.currentThread().getName()+"] 结案-案由异常数据过滤：AJBS=>"+rs.getString("AJBS")+",AY=>"+rs.getString("JAAY")+","+fy);
						aysl++;
						continue;
					}

				}
				//===========
				count++;
				Element element = root.addElement("R");
				element.addElement("ajbs").setText(rs.getString("AJBS"));
				element.addElement("ajlx").setText(ajlxbs);
				element.addElement("ah").setText( StringUtils.trimToEmpty(rs.getString("AH")));
				element.addElement("jbfy").setText(rs.getString("JBFY"));
				if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("SARQ")))){
					element.addElement("sarq").setText(rs.getString("SARQ"));
				}else{
					element.addElement("sarq").setText(rs.getString("DJLARQ"));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("BYDJLARQ")))){
					element.addElement("bydjlarq").setText(CommUtil.convertRq8(rs.getString("BYDJLARQ")));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("BYSLCDRQ")))){
					element.addElement("byslcdrq").setText(CommUtil.convertRq8(rs.getString("BYSLCDRQ")));
				}
				element.addElement("djlarq").setText(rs.getString("DJLARQ"));
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("LAAY")))){
					element.addElement("laay").setText(rs.getString("LAAY"));//cdx
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("SALY")))){
					element.addElement("saly").setText(getCodeValue(rs.getString("SALY"),false));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("LAAYZTLX")))){
					element.addElement("laayztlx").setText(getCodeValue(rs.getString("LAAYZTLX"),false));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("LAAYXZXWZL")))){
					element.addElement("laayxzxwzl").setText( StringUtils.trimToEmpty(rs.getString("LAAYXZXWZL")));//cdx
				}
				element.addElement("jarq").setText(CommUtil.convertRq8(rs.getString("JARQ")));
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("JAAY")))){
					element.addElement("jaay").setText( StringUtils.trimToEmpty(rs.getString("JAAY")));//cdx
				}
				element.addElement("jafs").setText(getCodeValue(rs.getString("JAFS"),false));
				LogService15.insJaLog(rq,fy.getDm(),ajlxbs,rs.getString("AJBS"),rs.getString("AH"),rs.getString("SARQ"),
						rs.getString("BYDJLARQ"),rs.getString("BYSLCDRQ"),rs.getString("DJLARQ"),rs.getString("LAAY"),
						getCodeValue(CommUtil.trim(rs.getString("SALY")),false),getCodeValue(CommUtil.trim(rs.getString("LAAYZTLX")),false),rs.getString("LAAYXZXWZL"),
						rs.getString("JARQ"),rs.getString("JAAY"),fy.getFydm(),rs.getString("JAFS"));
			}
			int countycsl = sjlxsl+salysl+jarqsl+laayztlxsl+laayxzxwzlsl+laaysl+aysl+jafssl;//全部异常数量
			map.put("JA_"+fy.getFydm()+"_SJLX", sjlxsl);
			map.put("JA_"+fy.getFydm()+"_SALY", salysl);
			map.put("JA_"+fy.getFydm()+"_JARQ", jarqsl);
			map.put("JA_"+fy.getFydm()+"_LAAYZTLX", laayztlxsl);
			map.put("JA_"+fy.getFydm()+"_LAAYXZXWZL", laayxzxwzlsl);
			map.put("JA_"+fy.getFydm()+"_LAAY", laaysl);
			map.put("JA_"+fy.getFydm()+"_JAFS", jafssl);
			map.put("JA_"+fy.getFydm()+"_AY", aysl);
			map.put("JA_"+fy.getFydm()+"_COUNT", count);
			map.put("JA_"+fy.getFydm()+"_YC_COUNT", countycsl);
			logger.info("库中实际数量:"+sqlCount+",实际xml中数量:"+count+"结案sql："+sql.toString());
		} catch (SQLException e) {
			logger.error("查询结案异常。。",e);
		}finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(pst);
			DBHelper.closeConn(conn);
		}
		return map;
	}
	
	private String getCodeValue(String code,boolean isfx){
		String newCode =code; 
		if(StringUtils.isNotEmpty(code)){
			if(code.indexOf(",")!=-1){
				String[] _temp = code.split(",");
				for (int i = 0; i < _temp.length; i++) {
					if(StringUtils.isNotEmpty(StringUtils.trim(_temp[i]))){
						newCode = _temp[i];
						break; 
					}
				}
			}
		}
		if(StringUtils.isEmpty(newCode)){
			return StringUtils.EMPTY;
		}
		if(newCode.startsWith("09_") || newCode.startsWith("15_")){
			String [] _temp = newCode.split("-");
			if(_temp.length==2){
				return _temp[1];
			}
		}
		return StringUtils.isNotEmpty(newCode)?newCode:StringUtils.EMPTY;
	}
	private String ahdmConvertAhdm18(String dm,String nd,String ajbm,String ahdm){
		//getDm()+nd+getAjbm()+getAhdm().substring(getAhdm().length()-6)
		//TS_FYMC.DM+EAJ.ND+TS_AJCXBM2015.AJLXBS+AHDM
		if(ahdm.length()>18){
			return ahdm;
		}
		return dm +nd+ajbm+ahdm.substring(ahdm.length()-6);//15转16
	}
	
	
}
