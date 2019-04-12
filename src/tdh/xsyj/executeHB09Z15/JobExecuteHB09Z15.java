package tdh.xsyj.executeHB09Z15;

import java.io.File;
import java.io.StringWriter;
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
import tdh.frame.common.UtilComm;
import tdh.util.CommUtil;
import tdh.web.WebContext;
import tdh.xsd.ValidateXML;

public class JobExecuteHB09Z15  implements StatefulJob{
	private final static Log logger = LogFactory.getLog(JobExecuteHB09Z15.class);
	private boolean flag = false;
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		if(!flag){
			logger.info("开始HB09Z15接口");
			Document  doc = DocumentHelper.createDocument();
			doc.setXMLEncoding("UTF-8");
			
			Element element = doc.addElement("root");
			element.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			element.addAttribute("xsi:noNamespaceSchemaLocation", "spdtAndAjList_all_15.xsd");
			GenerateXmlData(element);
			//removeEmptyNode(element);
			try {
				System.out.println("===========================");
				System.out.println(format(doc));
				System.out.println("===========================");
				
				//logger.info("发送的数据:\n"+doc.asXML());
				String xmlData =  ValidateXML.format(doc);
				String result = ValidateXML.validateXMLData15(xmlData);
				String fileName = StringUtils.EMPTY;
				String resultXml = StringUtils.EMPTY;
				try {
					FileUtils.writeStringToFile(new File(WebContext.DIR,"5分钟数据_数据.xml"), xmlData,"UTF-8");
				} catch (Exception e) {
					e.printStackTrace();
				}
				if("success".equals(result)){
					String xmlStr = wsSendTo(xmlData, WebContext.JKURL);
					if("接口为空".equals(xmlStr)||"接口传递参数为空（xml）".equals(xmlStr)){
						logger.info(xmlStr);
					}else{
						resultXml = xmlStr;
						Document doc_result = DocumentHelper.parseText(xmlStr);
						Element root = doc_result.getRootElement(); 
						Element flag = root.element("flag");
						Element message = root.element("message");
						if(flag!=null ){
							//获得根元素
							if(!flag.getTextTrim().contains("true") && !flag.getTextTrim().contains("success")){
								logger.info("本地校验通过,报送失败.. 原因=>\n"+flag.getTextTrim()+"\n"+xmlStr);
								fileName = "5分钟数据_请求返回结果不通过";
							}else{
								fileName = "5分钟数据_请求成功数据";
								logger.info("本地校验通过,报送成功.. =>\n"+xmlStr);
							}
						}else{
							fileName = "5分钟数据_请求返回结果不符合xml规范";
							logger.info("本地校验通过,接口返回结果，不符合规范xml结构 =>\n"+xmlStr);
						}
					}
				}else{
					fileName = "5分钟数据_xsd校验失败";
					logger.info("本地校验失败(不发送):"+result);
				}
				String suffix = CommUtil.foramtDate(new Date(), "yyyyMMdd_HHmmss")+".xml";
				File reqFile = new File(WebContext.DIR,fileName+suffix);
				File resFile = new File(WebContext.DIR,"请求结果_"+suffix);
				FileUtils.writeStringToFile(reqFile, xmlData,"UTF-8");
				logger.info("请求参数,本地生成路径"+reqFile.getAbsolutePath());
				FileUtils.writeStringToFile(resFile, resultXml,"UTF-8");
				logger.info("请求结果文件路径:"+resFile.getAbsolutePath());
			} catch (Exception e) {
				logger.error("接口["+WebContext.JKURL+"]【"+WebContext.NAMESPACE+"】报送失败.",e);
			}
		}else{
			logger.warn("新的一轮欲想开始,但上一轮仍在执行,本轮放弃...");
		}
	}
	
	public void GenerateXmlData(Element  element){
		Date start = new Date();
		String tjsj =CommUtil.foramtDate(start, "yyyy-MM-dd HH:mm:ss");
		
		element.addElement("tjsj").setText(CommUtil.convertRqT( tjsj));
		//测试华宇接口  3600 成功 
		element.addElement("gyid").setText(WebContext.allfyMap.get(WebContext.FyCode.substring(0, 2)+"0000").getDm());
		Element root = element.addElement("datas");
		ExecutorService executor = null;
		try {
			executor = Executors.newFixedThreadPool(WebContext.THREADS_NUMS);
			CompletionService<Map<String,Integer>> completionService = new ExecutorCompletionService<Map<String,Integer>>(executor);
			int n = 0;
			for (String key : WebContext.fyMap.keySet()) {
				n++;
				Fy fy = WebContext.fyMap.get(key);
				completionService.submit(new CourtThreadHB09Z15(fy, tjsj, n,root));
			}
			int xssl = 0,yjsl = 0;//立案总数,结案总数
			//等待线程完成
			for (String key : WebContext.fyMap.keySet()) {
				try {
					Map<String,Integer> re = completionService.take().get();
					xssl +=re.get("XSSL");
					yjsl +=re.get("YJSL");
				} catch (Exception e) {
					logger.error("线程内部发送的异常",e);
				}
			}
			root.addComment("当日新收案件总数:"+xssl);
			root.addComment("当日已结案件总数:"+yjsl);
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
			logger.error("接口传递参数为空（xml）");
			return "接口传递参数为空（xml）";
		}
		try {
			String paramname = "arg0";
//			if(WebContext.NAMESPACE.indexOf("tdh")>-1){
//				paramname = "xmlSpdt";
//			}else if(WebContext.NAMESPACE.indexOf("thunisoft")>-1){
//				if(WebContext.FyCode.startsWith("42")){
//					paramname = "xmlSpdt";//湖北地区由于namaspace也变更为thunisoft了，所以方法名改为xmlSpdt；全国版还是arg0
//				}else{
//					paramname = "arg0";//湖北地区由于namaspace也变更为thunisoft了，所以方法名改为xmlSpdt；全国版还是arg0
//				}
//				
//			}
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
			logger.error("接口wsSendTo失败", e);
			throw e;
		}
	}
	

	public synchronized static String format(Document doc){
        OutputFormat formater = OutputFormat.createPrettyPrint();
        formater.setEncoding("UTF-8");
        StringWriter out = new StringWriter();
        XMLWriter writer=null;
		try {
			writer = new XMLWriter(out, formater);
			writer.write(doc);
			return out.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			CommUtil.closeStream(out);
		}
        return "";
	}
	
	private void removeEmptyNode(Element root){
		List<Element> nexts =  root.elements();
		for (Element element : nexts) {
			if(element.isTextOnly()){
				String val = element.getTextTrim();
				if("".equals(val) || "null".equals(val)){
					Element parentElement  = element.getParent();
					if(parentElement==null){
						//System.out.println("没有父节点==>"+element.getName()); 
					}else{
						parentElement.remove(element);
						removeEmptyNode(parentElement);
					}
				}
			}else{
				removeEmptyNode(element);
			}
		}
	}
}
