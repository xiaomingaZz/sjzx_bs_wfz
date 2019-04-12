package tdh.xsyj.executeHB09Z15;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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

public class CourtThreadHB09Z15  implements Callable<Map<String,Integer>>{
	private static Log logger = LogFactory.getLog(CourtThreadHB09Z15.class);
	private Fy fy;
	private int id ;
	private String dateTime;
	private Element root;
	
	public CourtThreadHB09Z15(Fy fy,String dateTime, int id,Element root ) {
		super();
		this.fy = fy;
		this.dateTime = dateTime;
		this.id = id;
		this.root = root;
	}
	
	public String AjlxZh(String ajlx,String ajzlx,String ahdm,String dz,String sqsx,String saay,String bqlx,String bhlx){
		StringBuffer sql = new StringBuffer();
		String ajlxbs="",sj="";
		sql.append("select distinct(FL) FL from T_AJLX_DMZH where AJLX=?");
		Connection conn = DBHelper.getConn("export");
		
		PreparedStatement pst = null;
		ResultSet rs= null;
		try {
			if(conn==null){
				logger.error("["+Thread.currentThread().getName()+"] 法院代码:"+fy.getFydm()+",代码："+fy.getDm()+",获取链接异常,.");
				return "";
			}
			pst = conn.prepareStatement(sql.toString());
			pst.setString(1, ajlx);
			rs = pst.executeQuery();
			
			while(rs.next()) {
				Map<String,String> lxzh=WebContext.AjlxZhMap.get(rs.getString("FL"));
				if("1".equals(rs.getString("FL"))){
					ajlxbs=lxzh.get(ajlx);
				}else if("2".equals(rs.getString("FL"))){
					sj=ajlx+ajzlx;
					ajlxbs=lxzh.get(sj);
				}else if("3".equals(rs.getString("FL"))){
					sj=ajlx+dz;
					ajlxbs=lxzh.get(sj);
				}else if("4".equals(rs.getString("FL"))){
					sj=ajlx+ajzlx+bqlx;
					ajlxbs=lxzh.get(sj);
				}else if("5".equals(rs.getString("FL"))){
					sj=ajlx+sqsx;
					ajlxbs=lxzh.get(sj);
				}else if("6".equals(rs.getString("FL"))){
					sj=ajlx+ajzlx+sqsx;
					ajlxbs=lxzh.get(sj);
				}else if("7".equals(rs.getString("FL"))){
					sj=ajlx+bhlx;
					ajlxbs=lxzh.get(sj);
				}else if("8".equals(rs.getString("FL"))){
					sj=ajlx+dz+saay;
					ajlxbs=lxzh.get(sj);
				}
			}
		}catch (SQLException e) {
			e.printStackTrace();
		} finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(pst);
			DBHelper.closeConn(conn);
		}
		return ajlxbs;
	}

    
	@Override
	public Map<String,Integer> call() throws Exception {
		Thread.currentThread().setName("法院代码:"+fy.getFydm());
		/*Document  doc = DocumentHelper.createDocument();
		doc.setXMLEncoding("UTF-8");*/
		logger.info("["+Thread.currentThread().getName()+"] 开始生成有效数据..");
		Element element = root.addElement("R");
		element.addElement("id").setText(CommUtil.long4Dm(fy.getDm()));
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
		String dm=fy.getFydm().substring(0,2);
		sql.append("SELECT ");
		sql.append(" A.AHDM,A.ND,A.AJLX,A.DZ,A.SQSX,A.AH,A.SARQ,A.JARQ,A.LARQ,A.SAAY,");
		sql.append("(select BQLX from EAJ_MTBQ where EAJ_MTBQ.AHDM=A.AHDM) AS BQLX,");
		sql.append("(select EAJ_SALA.BHLX from EAJ_SALA where A.AHDM=EAJ_SALA.AHDM) AS BHLX,");
		sql.append(" B.AJLYN  AS  AJLY,B.XZGLFW,(select EAJ_SALA.AJZLX from EAJ_SALA where A.AHDM=EAJ_SALA.AHDM) AS AJZLX,");
		sql.append(" (SELECT MIN(XZXWZL) FROM EDSR_XZXW WHERE EDSR_XZXW.AHDM =A.AHDM  ) AS XZXWZL");
		sql.append(" FROM EAJ A ,EAJ_SALA B ");
		sql.append(" WHERE A.AHDM = B.AHDM ");
		sql.append("  AND A.FYDM = ? AND A.LARQ = ? and A.AJZT>='300'");
		Connection conn = WebContext.getFyEcourtConn(fy.getFydm());
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
				String ajlxbs = StringUtils.trim(AjlxZh(rs.getString("AJLX"),rs.getString("AJZLX"),rs.getString("AHDM"),rs.getString("DZ"),rs.getString("SQSX"),rs.getString("SAAY"),rs.getString("BQLX"),rs.getString("BHLX")));
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
				String saay = rs.getString("SAAY");
				String source_laays =  getCodeValue(saay,true);
				String [] arr_laays = source_laays.split("\n");
				for (int i = 0; i < arr_laays.length; i++) {
					String temp = WebContext.getAyzh(arr_laays[i]);
					if(StringUtils.isNotBlank(temp)){
						laays.append(temp).append("\n");
					}
				}

				//判断立案案由
				if(ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxbs.startsWith("05") || ajlxbs.startsWith("10")){
					if(StringUtils.isEmpty(laays.toString())){
						logger.error("["+Thread.currentThread().getName()+"] 立案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",SAAY=>"+rs.getString("SAAY")+","+fy);
						continue;
					}
				}
				
				//行政案件才判断
				if(ajlxbs.startsWith("04")){
					if(CommUtil.isEmpty(getCodeValue(CommUtil.trim(rs.getString("XZGLFW")),false))){
						logger.error("["+Thread.currentThread().getName()+"] 立案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",XZGLFW=>"+getCodeValue(CommUtil.trim(rs.getString("XZGLFW")),false)+","+fy);
						continue;
					}
					if(CommUtil.isEmpty(StringUtils.trimToEmpty(rs.getString("XZXWZL")))){
						logger.error("["+Thread.currentThread().getName()+"] 立案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",XZXWZL=>"+StringUtils.trimToEmpty(rs.getString("XZXWZL"))+","+fy);
						continue;
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
				String sarq="";
				if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("SARQ")))){
					sarq=rs.getString("SARQ");
					element.addElement("sarq").setText(CommUtil.convertRq8(rs.getString("SARQ")));
				}else{
					sarq=rs.getString("LARQ");
					element.addElement("sarq").setText(CommUtil.convertRq8(rs.getString("LARQ")));
				}
				if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("JARQ")))){
					element.addElement("bydjlarq").setText(CommUtil.convertRq8(rs.getString("JARQ")));
					element.addElement("byslcdrq").setText(CommUtil.convertRq8(rs.getString("JARQ")));
				}
				if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("LARQ")))){
					element.addElement("djlarq").setText(CommUtil.convertRq8(rs.getString("LARQ")));
				}
				if(StringUtils.isNotBlank(laays.toString())){
					element.addElement("laay").setText(laays.toString());//cdx
				}
				element.addElement("saly").setText(getCodeValue(rs.getString("AJLY"),false));
				if(CommUtil.isNotEmpty(getCodeValue(rs.getString("XZGLFW"),false))){
					element.addElement("laayztlx").setText(getCodeValue(rs.getString("XZGLFW"),false));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(rs.getString("XZXWZL")))){
					element.addElement("laayxzxwzl").setText( StringUtils.trimToEmpty(rs.getString("XZXWZL")));//cdx
				}
				LogService15.insLaLog(rq,fy.getDm(),ajlxbs,ahdm18,rs.getString("AH"),sarq,rs.getString("JARQ"),rs.getString("JARQ"), rs.getString("LARQ"),
						laays.toString(), getCodeValue(CommUtil.trim(rs.getString("AJLY")),false),
						getCodeValue(CommUtil.trim(rs.getString("XZGLFW")),false),rs.getString("XZXWZL"),fy.getFydm());
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
	
	private int loadJaElement(Element root,String rq,Fy fy){
		StringBuffer sql = new StringBuffer();
		String dm=fy.getFydm().substring(0,2);
		sql.append("SELECT ");
		sql.append(" A.AHDM,A.ND,A.AJLX,A.DZ,A.SQSX,A.AH,A.SARQ,A.JARQ,A.LARQ,A.SAAY,");//EAJ
		sql.append("(select BQLX from EAJ_MTBQ where EAJ_MTBQ.AHDM=A.AHDM) AS BQLX,");
		sql.append("(select EAJ_SALA.BHLX from EAJ_SALA where A.AHDM=EAJ_SALA.AHDM) AS BHLX,");
		sql.append(" C.JAFSN  AS JAFS ,(select EAJ_SALA.AJZLX from EAJ_SALA where A.AHDM=EAJ_SALA.AHDM) AS AJZLX,");
		sql.append(" B.AJLYN  AS  AJLY,");
		sql.append(" B.XZGLFW,");//EAJ_SALA
		sql.append(" C.AY,");//EAJ_SJQK
		sql.append("(SELECT MIN(XZXWZL) FROM EDSR_XZXW WHERE EDSR_XZXW.AHDM =A.AHDM  ) AS XZXWZL  ");
		sql.append(" FROM EAJ A, EAJ_SALA B,EAJ_SJQK C");
		sql.append(" WHERE A.AHDM = B.AHDM AND A.AHDM  = C.AHDM");
		sql.append(" AND A.FYDM = ? AND A.JARQ = ?  AND A.AJZT>='800'");
		Connection conn = WebContext.getFyEcourtConn(fy.getFydm());
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
				String ajlxbs = StringUtils.trim(AjlxZh(rs.getString("AJLX"),rs.getString("AJZLX"),rs.getString("AHDM"),rs.getString("DZ"),rs.getString("SQSX"),rs.getString("SAAY"),rs.getString("BQLX"),rs.getString("BHLX")));
				if(CommUtil.isEmpty(ajlxbs)){
					continue;
				}
				//---开始针对必填项 异常数据跳过操作
				if(CommUtil.isEmpty(getCodeValue(CommUtil.trim(rs.getString("AJLY")),false))){
					logger.error("结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",AJLY=>"+getCodeValue(CommUtil.trim(rs.getString("AJLY")),false)+","+fy);
					continue;
				}
				if(CommUtil.isEmpty(CommUtil.convertRq8(CommUtil.trim(rs.getString("JARQ"))))){
					logger.error("结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",JARQ=>"+getCodeValue(CommUtil.trim(rs.getString("JARQ")),false)+","+fy);
					continue;
				}
				if(CommUtil.isEmpty(getCodeValue(CommUtil.trim(rs.getString("JAFS")),false))){
					logger.error("结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",JAFS=>"+getCodeValue(CommUtil.trim(rs.getString("JAFS")),false)+","+fy);
					continue;
				}
				
				//行政案件才判断
				String xzglfw = CommUtil.trim(rs.getString("XZGLFW"));
				String xzxwzl = CommUtil.trim(rs.getString("XZXWZL"));
				if(ajlxbs.startsWith("04")){
					//===========
					if(CommUtil.isEmpty(getCodeValue(xzglfw,false))){
						logger.error("结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",XZGLFW=>"+xzglfw+","+fy);
						continue;
					}
					if(CommUtil.isEmpty( getCodeValue(StringUtils.trimToEmpty(xzxwzl),true))){
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
				if(ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxbs.startsWith("05") || ajlxbs.startsWith("10")){
					if(StringUtils.isEmpty(laays.toString())){
						logger.error("["+Thread.currentThread().getName()+"] 结案异常数据过滤：AHDM=>"+rs.getString("AHDM")+",SAAY=>"+rs.getString("SAAY")+","+fy);
						continue;
					}
				}
				String ay = rs.getString("AY");
				//结案案由过滤
				if(ajlxbs.startsWith("02") || ajlxbs.startsWith("03") || ajlxbs.startsWith("05") || ajlxbs.startsWith("10")){
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
				if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("SARQ")))){
					element.addElement("sarq").setText(CommUtil.convertRq8(rs.getString("SARQ")));
				}else{
					element.addElement("sarq").setText(CommUtil.convertRq8(rs.getString("LARQ")));
				}
				String jarq = CommUtil.convertRq8(rs.getString("JARQ"));
				if(CommUtil.isNotEmpty(jarq)){
					element.addElement("bydjlarq").setText(jarq);
					element.addElement("byslcdrq").setText(jarq);
				}
				if(CommUtil.isNotEmpty(CommUtil.convertRq8(rs.getString("LARQ")))){
					element.addElement("djlarq").setText(CommUtil.convertRq8(rs.getString("LARQ")));
				}
				if(StringUtils.isNotBlank(laays.toString())){
					element.addElement("laay").setText(laays.toString());//cdx
				}
				element.addElement("saly").setText(getCodeValue(CommUtil.trim(rs.getString("AJLY")),false));
				if(CommUtil.isNotEmpty(getCodeValue(xzglfw,false))){
					element.addElement("laayztlx").setText(getCodeValue(xzglfw,false));
				}
				if(CommUtil.isNotEmpty(StringUtils.trimToEmpty(xzxwzl))){
					element.addElement("laayxzxwzl").setText( getCodeValue(StringUtils.trimToEmpty(xzxwzl),true));//cdx
				}
				element.addElement("jarq").setText(CommUtil.convertRq8(rs.getString("JARQ")));
				if(CommUtil.isNotEmpty( StringUtils.trimToEmpty(rs.getString("AY")))){
					element.addElement("jaay").setText( StringUtils.trimToEmpty(rs.getString("AY")));//cdx
				}
				element.addElement("jafs").setText(getCodeValue(CommUtil.trim(rs.getString("JAFS")),false));
				LogService15.insJaLog(rq,fy.getDm(),ajlxbs,ahdm18,rs.getString("AH"),rs.getString("SARQ"),rs.getString("JARQ"),rs.getString("JARQ"),
						rs.getString("LARQ"),laays.toString(),getCodeValue(CommUtil.trim(rs.getString("AJLY")),false),
						getCodeValue(CommUtil.trim(rs.getString("XZGLFW")),false),StringUtils.trimToEmpty(rs.getString("XZXWZL")),
						rs.getString("JARQ"),StringUtils.trimToEmpty(rs.getString("AY")),fy.getFydm(),getCodeValue(CommUtil.trim(rs.getString("JAFS")),false));
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
			System.out.println(_temp[0]+"-"+_temp[1]);
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
