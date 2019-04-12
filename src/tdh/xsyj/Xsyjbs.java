/**
 *
 * @author Administrator
 * @date 2015年8月19日
 */
package tdh.xsyj;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import tdh.web.WebContext;
import tdh.xsyj.execute15.JobExecute15;
import tdh.xsyj.executeFJMS.JobExecuteFjms;
import tdh.xsyj.executeHB.JobExecuteHB;
import tdh.xsyj.executeHB09Z15.JobExecuteHB09Z15;
import tdh.xsyj.executeSP15.JobExecuteSP15;
import tdh.xsyj.executeSbtj.JobExecuteSbtj;

/**
 *
 * @author chenjx
 * @version 创建时间：2015年8月19日  下午3:30:01
 */
public class Xsyjbs  implements ServletContextListener{
	
	public void contextInitialized(ServletContextEvent event) {
		try{
			startScheduler();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void contextDestroyed(ServletContextEvent event) {
		if(scheduler!=null){
			try {
				scheduler.shutdown();
			} catch (SchedulerException e) {
				e.printStackTrace();
			}
		}
	}
	
	Scheduler scheduler = null;
	public void startScheduler()throws Exception{
		scheduler = StdSchedulerFactory.getDefaultScheduler();
		JobDetail minuteJob = null;
		if(!WebContext.VERSION.equals("09Z15")){
			System.out.println(WebContext.VERSION);
		}
		if(WebContext.VERSION.equals("09Z15")){
			minuteJob = new JobDetail("xsyjJob","SimpleGroup",JobExecute15.class);
		}else if(WebContext.VERSION.equals("HB")){
			minuteJob = new JobDetail("xsyjJob","SimpleGroup",JobExecuteHB.class);
		}else if(WebContext.VERSION.equals("HB09Z15")){
			minuteJob = new JobDetail("xsyjJob","SimpleGroup",JobExecuteHB09Z15.class);
		}else if(WebContext.VERSION.equals("SBTJ")){ //10420需求 广东地区5分钟统计上报
			minuteJob = new JobDetail("xsyjJob","SimpleGroup",JobExecuteSbtj.class);
		}else if(WebContext.VERSION.equals("FJMS")){
			minuteJob = new JobDetail("xsyjJob","SimpleGroup",JobExecuteFjms.class);
		}else if(WebContext.VERSION.equals("15")){//需求14200
			minuteJob = new JobDetail("xsyjJob","SimpleGroup",JobExecuteSP15.class);
		}else{
			minuteJob = new JobDetail("xsyjJob","SimpleGroup",JobExecuteNew.class);
		}//
		Trigger minuteJobtrigger = new CronTrigger("xsyjJobTrigger",null, "0 0/1 * * * ?");
		scheduler.scheduleJob(minuteJob, minuteJobtrigger);

		scheduler.start();
	}

}
