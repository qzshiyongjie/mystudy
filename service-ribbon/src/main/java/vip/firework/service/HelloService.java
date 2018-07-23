package vip.firework.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HelloService {

    @Autowired
    RestTemplate restTemplate;
//    @HystrixCommand(fallbackMethod = "hiError",//发生错误时的回调地址
//            threadPoolKey = "licenseByOrgThreadPool",
//            threadPoolProperties ={//线程池相关属性
//                    @HystrixProperty(name = "coreSize",value="30"),//设置线程池的大小
//                    @HystrixProperty(name="maxQueueSize",value="10"),//将在线程池前面设置的最大队列大小。如果设置为-1，没有采用队列而 Hystrix 将阻塞直到一个线程变为可用的处理。
//            },
//            commandProperties ={
//            @HystrixProperty(
//                    name="circuitBreaker.requestVolumeThreshold",//必须在滚劢窗口内 Hystrix 将开始检查断路器是否会跳闸前设置请求的最小数量。
//                     value="10"),
//             @HystrixProperty(
//            name="circuitBreaker.errorThresholdPercentage",//在断路器跳闸前必须在滚劢窗口内发生的故障百分比
//             value="75"),
//            @HystrixProperty(
//                    name="circuitBreaker.sleepWindowInMilliseconds",//在服务调用前，断路器已跳闸后 Hystrix 将等待的毫秒数。
//                    value="7000"),
//             @HystrixProperty(
//            name="metrics.rollingStats.timeInMilliseconds",//Hystrix 在窗口中将收集和监控统计服务调用的毫秒数。
//            value="15000"),
//            @HystrixProperty(
//            name="metrics.rollingStats.numBuckets", value="5")//Hystrix 将在它的监控窗口内维护的度量同数量。监视窗口中的桶越多，时间越低，Hystrix 将会监控窗口内的错诨。
//            }
//        )
    @HystrixCommand(fallbackMethod = "hiError")
    public String hiService(String name) {
        return restTemplate.getForObject("http://SERVICE-HI/hi?name="+name,String.class);
    }
    public String hiError(String name) {
        return "hi,"+name+",sorry,error!";
    }
}
