package tdh;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Priority;

/**
 * @author ShouJW
 * 该类建议用来给页面-将各级别日志分开记录,不会造成混合
 */
public class LevelIsolatorFileAppender extends DailyRollingFileAppender {

	@Override
	public boolean isAsSevereAsThreshold(Priority priority) {
		return super.getThreshold().equals(priority);
	}

}
