package tdh.xsyj.executeSbtj;

import java.io.File;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import tdh.bean.Fy;
import tdh.db.DBHelper;
import tdh.frame.common.UUID;
import tdh.frame.common.UtilComm;
import tdh.util.CommUtil;
import tdh.web.WebContext;
import tdh.xsd.ValidateXML;

/**
 * 10420需求 
 * @author 
 *
 */
public class JobExecuteSbtj  implements StatefulJob{
	private final static Log logger = LogFactory.getLog(JobExecuteSbtj.class);
	private static boolean flag = false; //控制标记，由于数据量过大程序运行时间过长，每隔5分钟运行一次，有可能上一次运行尚未结束
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		if(flag==false){
			flag=true;//当下一轮运行，如果程序未完成不让程序重新运行
			initSbtj();//初始化临时表AJ_SB_WFZ，将新增数据加入到临时表中
			logger.info("开始上报统计");
			ExecutorService executor = null;
			Date start = new Date();
			String tjsj =CommUtil.foramtDate(start, "yyyy-MM-dd HH:mm:ss");
			try {
				executor = Executors.newFixedThreadPool(WebContext.THREADS_NUMS);
				CompletionService<Map<String,String>> completionService = new ExecutorCompletionService<Map<String,String>>(executor);
				int n = 0;
				//根据法院代码依次操作
				for (String key : WebContext.fyMap.keySet()) {
					n++;
					Fy fy = WebContext.fyMap.get(key);
					completionService.submit(new CourtThreadSbtj(fy, tjsj));
				}
				//等待线程完成
				for (String key : WebContext.fyMap.keySet()) {
					try {
						Map<String,String> re = completionService.take().get();
					} catch (Exception e) {
						logger.error("线程内部发送的异常",e);
					}
				}
				destorySbtj();
			} catch (Exception e) {
				logger.error("",e);
				return;
			} finally {
				flag =false;
				if(executor!=null){
					executor.shutdown();
				}
				logger.info("关闭线程池......");
			}
		}else{
			logger.warn("新的一轮欲想开始,但上一轮仍在执行,本轮放弃...");
		}
	}
	
	/**
	 * 初始化临时表，新增数据到临时表中
	 */
	private void initSbtj(){
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			//依次获取源库连接
			for(Map.Entry<String, Map<String,String>> entry : WebContext.sbConnDetialMap.entrySet()){
				int count = 0;
				do{
					conn = DBHelper.getConn(entry.getValue().get("CONN_DRIVER"), entry.getValue().get("CONN_URL"), entry.getValue().get("CONN_USER"), entry.getValue().get("CONN_PASSWORD"));
					if(conn!=null){
						break;
					}else{count++;}
				}while(count<5);
				if(conn==null){
					logger.error("获取链接5次此仍然异常:"+entry.getValue());	
					continue;
				}else{
					logger.debug(entry.getKey()+"成功获得数据库连接，开始采集数据......");
				}
				st=conn.createStatement();
				//获取EAJ中大于等于上一次LASTUPDATE最大时间
				String sql="SELECT max(LASTUPDATE) LASTUPDATE FROM EAJ where LASTUPDATE>='" + WebContext.lastupdateMap.get(entry.getKey()) +"'";
				if(null!=WebContext.FILTER && !"".equals(WebContext.FILTER)){
					sql += " and " +WebContext.FILTER;
				}
				rs=st.executeQuery(sql);
				String d="";
				while(rs.next()){
					d=rs.getString("LASTUPDATE").substring(0, 19);
				}
				//将EAJ表中大于等于上一次LASTUPDATE且小于获取EAJ最大时间
				sql= "insert into AJ_SB_WFZ select AHDM,FYDM,'0','0' from EAJ where LASTUPDATE>='" + WebContext.lastupdateMap.get(entry.getKey()) +"' and LASTUPDATE<'"+d+"'";
				if(null!=WebContext.FILTER && !"".equals(WebContext.FILTER)){
					sql += " and " +WebContext.FILTER;
				}
				if(!UtilComm.isEmpty(WebContext.FILTER_FYDM)){
					String[] fydms=WebContext.FILTER_FYDM.split(",");
					StringBuffer sb=new StringBuffer();
					for(String fydm:fydms){
						sb.append("'").append(fydm).append("',");
					}
					sql+=" and FYDM not in("+sb.toString().substring(0,sb.toString().length()-1)+")";
				}
				st.executeUpdate(sql);
				if(!"".equals(d)){
					conn=DBHelper.getConn("export");
					st=conn.createStatement();
					//将获取EAJ最大时间添回到TS_DB表中LASTUPDATE字段
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
	
	private void destorySbtj(){
		Connection conn = null;
		Statement st = null;
		try {
			//依次获取源库连接
			for(Map.Entry<String, Map<String,String>> entry : WebContext.sbConnDetialMap.entrySet()){
				int count = 0;
				do{
					conn = DBHelper.getConn(entry.getValue().get("CONN_DRIVER"), entry.getValue().get("CONN_URL"), entry.getValue().get("CONN_USER"), entry.getValue().get("CONN_PASSWORD"));
					if(conn!=null){
						break;
					}else{count++;}
				}while(count<5);
				if(conn==null){
					logger.error("获取链接5次此仍然异常:"+entry.getValue());	
					continue;
				}else{
					logger.debug(entry.getKey()+"成功获得数据库连接，开始采集数据......");
				}
				st=conn.createStatement();
				String sql="truncate table AJ_SB_WFZ";
				st.executeUpdate(sql);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}
	}
	
}
