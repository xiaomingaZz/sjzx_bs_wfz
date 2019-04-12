package tdh.ws;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.log4j.Logger;

public class SoapOutInterceptor extends AbstractPhaseInterceptor<Message> {

	private Logger logger = Logger.getLogger(SoapOutInterceptor.class);

	public SoapOutInterceptor() {
		super(Phase.PREPARE_SEND); // 触发点在流关闭之前
	}

	@Override
	public void handleMessage(Message message) {
		// TODO Auto-generated method stub
		System.out
				.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*********In****SoapOutInterceptor******");

		System.out
				.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*********END****SoapOutInterceptor******");
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
