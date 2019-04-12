package tdh.xsyj;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tdh.bean.Fy;
import tdh.db.DBHelper;
import tdh.frame.common.UtilComm;
import tdh.framework.util.StringUtils;
import tdh.util.CalendarUtil;
import tdh.util.CommUtil;
import tdh.web.WebContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ThreadNew implements Callable<String> {
	
	private static Log log = LogFactory.getLog(ThreadNew.class);
	
	private int id;
	
	private String dateTime;
	
	private Fy fy;
	
	private Map<String,String> xtajlxXmlMap = new HashMap<String, String>();
	
	public void setTaskId(int id) {
		this.id = id;
	}
	
	public ThreadNew(int id,String dateTime, Fy fy){
		this.id = id;
		this.fy = fy;
		this.dateTime = dateTime;
	}
	
	public String call(){
		StringBuffer dataXml = new StringBuffer();
		String rq = CalendarUtil.getGsSj(dateTime, "yyyyMMdd");
		Connection fyConn = null;
		try{
			dataXml.append("<data>\n");
			dataXml.append("<id>").append(fy.getDm()).append("</id>\n");
			log.info("处理[线程号："+id+"\t法院："+fy.getFydm()+","+fy.getFymc()+"]的数据...");
			fyConn = WebContext.getFyEcourtConn(fy.getFydm());
			
			if(CommUtil.isNull(fyConn)){
				log.error("未在TS_DB中配置【"+fy.getFydm()+"】的数据库信息.");
				log.error("【"+fy.getFydm()+"】的新收已结数据未能生成！");
				if(WebContext.TYPE.equals("ZX")){
					LogService.insLogZxErr(fy.getFydm(), "1", "获取不到数据库连接");
				}else{
					LogService.insLogErr(fy.getFydm(), "1", "获取不到数据库连接");
				}
				log.info("数据库连接未能获取到......");
				return "error";
		}
			
			int laNum = 0;
			int jaNum = 0;
			log.info("开始执行查询"+fy.getFydm()+"收案结案数据......");
			Map<String,List<SaInfo>>  salistmap = getSaMapList(fy.getFydm(),rq,fyConn);
			log.info("结束执行查询"+fy.getFydm()+"收案数据......");
			Map<String,List<JaInfo>>  jalistmap = getJaList(fy.getFydm(),rq,fyConn);
			log.info("结束执行查询"+fy.getFydm()+"结案数据......");
			//立案
			for(String sbajlxdm : WebContext.SBAJLXDMLIST){
				
				StringBuffer sbajlxXml = new StringBuffer();
				List<SaInfo>  salist = salistmap.get(sbajlxdm);
				if(salist == null) {
					salist = new ArrayList<SaInfo>();
				}
				int xtajlxNum = 0;
				sbajlxXml.append("<lalist>\n");
				for(SaInfo bean : salist ) {
					xtajlxNum++;
					sbajlxXml.append("<laxx>\n");
					sbajlxXml.append("<ajbs>").append(bean.getAjbs()).append("</ajbs>\n");
					sbajlxXml.append("<ah>").append(bean.getAh()).append("</ah>\n");
					sbajlxXml.append("<sarq>").append(bean.getSarq()).append("</sarq>\n");
					sbajlxXml.append("<larq>").append(bean.getLarq()).append("</larq>\n");
					if(CommUtil.isNotEmpty(bean.getLaay())){
						sbajlxXml.append("<laay>").append(bean.getLaay()).append("</laay>\n");
					}
					if(CommUtil.isNotEmpty(bean.getQsbdje())){
						sbajlxXml.append("<qsbdje>").append(bean.getQsbdje()).append("</qsbdje>\n");
					}
					
					if(CommUtil.isNotEmpty(bean.getAjzlx())){
						sbajlxXml.append("<ajzlx>").append(bean.getAjzlx()).append("</ajzlx>\n");
					}
					if(CommUtil.isNotEmpty(bean.getTqxzpc())){
						sbajlxXml.append("<tqxzpc>").append(bean.getTqxzpc()).append("</tqxzpc>\n");
					}
					
					LogService.insLaLog(rq, fy.getDm(), bean.getAjbs(), bean.getAh(), sbajlxdm , bean.getSarq(),
							bean.getLarq(), bean.getLaay(), bean.getQsbdje() , bean.getAjzlx(), bean.getTqxzpc());
					
					sbajlxXml.append("</laxx>\n");
				}
				sbajlxXml.append("</lalist>\n");
				xtajlxXmlMap.put(sbajlxdm,"<ajlx>"+ sbajlxdm +"</ajlx>\n<sl>"+xtajlxNum+"</sl>\n"+sbajlxXml.toString());
				laNum = laNum + xtajlxNum;
			}
			
			dataXml.append("<drxs>\n");
			dataXml.append("<zs>").append(laNum).append("</zs>\n");
			
			JobExecuteNew.fyXmlMap.put(fy.getFydm()+"_DRXS", Integer.toString(laNum));
			
			for(String sbajlxdm  : WebContext.SBAJLXDMLIST){
				dataXml.append("<laxq>\n").append(xtajlxXmlMap.get(sbajlxdm)).append("</laxq>\n");
			}
			dataXml.append("</drxs>\n");
			//结案
			xtajlxXmlMap.clear();
			
			for(String sbajlxdm  :  WebContext.SBAJLXDMLIST ){
				StringBuffer xtajlxXml = new StringBuffer();
				int xtajlxNum = 0;
				xtajlxXml.append("<jalist>\n");
				
				List<JaInfo> list = jalistmap.get(sbajlxdm);
				if(list == null) {
					list = new ArrayList<JaInfo>();
				}
				for(JaInfo bean  : list) {
					xtajlxNum++;
					xtajlxXml.append("<jaxx>\n");
					xtajlxXml.append("<ajbs>").append(bean.getAjbs()).append("</ajbs>\n");
					xtajlxXml.append("<ah>").append(bean.getAh()).append("</ah>\n");
					xtajlxXml.append("<jarq>").append(bean.getJarq()).append("</jarq>\n");
					if(CommUtil.isNotEmpty(bean.getJaay())){
						xtajlxXml.append("<jaay>").append(bean.getJaay()).append("</jaay>\n");
					}
					if(CommUtil.isNotEmpty(bean.getJafs())){
						xtajlxXml.append("<jafs>").append(bean.getJafs()).append("</jafs>\n");
					}
					if(CommUtil.isNotEmpty(bean.getJabdje())){
							xtajlxXml.append("<jabdje>").append(bean.getJabdje()).append("</jabdje>\n");
					}
					
					if(CommUtil.isNotEmpty(bean.getLarq())){
						xtajlxXml.append("<larq>").append(bean.getLarq()).append("</larq>\n");
					}
				
					if(CommUtil.isNotEmpty(bean.getAjzlx())){
						xtajlxXml.append("<ajzlx>").append(bean.getAjzlx()).append("</ajzlx>\n");
					}
					
					if(CommUtil.isNotEmpty(bean.getTqxzpc())){
						xtajlxXml.append("<tqxzpc>").append(bean.getTqxzpc()).append("</tqxzpc>\n");
					}
					
					xtajlxXml.append("</jaxx>\n");
					
					LogService.insJaLog(rq, fy.getDm(), bean.getAjbs(), bean.getAh(), sbajlxdm , bean.getJarq(),
							bean.getJaay(), UtilComm.trim(String.valueOf(bean.getJabdje())),
							bean.getJafs(), bean.getLarq(), bean.getAjzlx(), bean.getTqxzpc());
				}
				xtajlxXml.append("</jalist>\n");
				xtajlxXmlMap.put(sbajlxdm,"<ajlx>"+ sbajlxdm +"</ajlx>\n<sl>"+xtajlxNum+"</sl>\n"+xtajlxXml.toString());
				jaNum = jaNum + xtajlxNum;
			}
			dataXml.append("<drja>\n");
			dataXml.append("<zs>").append(jaNum).append("</zs>\n");
			
			JobExecuteNew.fyXmlMap.put(fy.getFydm()+"_DRJA", Integer.toString(jaNum));
			
			for(String sbajlxdm  :  WebContext.SBAJLXDMLIST ){
				dataXml.append("<jaxq>\n").append(xtajlxXmlMap.get(sbajlxdm)).append("</jaxq>\n");
			}
			dataXml.append("</drja>\n");
			dataXml.append("</data>\n");
			
			JobExecuteNew.fyXmlMap.put(fy.getFydm(), dataXml.toString());
			
			//清除队列数据
			salistmap.clear();
			jalistmap.clear();
			
			if(WebContext.TYPE.equals("ZX")){
				LogService.insetZxLog(fy.getFydm(), dateTime, laNum, jaNum);
			}else{
				LogService.insetLog(fy.getFydm(), dateTime, laNum, jaNum);
			}
		}catch(Exception e){
			log.error("生成当日新收、结案异常......",e);
			if(WebContext.TYPE.equals("ZX")){
				LogService.insLogZxErr(fy.getFydm(), "2", e.toString());
			}else{
				LogService.insLogErr(fy.getFydm(), "2", e.toString());
			}
		}finally{
			DBHelper.closeConn(fyConn);
			log.info("线程号：["+id+"]\t"+fy.getFydm()+"--准备关闭连接......");
		}
		return "succ";
	}
	
	/**
	 * 获取当前法院的立案案件列表
	 * @param fydm
	 * @param conn
	 * @return
	 */
	public Map<String,List<SaInfo>> getSaMapList(String fydm,String _larq,Connection conn) {
		Map<String,List<SaInfo>> map = new HashMap<String,List<SaInfo>>();
		PreparedStatement pst = null;
		ResultSet rs= null;
		String sql = "SELECT A.AHDM,A.AJBS,A.AH,A.SARQ,A.LARQ, A.SAAY, A.BDJE,A.XTAJLX,A.AJLXDM ,"
				+ "(select AJZLX from EAJ_SALA WHERE EAJ_SALA.AHDM = A.AHDM) AS AJZLX ,"
				+ "(select TQXZPC from EAJ_SALA WHERE EAJ_SALA.AHDM = A.AHDM) AS TQXZPC "
				+ " FROM EAJ A "
				+ " WHERE A.FYDM = ? AND A.LARQ = ? and A.AJZT>='300' ";
		if(!UtilComm.isEmpty(WebContext.FILTER)){
			sql += " AND " +WebContext.FILTER;
		}
		try{
			log.debug(fy.getFydm() + "LA查询语句:" + sql);
			pst = conn.prepareStatement(sql);
			pst.setString(1, fydm);
			pst.setString(2,_larq);
			rs = pst.executeQuery();
			while(rs.next()) {
				String ajbs = rs.getString("AHDM");
				String ajlx = StringUtils.trim(rs.getObject("AJLXDM"));
				if(ajbs.length() <= 15){
					
				}else if(ajbs.length() == 18 || ajbs.length() == 19){
					if(WebContext.AHDM_FLAG){
						if("189".equals(ajlx) || "190".equals(ajlx)
								|| "191".equals(ajlx) || "193".equals(ajlx)
								|| "194".equals(ajlx) || "195".equals(ajlx)
								|| "196".equals(ajlx) || "197".equals(ajlx)){
							ajbs = CommUtil.convertLeng15Ahdm(ajbs);
						}else{
							ajbs = CommUtil.convertLeng15AhdmSp(ajbs);
						}

					}else{
						continue;
					}
				}else{
					log.error("异常的案件(长度异常)AHDM:"+ajbs);
					continue;
				}
				SaInfo bean = new SaInfo();
				
				String ah = CommUtil.trim(rs.getString("AH"));
				String sarq = CommUtil.convertRq8(rs.getString("SARQ"));
				String larq = CommUtil.convertRq8(rs.getString("LARQ"));
				if("".equals(sarq)){
					sarq = larq;
				}
				String laay = rs.getString("SAAY");
				if("0".equals(laay)) {
					laay = "";
				}
				double qsbd = rs.getDouble("BDJE");
				String ajzlx = CommUtil.trim(rs.getString("AJZLX"));
				ajzlx = WebContext.getBzdm(ajzlx); 
				ajzlx = CommUtil.removeCodePrefix(ajzlx); 
				
				String tqxzpc = CommUtil.trim(rs.getString("TQXZPC"));
				tqxzpc = WebContext.getBzdm(tqxzpc); 
				tqxzpc = CommUtil.removeCodePrefix(tqxzpc); 
				
				bean.setAjbs(ajbs);
				bean.setAh(ah);
				bean.setSarq(sarq);
				bean.setLarq(larq);
				bean.setLaay(laay);
				bean.setAjzlx(ajzlx);
				bean.setTqxzpc(tqxzpc);
			
				java.text.NumberFormat nf = java.text.NumberFormat.getInstance();   
				nf.setGroupingUsed(false);  
				bean.setQsbdje(nf.format(qsbd));
				
				bean.setXtajlx(CommUtil.trim(rs.getString("XTAJLX")));
				String ajlxdm = CommUtil.trim(String.valueOf(rs.getInt("AJLXDM")));
				if("0".equals(ajlxdm)){
					ajlxdm = "";
				}
				bean.setAjlxdm(ajlxdm);
			
				String sbajlx = getSbAjlx(bean.getAjlxdm(),bean.getXtajlx());
				if(UtilComm.isEmpty(sbajlx)) {
					log.info("无法匹配具体上报案件类型:"+bean.getAjbs()+"["+bean.getAh()+"]");
				}else{
					List<SaInfo> list = map.get(sbajlx);
					if(list == null) {
						list = new ArrayList<SaInfo>();
						map.put(sbajlx, list);
					}
					list.add(bean);
				}
			}
		}catch(Exception e){
			log.error("getSaList:" + sql,e);
		}finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(pst);
		}
		return map;
	}
	
	
	/**
	 * 获取当前法院的结案案件列表
	 * @param fydm
	 * @param conn
	 * @return
	 */
	public Map<String,List<JaInfo>> getJaList(String fydm,String _jarq,Connection conn) {
		Map<String,List<JaInfo>> map = new HashMap<String,List<JaInfo>>();
		PreparedStatement pst = null;
		ResultSet rs= null;
		String sql = "SELECT A.AHDM,A.AJBS,A.AH,A.LARQ,A.JARQ,B.AY,A.JAFS,B.JABDJE ,A.XTAJLX,A.AJLXDM,"
				+ "(select AJZLX from EAJ_SALA WHERE EAJ_SALA.AHDM = A.AHDM) AS AJZLX ,"
				+ "(select TQXZPC from EAJ_SALA WHERE EAJ_SALA.AHDM = A.AHDM) AS TQXZPC "
				+ " FROM EAJ A,EAJ_SJQK B "
				+ " WHERE A.FYDM = ? AND A.JARQ = ? "
				+ " AND A.AHDM = B.AHDM AND A.AJZT>='800' ";
		
		if(!UtilComm.isEmpty(WebContext.FILTER)){
			sql += " AND " +WebContext.FILTER;
		}
		log.debug(fy.getFydm() + " JA查询语句:" + sql);
		try{
			pst = conn.prepareStatement(sql);
			pst.setString(1, fydm);
			pst.setString(2, _jarq);
			rs = pst.executeQuery();
			while(rs.next()) {
				String ajbs = rs.getString("AHDM");
				String ajlx = StringUtils.trim(rs.getObject("AJLXDM"));
				if(ajbs.length() <= 15){
					
				}else if(ajbs.length() == 18 || ajbs.length() == 19){
					if(WebContext.AHDM_FLAG){
						if("189".equals(ajlx) || "190".equals(ajlx)
								|| "191".equals(ajlx) || "193".equals(ajlx)
								|| "194".equals(ajlx) || "195".equals(ajlx)
								|| "196".equals(ajlx) || "197".equals(ajlx)){
							ajbs = CommUtil.convertLeng15Ahdm(ajbs);
						}else{
							ajbs = CommUtil.convertLeng15AhdmSp(ajbs);
						}
					}else{
						continue;
					}
				}else{
					log.error("异常的案件(长度异常)AHDM:"+ajbs);
					continue;
				}
				JaInfo  bean = new JaInfo();
				
				String ah = CommUtil.trim(rs.getString("AH"));
				String jarq = CommUtil.convertRq8(rs.getString("JARQ"));
				String larq = CommUtil.convertRq8(rs.getString("LARQ"));
				String jafs = CommUtil.trim(rs.getString("JAFS"));
				jafs = WebContext.getBzdm(jafs); 
				jafs = CommUtil.removeCodePrefix(jafs); 
				
				String ajzlx = CommUtil.trim(rs.getString("AJZLX"));
				ajzlx = WebContext.getBzdm(ajzlx); 
				ajzlx = CommUtil.removeCodePrefix(ajzlx); 
				
				String tqxzpc = CommUtil.trim(rs.getString("TQXZPC"));
				tqxzpc = WebContext.getBzdm(tqxzpc); 
				tqxzpc = CommUtil.removeCodePrefix(tqxzpc); 
				
				
				String jaay = rs.getString("AY");
				if("0".equals(jaay)) {
					jaay = "";
				}
				double jabdje = rs.getDouble("JABDJE");
				
				bean.setAjbs(ajbs);
				bean.setAh(ah);
				bean.setJarq(jarq);
				bean.setJaay(jaay);
				bean.setJafs(jafs);
				
			
				java.text.NumberFormat nf = java.text.NumberFormat.getInstance();   
				nf.setGroupingUsed(false);  
				bean.setJabdje(nf.format(jabdje));
				
				bean.setLarq(larq);
				bean.setAjzlx(ajzlx);
				bean.setTqxzpc(tqxzpc);
				
				bean.setXtajlx(CommUtil.trim(rs.getString("XTAJLX")));
				String ajlxdm = CommUtil.trim(String.valueOf(rs.getInt("AJLXDM")));
				if("0".equals(ajlxdm)){
					ajlxdm = "";
				}
				bean.setAjlxdm(ajlxdm);
				
				
				String sbajlx = getSbAjlx(bean.getAjlxdm(),bean.getXtajlx());
				if(UtilComm.isEmpty(sbajlx)) {
					log.error("无法匹配具体上报案件类型:"+bean.getAjbs()+"["+bean.getAh()+"]");
				}else{
					List<JaInfo> list = map.get(sbajlx);
					if(list == null) {
						list = new ArrayList<JaInfo>();
						map.put(sbajlx, list);
					}
					list.add(bean);
				}
			}
		}catch(Exception e){
			log.error("getJaList:" + sql,e);
		}finally{
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(pst);
		}
		return map;
	}
	
	/**
	 * 案件根据ajlxdm和xtajlx来判断具体属于上报的那个案件类型
	 * @param ajlxdm
	 * @param xtajlx
	 * @return
	 */
	public String getSbAjlx(String ajlxdm,String xtajlx) {
		String rtn = "";
		//1、如果存在AJLXDM则通过AJLXDM进行转换
		String val = WebContext.ajlxdmzhMap.get(ajlxdm);
		if(!UtilComm.isEmpty(val)) {
			rtn =  val;
		}else{
			//2、如果AJLXDM不存在则默认为XTAJLX，并且此系统案件类型必须在规范内
			if(AjlxConst.AJLX_DM_MAP.containsKey(xtajlx)) {
				rtn =  AjlxConst.AJLX_DM_MAP.get(xtajlx);
			}
		}	
		// v2.3中对于废弃的转换对应的
		//19   	81	诉前保全	已弃用，新收的此类案件按非诉保全案件上报
		//27   	1B	没收违法所得二审	已弃用，新收的此类案件按刑事二审案件上报
		//28   	1C	没收违法所得再审	已弃用，新收的此类案件按刑事再审案件上报
		if("19".equals(rtn)) {
			rtn = "41";
		}else if("27".equals(rtn)) {
			rtn = "2";
		}else if("28".equals(rtn)) {
			rtn = "4";
		}
		return rtn;
	}
	
	
}
