package tdh.xsyj.execute15;

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class CourtThread  implements Callable<Map<String,Integer>>{
	private static Log logger = LogFactory.getLog(CourtThread.class);
	private static List<String> ajlxList=null;//国家赔偿案件中司法赔偿案件
	private static List<String> ajlxbsList=null;//国家赔偿中的行政赔偿案件
	static{
		ajlxbsList =Arrays.asList("0501,0502,0503,0504,0505,0506,0507,0508".split(","));
		ajlxList =Arrays.asList("0509,0510,0511,0512,0513,0514,0515".split(","));
	}
	private Fy fy;
	private int id ;
	private String dateTime;
	private Element root;
	
	public CourtThread(Fy fy,String dateTime, int id,Element root ) {
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
		element.addElement("id").setText(/*String.valueOf(id)*/CommUtil.long4Dm(fy.getDm()));
		Element sajls = element.addElement("drxsajsl");//当日新收案件数量
		Element jajls = element.addElement("dryjajsl");//当日已结案件数量
		String rq = CalendarUtil.getGsSj(dateTime, "yyyyMMdd");
		
		Element lalist = element.addElement("lalist");
		int lacount = loadLaElement(lalist,rq,fy);
		//removeEmptyNode(lalist);
		sajls.setText(String.valueOf(lacount));
		
		Element jalist = element.addElement("jalist");
		int jacount = loadJaElement(jalist,rq,fy);
		//removeEmptyNode(jalist);
		jajls.setText(String.valueOf(jacount));
		if(lacount==0  && jacount==0){
			root.remove(element);
		}
		/*if(lacount==0 ){
			element.remove(lalist);
		}
		if(jacount==0){
			element.remove(jalist);
		}*/
		
		Map<String,Integer> map =new HashMap<String,Integer>();
		map.put("XSSL", lacount);
		map.put("YJSL", jacount);
		return map;
	}

	private int loadLaElement(Element root,String rq,Fy fy){
		StringBuffer sql = new StringBuffer();
		Connection conn = WebContext.getFyEcourtConn(fy.getFydm());
		boolean xzxwFlag = false;
		sql.append("SELECT ");
		sql.append(" A.AHDM,A.ND,A.AJLXDM AS AJLXBS,A.AH,A.SARQ,A.JARQ,A.LARQ,A.SAAY,");
		sql.append(" B.AJLYN  AS  AJLY,");
		sql.append(" B.XZGLFW");
		//判断是否有EDSR_XZXW这张表
		if(hasTab(conn,"EDSR_XZXW")){
			xzxwFlag = true;
			sql.append(" ,(SELECT MIN(XZXWZL) FROM EDSR_XZXW WHERE EDSR_XZXW.AHDM =A.AHDM  ) AS XZXWZL");
		}
		sql.append(" FROM EAJ A ,EAJ_SALA B ");
		sql.append(" WHERE A.AHDM = B.AHDM ");
		sql.append("  AND A.FYDM = ? AND A.LARQ = ? and A.AJZT>='300' AND A.AJLXDM <2000");
		if(!UtilComm.isEmpty(WebContext.FILTER)){
			sql .append(" AND " +WebContext.FILTER);
		}
		
		PreparedStatement pst = null;
		ResultSet rs= null;
		int count = 0;
		logger.info("["+Thread.currentThread().getName()+"] 查询立案sql:"+sql.toString()+"\n参数："+fy.getFydm()+", "+rq);
		
		try {
			if(conn==null){
				logger.error("["+Thread.currentThread().getName()+"] 法院代码:"+fy.getFydm()+",代码："+fy.getDm()+",获取链接异常,.");
				return 0;
			}
			pst = conn.prepareStatement(sql.toString());
			pst.setString(1, fy.getFydm());
			pst.setString(2, rq);
			rs = pst.executeQuery();
			int sqlCount = 0;
			while(rs.next()) {
				sqlCount++;
				String ajlxbs = StringUtils.trim(WebContext.AjlxdmZh15Ajlxbs.get(rs.getString("AJLXBS")));
				if(CommUtil.isEmpty(ajlxbs)){
					continue;
				}
				//---开始针对必填项 异常数据跳过操作
				if(CommUtil.isEmpty(getCodeValue(CommUtil.trim(rs.getString("AJLY")),false))){
					logger.error("["+Thread.currentThread().getName()+"] 立案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",AJLY=>"+getCodeValue(CommUtil.trim(rs.getString("AJLY")),false)+","+fy);
					continue;
				}

				//立案案由要判断
				// 刑事案件，代码参见FYB/T 51203.1
				//民事案件，代码参见FYB/T 51203.2
				//国家赔偿案件中司法赔偿案件，代码参见FYB/T 51203.4
				//执行案件，代码参见FYB/T 51203.3
				StringBuffer laays = new StringBuffer();
				String saay = CommUtil.trim(rs.getString("SAAY"));
				String source_laays =  getCodeValue(saay,true);
				String [] arr_laays = source_laays.split("\n");
				for (int i = 0; i < arr_laays.length; i++) {
					String temp = WebContext.getAyzh(arr_laays[i]);
					if(StringUtils.isNotBlank(temp)){
						laays.append(temp).append("\n");
					}
				}

				//判断立案案由
				if(ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxList.contains(ajlxbs) || ajlxbs.startsWith("10")){
					if(StringUtils.isEmpty(laays.toString())){
						logger.error("["+Thread.currentThread().getName()+"] 立案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",SAAY=>"+rs.getString("SAAY")+","+fy);
						continue;
					}
				}

				//行政案件才判断
				if(ajlxbs.startsWith("04")||ajlxbsList.contains(ajlxbs)){
					if(CommUtil.isEmpty(getCodeValue(CommUtil.trim(rs.getString("XZGLFW")),false))){
						logger.error("["+Thread.currentThread().getName()+"] 立案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",XZGLFW=>"+getCodeValue(CommUtil.trim(rs.getString("XZGLFW")),false)+","+fy);
						continue;
					}
					if(xzxwFlag){
						if(CommUtil.isEmpty( getCodeValue(StringUtils.trimToEmpty(CommUtil.trim(rs.getString("XZXWZL"))),true))){
							logger.error("["+Thread.currentThread().getName()+"] 立案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",XZXWZL=>"+StringUtils.trimToEmpty(rs.getString("XZXWZL"))+","+fy);
							continue;
						}
					}
				}

				count++;
				Element element = root.addElement("R");
				String dm4 = CommUtil.long4Dm(fy.getDm());
				String nd = StringUtils.trimToEmpty(rs.getString("ND"));
				String sourceAhdm  =  StringUtils.trimToEmpty(rs.getString("AHDM"));
				String ahdm18 = ahdmConvertAhdm18(dm4, nd, ajlxbs, sourceAhdm);
				element.addElement("ajbs").setText(ahdm18);
				element.addElement("ajlx").setText(ajlxbs);
				element.addElement("ah").setText( StringUtils.trimToEmpty(rs.getString("AH")));
				element.addElement("jbfy").setText(dm4);
				/*if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("SARQ")))){
					element.addElement("sarq").setText(CommUtil.convertRq8(rs.getString("SARQ")));
				}else{
					element.addElement("sarq").setText(CommUtil.convertRq8(rs.getString("LARQ")));
				}*/
				/*if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("JARQ")))){
					element.addElement("bydjlarq").setText(CommUtil.convertRq8(rs.getString("JARQ")));
					element.addElement("byslcdrq").setText(CommUtil.convertRq8(rs.getString("JARQ")));
				}*/
				if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("LARQ")))){
					element.addElement("sarq").setText(CommUtil.convertRq8(rs.getString("LARQ")));
					element.addElement("djlarq").setText(CommUtil.convertRq8(rs.getString("LARQ")));
				}


				if(StringUtils.isNotBlank(laays.toString())){
					element.addElement("laay").setText(laays.toString());//cdx
				}
				element.addElement("saly").setText(getCodeValue(CommUtil.trim(rs.getString("AJLY")),false));
				if(CommUtil.isNotEmpty(getCodeValue(CommUtil.trim(rs.getString("XZGLFW")),false))){
					element.addElement("laayztlx").setText(getCodeValue(CommUtil.trim(rs.getString("XZGLFW")),false));
				}
				String xzxwzl  = "";
				if(xzxwFlag){
					 xzxwzl = rs.getString("XZXWZL");
				}
				if(StringUtils.isNotBlank(xzxwzl)){
					element.addElement("laayxzxwzl").setText( getCodeValue(StringUtils.trimToEmpty(xzxwzl),true));//cdx
				}
				LogService15.insLaLog(rq,fy.getDm(),ajlxbs,ahdm18,rs.getString("AH"),rs.getString("LARQ"),rs.getString("JARQ"),rs.getString("JARQ"), rs.getString("LARQ"),
						laays.toString(), getCodeValue(CommUtil.trim(rs.getString("AJLY")),false),
						getCodeValue(CommUtil.trim(rs.getString("XZGLFW")),false),xzxwzl,fy.getFydm());
			}
			logger.info("["+Thread.currentThread().getName()+"]sql:"+sql.toString()+" 库中数量:"+sqlCount+",实际xml中数量:"+count);
		} catch (SQLException e) {
			logger.error("["+Thread.currentThread().getName()+"] 查询立案异常。。",e);
		}finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(pst);
			DBHelper.closeConn(conn);
		}
		return count;
	}
	private boolean hasTab(Connection conn, String tabname) {
		PreparedStatement pst = null;
		ResultSet rs= null;
		try {
			String checkSql = "select count(*) from sysobjects where name = '"+tabname+"'";
			pst = conn.prepareStatement(checkSql);
			rs = pst.executeQuery();
			int num = 0;
			if(rs.next()){
				 num = rs.getInt(1);
			}
			if(num > 0){
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(pst);
		}
		return false;
	}


	private int loadJaElement(Element root,String rq,Fy fy){
		StringBuffer sql = new StringBuffer();
		Connection conn = WebContext.getFyEcourtConn(fy.getFydm());
		boolean xzxwFlag = false;
		sql.append("SELECT ");
		sql.append(" A.AHDM,A.ND, A.AJLXDM AS AJLXBS,A.AH,A.SARQ,A.JARQ,A.LARQ,A.SAAY,");//EAJ
		sql.append(" C.JAFSN  AS JAFS ,");
		sql.append(" B.AJLYN  AS  AJLY,");
		sql.append(" B.XZGLFW,");//EAJ_SALA
		sql.append(" C.AY");//EAJ_SJQK
		//判断是否有EDSR_XZXW这张表
		if(hasTab(conn,"EDSR_XZXW")){
			xzxwFlag = true;
			sql.append(" ,(SELECT MIN(XZXWZL) FROM EDSR_XZXW WHERE EDSR_XZXW.AHDM =A.AHDM  ) AS XZXWZL");
		}
		sql.append(" FROM EAJ A, EAJ_SALA B,EAJ_SJQK C");
		sql.append(" WHERE A.AHDM = B.AHDM AND A.AHDM  = C.AHDM");
		sql.append(" AND A.FYDM = ? AND A.JARQ = ?  AND A.AJZT>='800'  AND A.AJLXDM <2000");
		if(!UtilComm.isEmpty(WebContext.FILTER)){
			sql .append(" AND " +WebContext.FILTER);
		}
		PreparedStatement pst = null;
		ResultSet rs= null;
		int count = 0;		
		logger.info("查询结案sql："+sql.toString() +"\n参数："+fy.getFydm()+","+rq);
		try {
			if(conn==null){
				logger.error("["+Thread.currentThread().getName()+"] 法院代码:"+fy.getFydm()+",代码："+fy.getDm()+",获取链接异常,.");
				return 0;
			}
			pst = conn.prepareStatement(sql.toString());
			pst.setString(1, fy.getFydm());
			pst.setString(2, rq);
			rs = pst.executeQuery();
			int sqlCount = 0;
			while(rs.next()) {
				sqlCount++;
				String ajlxbs = StringUtils.trim(WebContext.AjlxdmZh15Ajlxbs.get(rs.getString("AJLXBS")));
				if(CommUtil.isEmpty(ajlxbs)){
					continue;
				}
				String ajly = CommUtil.trim(rs.getString("AJLY"));
				//---开始针对必填项 异常数据跳过操作
				if(CommUtil.isEmpty(getCodeValue(ajly,false))){
					logger.error("结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",AJLY=>"+ajly+","+fy);
				continue;
				}
				String jarq = CommUtil.trim(rs.getString("JARQ"));
				if(CommUtil.isEmpty(CommUtil.convertRq8(jarq))){
					logger.error("结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",JARQ=>"+jarq+","+fy);
					continue;
				}
				String jafs = CommUtil.trim(rs.getString("JAFS"));
				if(CommUtil.isEmpty(getCodeValue(jafs,false))){
					logger.error("结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",JAFS=>"+jafs+","+fy);
					continue;
				}

				//行政案件才判断
				String xzglfw = CommUtil.trim(rs.getString("XZGLFW"));
				String xzxwzl = "";
				if(xzxwFlag){
					xzxwzl = CommUtil.trim(rs.getString("XZXWZL"));
				}
				if(ajlxbs.startsWith("04")||ajlxbsList.contains(ajlxbs)){
					//===========
					if(CommUtil.isEmpty(CommUtil.trim(getCodeValue(xzglfw,false)))){
						logger.error("结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",XZGLFW=>"+xzglfw+","+fy);
					continue;
					}
					if(CommUtil.isEmpty( CommUtil.trim(getCodeValue(StringUtils.trimToEmpty(xzxwzl),true))) && xzxwFlag){
						logger.error("结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",XZXWZL=>"+StringUtils.trimToEmpty(xzxwzl)+","+fy);
						continue;
					}
				}

				String saay = CommUtil.trim(rs.getString("SAAY"));
				String source_laays =  getCodeValue(saay,true);
				String [] arr_laays = source_laays.split("\n");
				StringBuffer laays = new StringBuffer();
				for (int i = 0; i < arr_laays.length; i++) {
					String temp = WebContext.getAyzh(arr_laays[i]);
					if(StringUtils.isNotBlank(temp)){
						laays.append(temp).append("\n");
					}
				}
				//立案案由判断
				if(ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxList.contains(ajlxbs) || ajlxbs.startsWith("10")){
					if(StringUtils.isEmpty(laays.toString())){
						logger.error("["+Thread.currentThread().getName()+"] 结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",SAAY=>"+rs.getString("SAAY")+","+fy);
						continue;
					}
				}
				String ay = rs.getString("AY");
				//结案案由过滤
				if(ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxList.contains(ajlxbs) || ajlxbs.startsWith("10")){
					if(StringUtils.isEmpty(ay)){
						logger.error("["+Thread.currentThread().getName()+"] 结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",AY=>"+rs.getString("AY")+","+fy);
						continue;
					}

				}

				count++;
				Element element = root.addElement("R");
				String dm4 = CommUtil.long4Dm(fy.getDm());
				String nd =  StringUtils.trimToEmpty(rs.getString("ND"));
				String sourceAhdm  =  StringUtils.trimToEmpty(rs.getString("AHDM"));
				String ahdm18 = ahdmConvertAhdm18(dm4, nd, ajlxbs, sourceAhdm);
				element.addElement("ajbs").setText(ahdm18);
				element.addElement("ajlx").setText(ajlxbs);
				element.addElement("ah").setText( StringUtils.trimToEmpty(rs.getString("AH")));
				element.addElement("jbfy").setText(dm4);
				/*if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("SARQ")))){
					element.addElement("sarq").setText(CommUtil.convertRq8(rs.getString("SARQ")));
				}else{
					element.addElement("sarq").setText(CommUtil.convertRq8(rs.getString("LARQ")));
				}*/
				jarq = CommUtil.convertRq8(jarq);
				String larq = rs.getString("LARQ");
				if(CommUtil.isNotEmpty(CommUtil.convertRq8(larq))){
					element.addElement("sarq").setText(CommUtil.convertRq8(rs.getString("LARQ")));
				}
				//不予登记  不予受理 新判断 根据JAFS去TS_BZDM_15找到对应MC，看MC是否含有相关字符
				if(check(jafs,"不予登记")){
					if(CommUtil.isNotEmpty(jarq)){
						element.addElement("bydjlarq").setText(jarq);
					}
				}
				if(check(jafs,"不予受理")){
					if(CommUtil.isNotEmpty(jarq)){
						element.addElement("byslcdrq").setText(jarq);
					}
				}
				if(CommUtil.isNotEmpty(CommUtil.convertRq8(larq))){
					element.addElement("djlarq").setText(CommUtil.convertRq8(larq));
				}

				if(StringUtils.isNotBlank(laays.toString())){
					element.addElement("laay").setText(laays.toString());//cdx
				}
				element.addElement("saly").setText(getCodeValue(ajly,false));
				if(CommUtil.isNotEmpty(getCodeValue(xzglfw,false))){
					element.addElement("laayztlx").setText(getCodeValue(xzglfw,false));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(xzxwzl))){
					element.addElement("laayxzxwzl").setText( getCodeValue(StringUtils.trimToEmpty(xzxwzl),true));//cdx
				}
				element.addElement("jarq").setText(jarq);
				if(CommUtil.isNotEmpty( StringUtils.trimToEmpty(ay))){
					element.addElement("jaay").setText(getCodeValue(StringUtils.trimToEmpty(ay),true));//cdx
				}
				element.addElement("jafs").setText(getCodeValue(jafs,false));
				LogService15.insJaLog(rq,fy.getDm(),ajlxbs,ahdm18,rs.getString("AH"),rs.getString("LARQ"),rs.getString("JARQ"),rs.getString("JARQ"),
						rs.getString("LARQ"),laays.toString(),getCodeValue(ajly,false),
						getCodeValue(xzglfw,false),getCodeValue(StringUtils.trimToEmpty(xzxwzl),true),
						rs.getString("JARQ"),getCodeValue(StringUtils.trimToEmpty(ay),true),fy.getFydm(),getCodeValue(jafs,false));
			}
			logger.info("结案sql："+sql.toString() +",库中实际数量:"+sqlCount+",实际xml中数量:"+count);
		} catch (SQLException e) {
			logger.error("查询结案异常。。",e);
		}finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(pst);
			DBHelper.closeConn(conn);
		}
		return count;
	}
	
	private boolean check(String jafs, String val) {
		if(jafs == null || jafs.equals("")){
			return false;
		}
		String mc = WebContext.codeMcMap.get(jafs);
		if(mc != null && !mc.equals("")){
			if(mc.contains(val)){
				return true;
			}
		}
		return false;
	}
	/**
	 * 处理 ,09_xxxx-y, 或者 ,15_xxxx-y, 或 xx_xxxx-y
	 * @param code 代码值
	 * @param isfx 是否复选  复选时,如果 库内置为   xx_xxxx-a,xx_xxxx-b,xx_xxxx-c 则返回 a\nb\nc
	 * @return 返回y
	 */
	private String getCodeValue(String code,boolean isfx){
		String newCode =code; 
		StringBuffer code_cdx  = new StringBuffer();
		if(StringUtils.isNotEmpty(code)){
			if(code.indexOf(",")!=-1){
				String[] _temp = code.split(",");
				for (int i = 0; i < _temp.length; i++) {
					if(StringUtils.isNotEmpty(StringUtils.trim(_temp[i]))){
						if(isfx){
							code_cdx.append(getValidValue(_temp[i])).append("\n");
						}else{
							newCode = _temp[i];
							break; 
						}
					}
				}
				if(isfx){
					newCode = code_cdx.toString();
				}
			}
		}
		newCode = getValidValue(newCode);
		return StringUtils.isNotEmpty(newCode)?newCode:StringUtils.EMPTY;
	}
	/**
	 * 针对 xx_xxxx-y 类型的值，取 y值
	 * @param code 原值
	 * @return y值
	 */
	private String getValidValue(String code){
		if(code.startsWith("09_") || code.startsWith("15_")){
			String [] _temp = code.split("-");
			if(_temp.length==2){
				return _temp[1];
			}
		}
		return code;
	}
	private String ahdmConvertAhdm18(String dm,String nd,String ajbm,String ahdm){
		//getDm()+nd+getAjbm()+getAhdm().substring(getAhdm().length()-6)
		//TS_FYMC.DM+EAJ.ND+TS_AJCXBM2015.AJLXBS+AHDM
//		if(ahdm.length()>18){
//			return ahdm;
//		}
//		return dm +nd+ajbm+ahdm.substring(ahdm.length()-6);//15转16
		if (ahdm.length() > 18) {
			return ahdm.substring(1);
		} else if (ahdm.length() < 18) {
			return dm + nd + ajbm + ahdm.substring(ahdm.length() - 6);// 15转16
		} else {
			return ahdm;
		}
	}
	
	
}
