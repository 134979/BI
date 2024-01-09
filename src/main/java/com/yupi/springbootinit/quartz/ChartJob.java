package com.yupi.springbootinit.quartz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.springbootinit.bizmq.BiMessageProducer;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.Wrapper;
import java.util.List;

@Component
@Slf4j
public class ChartJob extends QuartzJobBean {

    @Resource
    private BiMessageProducer biMessageProducer;

    @Resource
    private ChartService chartService;


    /**
     * 定时扫描数据中失败的任务，重新提交到MQ
     * @param context
     * @throws JobExecutionException
     */
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        QueryWrapper<Chart> queryWrapper=new QueryWrapper<Chart>();
        queryWrapper.eq("status","failed");
        List<Chart> charts = chartService.list(queryWrapper);
        if(charts.isEmpty()){
            return;
        }
        System.out.println("定时检索数据库中失败的图表往Mq发送消息");
        for(Chart chart : charts){
            biMessageProducer.sendMessage(String.valueOf(chart.getId()));
            log.debug("往MQ发送消息成功:"+chart.getId());
        }
    }
}
