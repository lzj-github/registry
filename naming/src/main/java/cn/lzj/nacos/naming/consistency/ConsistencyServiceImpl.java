package cn.lzj.nacos.naming.consistency;


import cn.lzj.nacos.naming.core.Instances;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@org.springframework.stereotype.Service("consistencyService")
public class ConsistencyServiceImpl implements ConsistencyService {

    //Map<namespaceId+"##"+serviceName,Instances>
    private Map<String, Instances> dataMap = new ConcurrentHashMap<>(1024);

    @Autowired
    private TaskDispatcher taskDispatcher;

    public volatile Notifier notifier = new Notifier();

    //容器,装着观察者即Service
    private Map<String, CopyOnWriteArrayList<RecordListener>> listeners = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        ScheduledExecutorService executorService =
                new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("naming.distro.notifier");
                        t.setDaemon(true);
                        return t;
                    }
                });
        //启动个后台线程来异步更新注册内存表,初始化时启动该线程
        executorService.submit(notifier);
    }


    @Override
    public void listen(String key, RecordListener listener) {
        if(!listeners.containsKey(key)){
            listeners.put(key,new CopyOnWriteArrayList<>());
        }

        if(listeners.get(key).contains(listener)){
            return;
        }
        //添加观察者，listener就是Service实例
        listeners.get(key).add(listener);
    }



    class Notifier implements Runnable{

        private BlockingDeque<Pair> tasks=new LinkedBlockingDeque(1024*1024);

        public void addTask(String key, ApplyAction action){

            //添加到阻塞队列里面去
            tasks.add(Pair.with(key,action));
        }

        @SneakyThrows
        @Override
        public void run() {
            while(true){
                //后台线程从阻塞队列里面取数据，若没有数据就阻塞住了
                Pair pair = tasks.take();
                if(pair==null){
                    continue;
                }
                log.info("后台线程有数据拿出来了:"+pair);
                String key = (String) pair.getValue0();//namespaceId##serviceName
                ApplyAction action = (ApplyAction) pair.getValue1();//CHANGE 或者 DELETE

                if (!listeners.containsKey(key)) {
                    continue;
                }

                for(RecordListener listener:listeners.get(key)){//取出监听者这个key的观察者，调用它们的监听方法
                    if(action==ApplyAction.CHANGE){
                        listener.onChange(key,dataMap.get(key));
                        continue;
                    }
                }


            }
        }
    }

    @Override
    public void put(String key, Instances instances) {
        onPut(key,instances);
    }

    @Override
    public void notifyCluster(String key){
        //通知集群
        taskDispatcher.addTask(key);
    }

    public void onPut(String key, Instances instances) {
        dataMap.put(key,instances);
        //把实例列表添加到后台线程的阻塞队列里面
        notifier.addTask(key,ApplyAction.CHANGE);
    }

    /**
     * 修改完内存注册表再来把当前dataMap修改为最新值
     * @param key
     * @param instances
     */
    @Override
    public void setInstance(String key, Instances instances){
        dataMap.put(key,instances);
    }

    /**
     * 返回实例列表
     * @return
     */
    @Override
    public Map<String, Instances> getInstances(){
        return dataMap;
    }

    /**
     *暂时没用到该方法，因为添加和删除都使用onPut方法了
     * @param key
     */
    @Override
    public void remove(String key)  {
        onRemove(key);
        if(!listeners.containsKey(key)){
            return;
        }
        listeners.remove(key);
    }

    public void onRemove(String key) {
        notifier.addTask(key,ApplyAction.DELETE);
    }
}
