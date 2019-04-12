package tdh.ws;

import javax.jws.WebService;

@WebService
public interface ISpdtWebService {
	public String importSpdt(String spdtXml);
	
	public String importSpdtAndAjList(String spdtXml);
}
