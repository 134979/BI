项目介绍：
智能BI系统后端采用springboot+mysql+redis+kafka+AIGC，前端采用React+TypeScript+Echarts可视化库实现。区别于传统BI，用户只需要在前端输入分析诉求以及数据，系统后端调用AI生成对应的分析结论以及Echarts可视化代码在前端呈现对应的可视化图表。

![image](https://github.com/134979/BI/assets/82896512/b9045177-d831-44dc-bc53-f16419f4e0cd)
![image](https://github.com/134979/BI/assets/82896512/2c949891-371b-4531-af65-617ea2b148ff)
系统页面：
同步接口：
![image](https://github.com/134979/BI/assets/82896512/24c76f32-b6d3-4116-b406-991cb55594e6)
同步接口缺点：用户需要等待结果生成，如果AI能力比较弱，需要分析的数据较多，用户要等待的时间较长，体验不好。所以引入消息中间件kafka，将每个任务提交到消息队列中，根据消费者的消费能力进行消费消息，让调用AI接口异步化，不需要等待，结果生成后在我的图表中查看分析结果。如下图：。
![image](https://github.com/134979/BI/assets/82896512/c9a4ace3-78f1-49df-9c56-80ac69273ce9)
图表信息采用Redis进行缓存，减轻MySQL压力，提高系统性能。
![image](https://github.com/134979/BI/assets/82896512/320a0b6f-c811-412a-8cf0-04915f44fb55)


