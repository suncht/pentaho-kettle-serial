package com.peraglobal.web.quartz;

import java.util.Map;
import java.util.Map.Entry;

import org.pentaho.di.repository.kdr.KettleDatabaseRepository;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;

import com.peraglobal.web.utils.DateTime;



public class QuartzManager {

	private static SchedulerFactory schedulerFactory = new StdSchedulerFactory();  

	/**
	 * @Title addJob
	 * @Description 添加一个定时任务 
	 * @param jobName 任务名
	 * 				以作业为例：  JOB@1(资源库ID)@/job/mysql-mysql(JOB全路径)
	 * @param jobGroupName 任务组名 
	 * 				以作业为例：  JOB_GROUP@1(用户ID)@1(资源库ID)@/job/mysql-mysql(JOB全路径)
	 * @param triggerName 触发器名 
	 * 				以作业为例：  TRIGGER@1(资源库ID)@/job/mysql-mysql(JOB全路径)
	 * @param triggerGroupName 触发器组名 
	 * 				以作业为例：  TRIGGER_GROUP@1(用户ID)@1(资源库ID)@/job/mysql-mysql(JOB全路径)
	 * @param jobClass 任务对象实例
	 * @param cron 时间设置，参考quartz说明文档 
	 * @param parameter 传入的参数
	 * @return void
	 */
    public static void addJob(String jobName, String jobGroupName, 
            String triggerName, String triggerGroupName, Class<? extends Job> jobClass, String cron, Map<String, Object> parameter) {  
        try {
            Scheduler sched = schedulerFactory.getScheduler();
            // 任务名，任务组，任务执行类
            JobDetail jobDetail= JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName).build();
            // 添加任务执行的参数
            for (Entry<String, Object> entry : parameter.entrySet()) {
            	jobDetail.getJobDataMap().put(entry.getKey(), entry.getValue());
            }
            // 触发器
            TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
            // 触发器名,触发器组
            triggerBuilder.withIdentity(triggerName, triggerGroupName);
            triggerBuilder.startNow();
            // 触发器时间设定
            triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cron));
            // 创建Trigger对象
            CronTrigger trigger = (CronTrigger) triggerBuilder.build();
            // 调度容器设置JobDetail和Trigger
            sched.scheduleJob(jobDetail, trigger);
            // 启动  
            if (!sched.isShutdown()) {  
                sched.start();  
            }  
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    }  
    
    
    public static void addOnceJob(String jobName, String jobGroupName, 
            String triggerName, String triggerGroupName, Class<? extends Job> jobClass, Map<String, Object> parameter){
    	try {
    		Scheduler sched = schedulerFactory.getScheduler();
    		// 任务名，任务组，任务执行类
            JobDetail jobDetail= JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName).build();
            // 添加任务执行的参数
            for (Entry<String, Object> entry : parameter.entrySet()) {
            	jobDetail.getJobDataMap().put(entry.getKey(), entry.getValue());
            }
            // 触发器
            TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
            // 触发器名,触发器组
            triggerBuilder.withIdentity(triggerName, triggerGroupName);
            triggerBuilder.startNow();
            //立即执行
            StringBuilder cronBuilder = new StringBuilder();
    		DateTime dateTime = new DateTime();
    		Integer addMinute = dateTime.second() >= 58 ? 2 : 1;
    		cronBuilder.append("0").append(" ")
    						.append(dateTime.minute() + addMinute).append(" ")
    						.append(dateTime.hour(true)).append(" ")
    						.append(dateTime.dayOfMonth()).append(" ")
    						.append(dateTime.monthStartFromOne()).append(" ")
    						.append("?").append(" ")
    						.append(String.valueOf(dateTime.year()));
            // 触发器时间设定
            triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cronBuilder.toString()));
            // 创建Trigger对象
            CronTrigger trigger = (CronTrigger) triggerBuilder.build();
            // 调度容器设置JobDetail和Trigger
            sched.scheduleJob(jobDetail, trigger);
            // 添加任务执行监听器
            sched.getListenerManager().addJobListener(new QuartzListener(), 
            		KeyMatcher.keyEquals(new JobKey(jobName, jobGroupName)));
            // 启动  
            if (!sched.isShutdown()) {  
                sched.start();  
            }             
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 
     * @Description: 修改一个任务的触发时间
     *  
     * @param jobName 
     * @param jobGroupName
     * @param triggerName 触发器名
     * @param triggerGroupName 触发器组名 
     * @param cron   时间设置，参考quartz说明文档   
     */  
    public static void modifyJobTime(String jobName, 
            String jobGroupName, String triggerName, String triggerGroupName, String cron) {  
        try {  
            Scheduler sched = schedulerFactory.getScheduler();  
            TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
            CronTrigger trigger = (CronTrigger) sched.getTrigger(triggerKey);  
            if (trigger == null) {  
                return;  
            }
            String oldTime = trigger.getCronExpression();  
            if (!oldTime.equalsIgnoreCase(cron)) { 
                /** 方式一 ：调用 rescheduleJob 开始 */
                // 触发器  
                TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
                // 触发器名,触发器组  
                triggerBuilder.withIdentity(triggerName, triggerGroupName);
                triggerBuilder.startNow();
                // 触发器时间设定  
                triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cron));
                // 创建Trigger对象
                trigger = (CronTrigger) triggerBuilder.build();
                // 方式一 ：修改一个任务的触发时间
                sched.rescheduleJob(triggerKey, trigger);
                /** 方式一 ：调用 rescheduleJob 结束 */

                /** 方式二：先删除，然后在创建一个新的Job  */
                //JobDetail jobDetail = sched.getJobDetail(JobKey.jobKey(jobName, jobGroupName));  
                //Class<? extends Job> jobClass = jobDetail.getJobClass();  
                //removeJob(jobName, jobGroupName, triggerName, triggerGroupName);  
                //addJob(jobName, jobGroupName, triggerName, triggerGroupName, jobClass, cron); 
                /** 方式二 ：先删除，然后在创建一个新的Job */
            }  
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    }  

    /** 
     * @Description: 移除一个任务 
     *  
     * @param jobName 
     * @param jobGroupName 
     * @param triggerName 
     * @param triggerGroupName 
     */  
    public static void removeJob(String jobName, String jobGroupName,  
            String triggerName, String triggerGroupName) {  
        try {  
            Scheduler sched = schedulerFactory.getScheduler();  
            TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
            sched.pauseTrigger(triggerKey);// 停止触发器  
            sched.unscheduleJob(triggerKey);// 移除触发器  
            sched.deleteJob(JobKey.jobKey(jobName, jobGroupName));// 删除任务  
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    }  

    /** 
     * @Description:启动所有定时任务 
     */  
    public static void startJobs() {  
        try {  
            Scheduler sched = schedulerFactory.getScheduler();  
            sched.start();  
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    }  

    /** 
     * @Description:关闭所有定时任务 
     */  
    public static void shutdownJobs() {  
        try {  
            Scheduler sched = schedulerFactory.getScheduler();  
            if (!sched.isShutdown()) {  
                sched.shutdown();  
            }  
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        }  
    } 
}
