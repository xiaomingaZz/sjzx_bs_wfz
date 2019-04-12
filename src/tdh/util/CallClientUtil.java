package tdh.util;

import java.net.URL;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

public final class CallClientUtil {
	/**
	 * 调用接口
	 * 
	 * @param url:接口地址
	 * @param targetNameSpace:接口NameSpace
	 * @param methodName:方法名
	 * @param params:参数的数组
	 * @param paramNames:参数名称的数组
	 * 
	 * @return String 接口返回值
	 * @author xiarui
	 * @date 2017-03-14
	 * 
	 */
	public static String callWebService(String url, String targetNameSpace, String methodName, Object[] params, Object[] paramNames) {
		String rtn = "";
		try {
			Service service = new Service();
			Call call = (Call) service.createCall();
			call.setTargetEndpointAddress(new URL(url));
			call.setOperationName(new javax.xml.namespace.QName(targetNameSpace, methodName));
			for (Object paramName : paramNames) {
				call.addParameter(paramName + "", org.apache.axis.encoding.XMLType.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
			}
			call.setReturnType(org.apache.axis.encoding.XMLType.XSD_STRING);
			call.setTimeout(100000);
			rtn = (String) call.invoke(params);
		} catch (Exception e) {
			rtn = "fail";
			e.printStackTrace();
		}

		return rtn;
	}
}
