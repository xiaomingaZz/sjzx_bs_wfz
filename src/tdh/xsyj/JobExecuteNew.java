/**
 *
 * @author Administrator
 * @date 2015年8月19日
 */
package tdh.xsyj;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import tdh.bean.Fy;
import tdh.frame.common.UtilComm;
import tdh.util.CalendarUtil;
import tdh.util.CommUtil;
import tdh.web.WebContext;
import tdh.ws.ISpdtWebService;
import tdh.ws.SoapInInterceptor;
import tdh.ws.SoapOutInterceptor;

/**
 *
 * @author chenjx
 * @version 创建时间：2015年8月19日 下午3:29:19
 */
public class JobExecuteNew implements StatefulJob {

	public static Map<String, String> fyXmlMap = new HashMap<String, String>();


	private static Log log = LogFactory.getLog(JobExecuteNew.class);

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		fyXmlMap.clear();
		Set<String> set = WebContext.fyMap.keySet();
		String dateTime = CalendarUtil.getNow("");
		String dt = dateTime.replace("-", "").replace(" ", "").replace(":", "");
		String rq = CalendarUtil.getGsSj(dateTime, "yyyyMMdd");
		
		ExecutorService executor = null;
		try {
			executor = Executors.newFixedThreadPool(WebContext.THREADS_NUMS);
			CompletionService<String> completionService = new ExecutorCompletionService<String>(executor);
			int n = 0;
			for (String key : set) {
				n++;
				Fy fy = WebContext.fyMap.get(key);
				completionService.submit(new ThreadNew(n, dateTime, fy));
			}
			//等待线程完成
			for (String key : set) {
				String re = completionService.take().get();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally {
			executor.shutdown();
			log.info("关闭线程池......");
		}
		log.info("开始组织xml......");
		StringBuffer result = new StringBuffer();
		try {
			// 组织XML
			StringBuffer xml = new StringBuffer();
			xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
			xml.append("<root>\n");
			xml.append("<tjsj>").append(CommUtil.convertRqT(dateTime)).append("</tjsj>\n");
			System.out.println("--------------"+WebContext.FyCode);
			System.out.println(WebContext.allfyMap.get(WebContext.FyCode).getDm());
			xml.append("<gyid>").append(WebContext.allfyMap.get(WebContext.FyCode).getDm()).append("</gyid>\n");
			xml.append("<datas>\n");

			// 遍历取数据
			for (String key : set) {
				String drxs = fyXmlMap.get(key + "_DRXS");
				String drja = fyXmlMap.get(key + "_DRJA");
				String id = WebContext.fyMap.get(key).getDm();
				if (CommUtil.isEmpty(drxs) || CommUtil.isEmpty(drja) || CommUtil.isEmpty(id)) {
					log.info(key + " 未生产数据......");
				} else {
					xml.append("<data>\n");
					xml.append("<id>").append(id).append("</id>\n");
					xml.append("<drxs>").append(drxs).append("</drxs>\n");
					xml.append("<drja>").append(drja).append("</drja>\n");
					xml.append("</data>\n");
				}
			}
			xml.append("</datas>\n");
			xml.append("</root>\n");
			writeToFile(WebContext.DIR, rq, dt + "(old).xml", xml.toString());
			log.info("旧5分钟xml已生成完毕，准备开始发送......");
			if (!WebContext.FyCode.startsWith("51") && !WebContext.FyCode.startsWith("53")) {
				String filename = WebContext.TYPE + "_" + rq + "//"+dt;
				try {
					String ret = wsSendToOld(xml.toString(), WebContext.OLDURL);
					if (ret.contains("true")) {
						result.append(dateTime + "(" + rq + ")时实数据(旧五分钟)发送成功 文件目录：" + WebContext.DIR + "//" + filename + "(old).xml\n");
					} else if (ret.contains("false")) {
						result.append(dateTime + "(" + rq + ")时实数据(旧五分钟) 最高院反馈：未能够成功导入到平台数据库中\n 文件目录：" + WebContext.DIR
								+ "//" + filename + "(old).xml\n");
						result.append(ret);
					} else if ("接口为空".equals(result)) {
						result.append(dateTime + "(" + rq + ")时实数据(五分钟) 接口为空 只生成在本地");
					} else {
						result.append(dateTime + "(" + rq + ")时实数据(旧五分钟) 最高院反馈的信息未能识别 文件目录：" + WebContext.DIR + "//"
								+ filename + "(old).xml\n");
						result.append(ret);
					}
				} catch (Exception e) {
					e.printStackTrace();
					result.append(dateTime + "(" + rq + ")时实数据(旧五分钟) 调用最高院接口失败。 文件目录：" + WebContext.DIR + "//" + filename + "(old).xml\n");
				}
			}
			xml.setLength(0);
			// 新五分钟
			// 组织XML
			xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
			xml.append("<root>\n");
			//xml.append("<tjsj>").append(CommUtil.convertRq9(dateTime)).append("</tjsj>\n");
			xml.append("<tjsj>").append(CommUtil.convertRqT(dateTime)).append("</tjsj>\n");
			xml.append("<gyid>").append(WebContext.allfyMap.get(WebContext.FyCode).getDm()).append("</gyid>\n");
			xml.append("<datas>\n");
			// 遍历取数据
			for (String key : set) {
				String data = fyXmlMap.get(key);
				if (CommUtil.isEmpty(data)) {
					log.info(key + " 未生产数据。");
				} else {
					xml.append(data);
				}
			}

			xml.append("</datas>\n");
			xml.append("</root>\n");
			writeToFile(WebContext.DIR, rq, dt + ".xml", xml.toString());
			log.info("新5分钟xml已生成完毕，准备开始发送......");
			String filename = WebContext.TYPE + "_" + rq + "//"+dt;
			try {
				log.info("开始调接口  传递xml数据");
				String ret = wsSendTo(xml.toString(), WebContext.JKURL);
				if (ret.contains("true")) {
					result.append(dateTime + "(" + rq + ")时实数据(五分钟)发送成功 文件目录：" + WebContext.DIR + "//" + filename
							+ ".xml\n");
				} else if (ret.contains("false")) {
					result.append(dateTime + "(" + rq + ")时实数据(五分钟) 最高院反馈：未能够成功导入到平台数据库中\n 文件目录：" + WebContext.DIR
							+ "//" + filename + ".xml\n");
					result.append(ret);
				} else if ("接口为空".equals(result)) {
					result.append(dateTime + "(" + rq + ")时实数据(五分钟) 接口为空 只生成在本地");
				} else {
					result.append(dateTime + "(" + rq + ")时实数据(五分钟) 最高院反馈的信息未能识别 文件目录：" + WebContext.DIR + "//" + filename + ".xml\n");
					result.append(ret);
				}
			} catch (Exception e) {
				log.error(e);
				result.append(dateTime + "(" + rq + ")时实数据(五分钟) 调用最高院接口失败。 文件目录：" + WebContext.DIR + "//" + filename + ".xml\n");
			}

			log.info(result.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			log.info("本轮5分钟报送结束......");
		}
	}

	/**
	 * 调用最高院的接口服务发送每日新收和已结
	 * 
	 * @param xml
	 */
	private String wsSendTo(String xml, String url) throws Exception {
		if (UtilComm.isEmpty(url)) {
			return "接口为空";
		}
		if (UtilComm.isEmpty(xml)) {
			log.error("接口传递参数为空（xml）");
			return "接口传递参数为空（xml）";
		}
		try {
			String paramname = "arg0";
			/*if(WebContext.NAMESPACE.indexOf("tdh")>-1){
				paramname = "xmlSpdt";
			}else if(WebContext.NAMESPACE.indexOf("thunisoft")>-1){
				if(WebContext.FyCode.startsWith("42")){
					paramname = "xmlSpdt";//湖北地区由于namaspace也变更为thunisoft了，所以方法名改为xmlSpdt；全国版还是arg0
				}else{
					paramname = "arg0";//湖北地区由于namaspace也变更为thunisoft了，所以方法名改为xmlSpdt；全国版还是arg0
				}
				
			}*/
			Service service = new Service();
			Call call = (Call) service.createCall();
			call.setTargetEndpointAddress(url);
			call.setOperationName(new QName(WebContext.NAMESPACE, WebContext.METHOD));
			call.setTimeout(100 * 1000);
			call.addParameter(WebContext.PARAMETER, XMLType.XSD_STRING, ParameterMode.IN);
			call.setReturnType(XMLType.XSD_STRING);
			String result = (String) call.invoke(new Object[] { xml });
			return result;
		} catch (Exception e) {
			log.error("接口wsSendTo失败", e);
			throw e;
		}
	}

	/**
	 * 调用最高院的接口服务发送每日新收和已结
	 * 
	 * @param xml
	 */
	private String wsSendToOld(String xml, String url) throws Exception {
		if (UtilComm.isEmpty(url)) {
			return "接口为空";
		}
		try {
			JaxWsProxyFactoryBean factorBean = new JaxWsProxyFactoryBean();
			factorBean.getInInterceptors().add(new SoapInInterceptor());
			factorBean.getOutInterceptors().add(new SoapOutInterceptor());
			factorBean.setServiceClass(ISpdtWebService.class);
			factorBean.setAddress(url);

			ISpdtWebService impl = (ISpdtWebService) factorBean.create();

			String result = impl.importSpdt(xml);

			return result;
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * 保存到临时文件下
	 * 
	 * @param l4dm
	 * @param xmlName
	 * @param xmlNr
	 * @param dir
	 *            路径 ExportContext.XML_ERR_DIR 或者 ExportContext.XML_DIR
	 */
	private void writeToFile(String dir, String rq, String xmlName, String xmlNr) {
		try {
			String filePath = dir + "//" + rq;
			File file = new File(filePath + "//" + WebContext.TYPE + "_" +xmlName);
			if (file.exists()) {
				file.delete();
			}
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			FileUtils.writeStringToFile(file, xmlNr, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
