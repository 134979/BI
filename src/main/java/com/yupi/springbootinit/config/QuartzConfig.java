package com.yupi.springbootinit.config;

import com.yupi.springbootinit.quartz.ChartJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {
    @Bean
    public JobDetail jobDetail() {
        return JobBuilder.newJob(ChartJob.class) //具体任务类
                //给 JobDetail 起一个 id, 不写也会自动生成唯一的 TriggerKey
                .withIdentity("chartJobDetail")
                //JobDetail 内部的一个 map, 可以存储有关 Job 的数据, 这里的数据
                // 可通过 Job 类中executeInternal方法的参数进行获取
                .usingJobData("job_chart","getChart")
                .storeDurably()  //即使没有Trigger关联时也不删除该Jobdetail
                .build();
    }
    //构建 Trigger 及 Scheduler
    @Bean
    public Trigger jobTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail())
                .withIdentity("chartTrigger")
                .usingJobData("trigger_chart","getChart")
                .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(5))
                .build();
    }

}
