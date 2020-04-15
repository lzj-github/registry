package cn.lzj.nacos.naming.utils;

import cn.lzj.nacos.api.common.Constants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.net.InetAddress;
import java.util.*;

public class SystemUtils {

    public static void main(String[] args) throws IOException {
        System.out.println(readClusterConf());
        System.out.println(mappingMap);
    }

    private static BufferedReader bufferedReader;

    //serverIp与nettyIP得映射关系
    public static Map<String, String> mappingMap = new HashMap<>();

    public static List<String> readClusterConf() throws IOException {
        List<String> serversList = new ArrayList<String>();
        List<String> lines = new ArrayList<String>();
        try {
            //获取resource目录下的文件路径
            Resource resource = new ClassPathResource("conf/cluster.conf");
            File file = resource.getFile();
            Reader reader = new InputStreamReader(new FileInputStream(file));
            bufferedReader = new BufferedReader(reader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line.trim());
            }
            String comment = "#";
            for (String line1 : lines) {
                String instance = line1.trim();
                if (instance.startsWith(comment)) {
                    //注释跳过   如 # this is ip list
                    continue;
                }
                if (instance.contains(comment)) {
                    // 192.168.153.128:8848 # Instance A
                    instance = instance.substring(0, instance.indexOf(comment));
                    instance = instance.trim();
                }

                // 格式： 192.168.153.1:9000,192.168.153.1:9001
                String serverIp = instance.split(",")[0];
                String nettyServerIp = instance.split(",")[1];
                if(serverIp.startsWith("localhost")){
                    serverIp= InetAddress.getLocalHost().getHostAddress().toString()+serverIp.substring("localhost".length());
                }
                if(nettyServerIp.startsWith("localhost")){
                    nettyServerIp= InetAddress.getLocalHost().getHostAddress().toString()+nettyServerIp.substring("localhost".length());
                }
                serversList.add(serverIp);
                mappingMap.put(serverIp, nettyServerIp);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            bufferedReader.close();
        }

        return serversList;
    }


}
