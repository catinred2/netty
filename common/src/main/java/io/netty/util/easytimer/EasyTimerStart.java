package io.netty.util.easytimer;

import io.netty.util.Timeout;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class EasyTimerStart {
    private static final EasyTimer timer = new EasyHashedWheelTimer();
    private Logger logger = Logger.getLogger(EasyTimerStart.class);
    
    private volatile static EasyTimerStart instance = null;
    
    public static EasyTimerStart getInstance() {
        if (instance == null) {
            synchronized (EasyTimerStart.class) {
                if (instance == null) {
                    instance = new EasyTimerStart();
                }
            }
            
        }
        return instance;
    }
    
    /**
     * 
     * <一句话功能简述>添加执行的任务，EasyTimerTask(String cronExpression, String name)
     * name=是任务的标识符；cronExpression=是时间的正则表达式，时间可配置成重复执行。参照quartz配置 <功能详细描述>
     * 
     * @param task 执行的任务
     * @author： along
     */
    public boolean addTimer(EasyTimerTask task) {
        long next = task.nextTriggerTime();
        if (next == -1L) {
            logger.debug("The execution time expired ,task name:" + task.name);
            // logger.debug("task time:"+task.cronExpression.toString());
            return false;
        }
        long span = next - System.currentTimeMillis();
        if (span < 0) {
            span = 0;
        }
        Timeout timeout = timer.newTimeout(task, span, TimeUnit.MILLISECONDS);
        TimerUtil.timeoutMap.put(task.getName(), timeout);// 任务名作为标识符，停止时。以后考虑用唯一码等
        return true;
    }
    
    /**
     * 
     * <一句话功能简述>取消某个执行的任务，该任务下一个执行的时间点后将不在执行，已经过的时间点则已经执行不能取消 <功能详细描述>
     * 
     * @param name 唯一标识符
     * @author： along
     */
    public boolean cancelTask(String taskName) {
        Timeout time = TimerUtil.timeoutMap.get(taskName);
        if (time != null) {
            boolean result = time.cancel();
            return result;
        }
        logger.debug(taskName + " task does not exist ");
        return true;
    }
}
