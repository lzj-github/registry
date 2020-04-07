package cn.lzj.nacos.naming.core;

import cn.lzj.nacos.api.common.Constants;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class GlobalExecutor {

    private static ScheduledExecutorService executorService =
            new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setName("naming.timer");
                    return t;
                }
            });

    private static final ScheduledExecutorService SERVER_STATUS_EXECUTOR
            = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("naming.status.worker");
            t.setDaemon(true);
            return t;
        }
    });

    private static ScheduledExecutorService notifyServerListExecutor =
            new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setName("naming.server.list.notifier");
                    return t;
                }
            });

    private static ScheduledExecutorService taskDispatchExecutor =
            new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setName("naming.distro.task.dispatcher");
                    return t;
                }
            });

    //3分钟执行一次，如果上一个任务没有执行完毕，则需要等上一个任务执行完毕后立即执行，周期性执行任务。
    public static void registerServerListUpdater(Runnable runnable) {
        executorService.scheduleAtFixedRate(runnable, 0, Constants.SERVER_LIST_REFRESH_INTERVAL, TimeUnit.MINUTES);
    }

    //30s执行一次server的心跳状态报告
    public static void registerServerStatusReporter(Runnable runnable, int delay) {
        SERVER_STATUS_EXECUTOR.schedule(runnable, delay, TimeUnit.SECONDS);
    }

    //通知其他节点集群列表更改了
    public static void notifyServerListChange(Runnable runnable) {
        notifyServerListExecutor.submit(runnable);
    }

    //通知集群里的其他节点实例增加或删除了
    public static void submitTaskDispatch(Runnable runnable) {
        taskDispatchExecutor.submit(runnable);
    }

    public static void submit(Runnable runnable) {
        executorService.submit(runnable);
    }
}
