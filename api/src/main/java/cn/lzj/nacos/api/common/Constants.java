package cn.lzj.nacos.api.common;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Constants {

    //默认命名空间
    public static final String DEFAULT_NAMESPACE= "36a3c1fa-646a-4bec-ab10-b0427cfe4278";

    //默认组名
    public static final String DEFAULT_GROUP = "DEFAULT_GROUP";

    //发送心跳间隔时间
    public static final int DEFAULT_SEND_HEART_BEAT_INTERVAL = 5;

    //接收心跳间隔时间
    public static final int DEFAULT_RECEIVED_HEART_BEAT_INTERVAL = 6;

    //心跳线程池的默认线程数
    public static final int DEFAULT_BEAT_THREAD_COUNT = 2;

    //定义一个协议字符串的长度
    public static final int PROTOCOL_LEN=4;

    //心跳消息的前后缀
    public static final String BEAT_ROUND="[bt]";

    //发送断开连接消息的前后缀
    public static final String DISCONNECT_ROUND="[dt]";

    //发送注册消息的前后缀
    public static final String REGISTER_SERVICE_ROUND="[rr]";

    //发送服务发现消息的前后缀
    public static final String SERVICE_FOUND_ROUND="[se]";

    //集群文件的分隔符
    public static String COMMA_DIVISION = ",";

    //ip和端口的分割符
    public static String IP_PORT_SPLITER=":";

    //每隔3分钟读取配置文件cluster.conf,刷新一次集群列表
    public static final long SERVER_LIST_REFRESH_INTERVAL = 3;

    //每隔15s获取服务端最新的注册数据，把数据设置到客户端的缓存map中
    public static final long SERVICE_FOUND_REFRESH_INTEEVAL = 15;

    //超过15s没收到server集群间的消息，就认为该server过期了
    public static long SERVER_EXPIRED_MILLS = TimeUnit.SECONDS.toMillis(15);

    //server每隔3秒向集群里的其他server节点发送一次心跳
    public static int SERVER_STATUS_SYNCHRONIZATION_PERIOD_MILLIS = 3;

}
