package cn.lzj.nacos.naming.consistency;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.naming.cluster.Server;
import cn.lzj.nacos.naming.config.NetConfig;
import cn.lzj.nacos.naming.core.Instances;
import cn.lzj.nacos.naming.misc.GlobalExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TaskDispatcher {

    @Autowired
    private DataSyncer dataSyncer;

    @Autowired
    private ConsistencyService consistencyService;

    @Autowired
    private NetConfig netConfig;

    public volatile TaskScheduler taskScheduler=new TaskScheduler();

    private int dataSize = 0;

    //上次同步的时间
    private long lastDispatchTime = 0L;

    @PostConstruct
    public void init() {
        GlobalExecutor.submitTaskDispatch(taskScheduler);
    }

    public void addTask(String key) {
        taskScheduler.addTask(key);
    }

    class TaskScheduler implements Runnable{

        private BlockingQueue<String> queue = new LinkedBlockingQueue<>(128 * 1024);

        public void addTask(String key) {
            queue.offer(key);
        }

        @Override
        public void run() {
            while(true){
                try{

                    //获取并移出队列的头元素，若队列为空，则返回null,2s超时时间
                    String key = queue.poll(Constants.TASK_DISPATCH_PERIOD, TimeUnit.MILLISECONDS);
                    Map<String, Instances> dataMap = consistencyService.getInstances();

                    //没有实例新增或删除，不用同步，跳过
                    if (StringUtils.isBlank(key)) {
                        continue;
                    }
                    //没有健康的server了，跳过
                    if(dataSyncer.getServers()==null||dataSyncer.getServers().isEmpty()){
                        continue;
                    }
                    dataSize++;

                    String serverAddr=netConfig.getServerIp()+ Constants.IP_PORT_SPLITER+netConfig.getServerPort();

                    //新增的实例数或者删除的实例数(即发生改变的实例数)达到100时才会进行同步或者距离上次同步超过2s才会同步
                    if(dataSize==Constants.BATCH_SYNC_KEY_COUNT||(System.currentTimeMillis()-lastDispatchTime)>Constants.TASK_DISPATCH_PERIOD){
                        for(Server member:dataSyncer.getServers()){
                            //跳过自己
                            if(serverAddr.equals(member.getKey())){
                                continue;
                            }
                            SyncTask syncTask=new SyncTask();
                            syncTask.setDataMap(dataMap);
                            syncTask.setTargetServer(member.getKey());
                            log.info("向"+syncTask.getTargetServer()+"开始同步数据:"+syncTask.getDataMap());
                            //开始同步
                            dataSyncer.submit(syncTask,0);

                        }
                        //重置同步时间和要修改的实例数，以便下次重新统计
                        lastDispatchTime=System.currentTimeMillis();
                        dataSize=0;
                    }





                }catch (Exception e){

                }
            }
        }
    }
}
