package cn.lzj.nacos.naming.misc;

import cn.lzj.nacos.naming.misc.Message;

public interface Synchronizer {

    /**
     * 向服务器发送消息
     */
    void send(String serverIP, Message msg);

    /**
     * 使用消息密钥从服务器获取消息
     */
    Message get(String serverIP, String key);
}
