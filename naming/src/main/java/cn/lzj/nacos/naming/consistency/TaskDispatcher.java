package cn.lzj.nacos.naming.consistency;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.naming.cluster.Server;
import cn.lzj.nacos.naming.config.NetConfig;
import cn.lzj.nacos.naming.core.Instances;
import cn.lzj.nacos.naming.core.ServiceManager;
import cn.lzj.nacos.naming.misc.GlobalExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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

    @Autowired
    private Redisson redisson;

    @Autowired
    private ServiceManager serviceManager;

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

//        private CountDownLatch countDownLatch;
//        public TaskScheduler(CountDownLatch countDownLatch){
//            this.countDownLatch=countDownLatch;
//        }

        private BlockingQueue<String> queue = new LinkedBlockingQueue<>(128 * 1024);

        public void addTask(String key) {
            //这个key不是表示啥，只是为了表示有数据进来了
            queue.offer(key);
        }

        @Override
        public void run() {
            while(true){
                String lockKey="lockKey";
                RLock redissonLock=redisson.getLock(lockKey);
                try{

                    //获取并移出队列的头元素，若队列为空，则返回null,2s超时时间
                    String key = queue.poll(Constants.TASK_DISPATCH_PERIOD, TimeUnit.MILLISECONDS);


                    //没有实例新增或删除，不用同步，跳过
                    //dataSize>0证明还没达到批量同步的数据或者刚同步的时间差少于2s，所以没进行同步,所以>0时还是需要同步
                    if (StringUtils.isBlank(key)&&dataSize<=0) {
                        continue;
                    }
                    //没有健康的server了，跳过
                    if(dataSyncer.getServers()==null||dataSyncer.getServers().isEmpty()){
                        continue;
                    }

                    //同时只能一个server同步数据
                    redissonLock.lock();
                    //这里拿到的实例数据是暂时来说最新的，假如有两个server，两个client，
                    // client1向server1注册，server1的注册表有client1，client2向server2注册，server2的注册表有client2
                    // 假设server1先拿到锁，server2收到server1发来的集群同步消息，注册表这时有client1，client2
                    //然后server2拿到锁，再同步实例信息，这时server1的注册表也有client和client2了

                    Map<String, Instances> dataMap = consistencyService.getInstances();
                    dataSize++;

                    String serverAddr=netConfig.getServerIp()+ Constants.IP_PORT_SPLITER+netConfig.getServerPort();

                    //异步批量处理
                    //新增的实例数或者删除的实例数(即发生改变的实例数)达到100时才会进行同步或者距离上次同步超过2s才会同步
                    //这里也防止了不断死循环的集群同步，但这里也有问题，如果同时有多个实例来注册到这个server，上次刚同步完，还没到2s，这时再同步的请求就被阻断了，这样集群一致性就不能保证了
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
                    log.error("同步数据失败:", e);
                }finally {
                    redissonLock.unlock();
                }
            }
        }
    }
}
