package cn.lzj.nacos.naming.core;

import cn.lzj.nacos.api.pojo.Instance;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Instances {

    private List<Instance> instanceList = new ArrayList<>();
}
