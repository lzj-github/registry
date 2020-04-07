package cn.lzj.nacos.naming.utils;

import cn.lzj.nacos.api.common.Constants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SystemUtils {

    public static void main(String[] args) throws IOException {
        System.out.println(readClusterConf());
    }
    private static BufferedReader bufferedReader;

    public static List<String> readClusterConf() throws IOException {
        List<String> serversList = new ArrayList<String>();
        List<String> lines = new ArrayList<String>();
        try {
            //获取resource目录下的文件路径
            Resource resource = new ClassPathResource("conf/cluster.conf");
            File file = resource.getFile();
            Reader reader=new InputStreamReader(new FileInputStream(file));
            bufferedReader=new BufferedReader(reader);
            String line=null;
            while((line=bufferedReader.readLine())!=null){
                lines.add(line.trim());
            }
            String comment = "#";
            for(String line1:lines){
                String instance=line1.trim();
                if(instance.startsWith(comment)){
                    //注释跳过   如 # this is ip list
                    continue;
                }
                if(instance.contains(comment)){
                    // 192.168.153.128:8848 # Instance A
                    instance=instance.substring(0,instance.indexOf(comment));
                    instance=instance.trim();
                }
                int multiIndex = instance.indexOf(Constants.COMMA_DIVISION);
                if (multiIndex > 0) {
                    // 格式： ip1:port,ip2:port  # multi inline
                    serversList.addAll(Arrays.asList(instance.split(Constants.COMMA_DIVISION)));
                } else {
                    // 格式： 192.168.153.128:8848
                    serversList.add(instance);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            bufferedReader.close();
        }

        return serversList;
    }


}
