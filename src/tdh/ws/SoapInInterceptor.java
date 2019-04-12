package tdh.ws;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.log4j.Logger;

public class SoapInInterceptor extends AbstractPhaseInterceptor<Message> {
	private static final Logger log = Logger.getLogger(SoapInInterceptor.class);

	public SoapInInterceptor(String phase) {
		super(phase);
	}

	public SoapInInterceptor() {
		super(Phase.RECEIVE);
	}

	public void handleMessage(Message message) throws Fault {

		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*********In****SoapInInterceptor******");
		/*
		 * CachedOutputStream csnew = (CachedOutputStream) message
		 * .getContent(OutputStream.class); InputStream in; try {
		 * 
		 * String xml = ""; if (csnew != null) { in = csnew.getInputStream();
		 * xml = IOUtils.toString(in); } System.out.println("输入进来的XML：" + xml);
		 * } catch (IOException e) { // TODO Auto-generated catch block
		 * log.error("Error when split original inputStream. CausedBy : " + "\n"
		 * + e); e.printStackTrace(); }
		 */

		String reqParams = null;
		if (message.get(message.HTTP_REQUEST_METHOD).equals("GET")) {// 采用GET方式请求
			reqParams = (String) message.get(message.QUERY_STRING);
			message.remove(message.QUERY_STRING);
			/* reqParams=this.getParams(this.getParamsMap(reqParams)); */
			//System.out.println(reqParams);
			message.put(message.QUERY_STRING, reqParams);

		} else if (message.get(message.HTTP_REQUEST_METHOD).equals("POST")) {// 采用POST方式请求
			try {
				String aa = message.getContent(String.class);
				InputStream is = message.getContent(InputStream.class);
				reqParams = IOUtils.toString(is);
				//System.out.println(reqParams);
				reqParams = reqParams.replace(".xmlns", " xmlns");
				if (is != null)
					message.setContent(InputStream.class,
							new ByteArrayInputStream(reqParams.getBytes()));
			} catch (Exception e) {
				log.error("GatewayInInterceptor异常", e);
			}
		}
		//log.info("请求的参数：" + reqParams);
		//System.out.println("输入进来的XML：" + reqParams);

		//System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*********END****SoapInInterceptor******");
	}

	private Map<String, String> getParamsMap(String strParams) {
		if (strParams == null || strParams.trim().length() <= 0) {
			return null;
		}
		Map<String, String> map = new HashMap<String, String>();
		String[] params = strParams.split("&");
		for (int i = 0; i < params.length; i++) {
			String[] arr = params[i].split("=");
			map.put(arr[0], arr[1]);
		}
		return map;
	}

	private String getParams(Map<String, String> map) {
		if (map == null || map.size() == 0) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		Iterator<String> it = map.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			String value = map.get(key);
			/*
			 * 这里可以对客户端上送过来的输入参数进行特殊处理。如密文解密；对数据进行验证等等。。。
			 * if(key.equals("content")){ value.replace("%3D", "="); value =
			 * DesEncrypt.convertPwd(value, "DES"); }
			 */
			if (sb.length() <= 0) {
				sb.append(key + "=" + value);
			} else {
				sb.append("&" + key + "=" + value);
			}
		}
		return sb.toString();
	}

	private class CachedStream extends CachedOutputStream {

		public CachedStream() {

			super();

		}

		protected void doFlush() throws IOException {

			currentStream.flush();

		}

		protected void doClose() throws IOException {

		}

		protected void onWrite() throws IOException {

		}

	}
}
