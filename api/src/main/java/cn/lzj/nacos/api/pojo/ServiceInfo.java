package cn.lzj.nacos.api.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ServiceInfo {

    //服务的名字
    private String name;

    private String clusters;

    //该服务下的实例列表
    private List<Instance> instances = new ArrayList<Instance>();
}
