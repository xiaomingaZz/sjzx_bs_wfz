package tdh.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class Base64 {

	private static BASE64Encoder encoder = new sun.misc.BASE64Encoder();
	private static BASE64Decoder decoder = new sun.misc.BASE64Decoder();

	public static String encode(byte[] bytes){
		if(bytes == null || bytes.length == 0) return "";
		return encoder.encode(bytes);
	}
	
	public static String encode(String s) {
		try {
			if (s == null)
				return "";
			return encoder.encode(s.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String encode(String s, String bmfs) {
		try {
			if (s == null)
				return "";
			return encoder.encode(s.getBytes(bmfs));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static byte[] decodeToBytes(String s){
		if(s == null || "".equals(s.trim())) return null;
		try {
			return decoder.decodeBuffer(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String decode(String s) {
		try {
			if (s == null)
				return "";
			byte[] temp = decoder.decodeBuffer(s);
			return new String(temp, "utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}

	public static String decode(String s, String bmfs) {
		try {
			if (s == null)
				return "";
			byte[] temp = decoder.decodeBuffer(s);
			return new String(temp, bmfs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}
}
