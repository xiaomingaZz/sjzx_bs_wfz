package tdh.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CalendarUtil {
	/*
	 * 时间格式为空时 默认为"yyyy-MM-dd HH:mm:ss"
	 */
	public static String  getNow(String gs){
		if(CommUtil.isEmpty(gs)){
			gs = "yyyy-MM-dd HH:mm:ss";
		}
		SimpleDateFormat sdf = new SimpleDateFormat(gs);
		return sdf.format(Calendar.getInstance().getTime());
	}
	
	public static Long getNowLong(){
		return Calendar.getInstance().getTimeInMillis();
	}
	
	public static String  getYear(){
		return Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
	}
		
	public static String  getGsSj(String sj,String gs ){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date=null;
		  try{
		    date=sdf.parse(sj); 
		  }catch(Exception e){
			  e.printStackTrace();
		  }
		  Calendar cal = Calendar.getInstance();
		  cal.setTime(date);
		  String  rtn= new SimpleDateFormat(gs).format(cal.getTime());
		  return rtn;
	}
	
	public static String getCjsj(){
		Calendar calendar = Calendar.getInstance();
    	calendar.set(calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH),calendar.get(Calendar.HOUR_OF_DAY) ,calendar.get(Calendar.MINUTE),calendar.get(Calendar.MILLISECOND));
        calendar.add(Calendar.MINUTE,-5);
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(calendar.getTime());
	}
	
	/**
	 * 
	 * 获取传入时间的相差天数的时间
	 * @param dt
	 * @param day  负数表示前几天 正数表示后几天
	 * @param gs
	 * @return
	 * @author chenjx
	 * @date 2015年6月26日
	 */
	public static String getLastDay(String dt,int day,String gs){
		  SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
		  Date date=null;
		  try{
		    date=sdf.parse(dt); 
		  }catch(Exception e){
			  e.printStackTrace();
		  }
		  Calendar cal = Calendar.getInstance();
		  cal.setTime(date);
		  cal.add(Calendar.DATE, day);
		  String nextDay = new SimpleDateFormat(gs).format(cal.getTime());
		  return nextDay;
	  }

}
