package cn.lzj.nacos.client.beat;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.BeatInfo;
import cn.lzj.nacos.client.naming.NamingProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*@Component
public class BeatReactor {

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Autowired
    private NamingProxy namingProxy;

    public BeatReactor(){
        scheduledThreadPoolExecutor=new ScheduledThreadPoolExecutor(Constants.DEFAULT_BEAT_THREAD_COUNT);
    }

    public void addBeanInfo(BeatInfo beatInfo) {
        //schedule只是一次性操作
        scheduledThreadPoolExecutor.schedule(new BeatTask(beatInfo),0, TimeUnit.SECONDS);
    }

    class BeatTask implements Runnable {
        BeatInfo beatInfo;

        public BeatTask(BeatInfo beatInfo) {
            this.beatInfo = beatInfo;
        }

        @Override
        public void run() {
            namingProxy.sendBeat(beatInfo);
            //每隔5秒发送一次心跳
            scheduledThreadPoolExecutor.schedule(new BeatTask(beatInfo),beatInfo.getPeriod(), TimeUnit.SECONDS);
        }
    }
}*/
