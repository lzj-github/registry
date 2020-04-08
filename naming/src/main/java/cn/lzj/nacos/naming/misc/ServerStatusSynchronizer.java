package cn.lzj.nacos.naming.misc;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ServerStatusSynchronizer implements Synchronizer {


    @Override
    public void send(final String serverIP, Message msg) {

        Map<String,String> params=new HashMap<>(2);
        params.put("serverStatus",msg.getData());
        System.out.println(params);
        String url="http://"+serverIP+"/server/status";
        System.out.println(url);
        try{
            HttpClientUtils.asyncHttpGet(url, null, params, new AsyncCompletionHandler() {
                @Override
                public Object onCompleted(Response response) throws Exception {
                    //回调函数
                    if(response.getStatusCode()!=HttpURLConnection.HTTP_OK){
                        log.warn("发送server状态失败,要发送给的server:"+serverIP);
                        return 1;
                    }
                    log.info("发送server状态成功,要发送给的server:"+serverIP);
                    return 0;
                }
            });

        }catch (Exception e){
            log.warn("发送server状态失败,要发送给的server:"+serverIP,e);
        }

    }

    @Override
    public Message get(String serverIP, String key) {
        return null;
    }
}
