package cn.lzj.nacos.naming.consistency;

import cn.lzj.nacos.naming.core.Instances;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SyncTask {

    private Map<String, Instances> dataMap=new HashMap<>();

    private int retryCount;

    private long lastExecuteTime;

    private String targetServer;



}
