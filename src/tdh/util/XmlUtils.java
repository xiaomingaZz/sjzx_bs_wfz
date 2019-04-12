package tdh.util;

public class XmlUtils {
	
	public static final String XML_HEAD= "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
	
	public static final String XML_GS = "xmlns=\"http://dataexchange.court.gov.cn/2009/data\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
	
	public static String getHead(){
		return XML_HEAD;
	}
	
	public static String getXmlGs(){
		return XML_GS;
	}

	
}
