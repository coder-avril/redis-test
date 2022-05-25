package com.lding;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class JedisTest {
    private Jedis jedis = null;

    @Before
    public void init() {
        this.jedis = new Jedis("192.168.0.10", 6379);
    }

    @After
    public void close() {
        if (this.jedis != null) {
            this.jedis.close();
        }
    }

    @Test
    public void stringSetTest() {
        String result = this.jedis.set("test", "hello world");
        if ("OK".equals(result)) {
            System.out.println(this.jedis.get("test"));
            this.jedis.del("test");
        }
    }

    @Test
    public void StringSetPerformanceTest() {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100_000; i++) {
            this.jedis.set("test" + i, "value" + i);
        }
        long endTime = System.currentTimeMillis();
        /* 普通String的set用时: 31秒 */
        System.out.println("普通String的set用时: " + (endTime - startTime)/1000 + "秒");
    }

    @Test
    public void StringSetPipelineTest() {
        long startTime = System.currentTimeMillis();
        Pipeline pipelined = this.jedis.pipelined(); // 创建一个管道
        for (int i = 0; i < 100_000; i++) {
            pipelined.set("test" + i, "value" + i); // 把需要执行的命令放入管道
        }
        pipelined.sync(); // 让管道一次性同步执行命令
        long endTime = System.currentTimeMillis();
        /* Pipeline用时: 0秒 */
        System.out.println("Pipeline用时: " + (endTime - startTime)/1000 + "秒");
    }

    @Test
    public void publishTest() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.jedis.publish("9527", "Hello " + i);
            TimeUnit.MILLISECONDS.sleep(50);
        }
    }

    @Test
    public void subscribeTest() {
        this.jedis.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                System.out.println("channel = " + channel);
                System.out.println("message = " + message);
            }
        }, "9527");
    }

    @Test
    public void bitmapTest() {
        // 记录用户一周的打卡情况
        for (int i = 0; i < 5; i++) {
            // 记录 i 为偶数则出勤：所以是 0 2 4 也就是周一 周三 周五
            String value = i % 2 == 0 ? "1" : "0";
            this.jedis.setbit("user:007", i, value);
        }
        System.out.println("1周出勤总数: " + this.jedis.bitcount("user:007"));
    }

    @Test
    public void hyperloglogTest() {
        String[] numbers1 = {"1", "1", "2", "3", "4"};
        String[] numbers2 = {"3", "4", "5", "6", "6"};
        this.jedis.pfadd("count1", numbers1);
        this.jedis.pfadd("count2", numbers2);
        System.out.println("count1: " + this.jedis.pfcount("count1"));
        System.out.println("count2: " + this.jedis.pfcount("count2"));
        this.jedis.pfmerge("count3", "count1", "count2");
        System.out.println("count3: " + this.jedis.pfcount("count3"));
    }

    @Test
    public void geoTest() {
        this.jedis.geoadd("china:city", this.getLocations());
        // 检查离合肥400km范围内的城市
        List<GeoRadiusResponse> georadius = this.jedis.georadius("china:city",
                117.28, 31.86, 400, GeoUnit.KM);
        for (GeoRadiusResponse response: georadius) {
            System.out.println(new String(response.getMember()));
        }
    }

    private Map<String, GeoCoordinate> getLocations() {
        Map<String, GeoCoordinate> locations = new HashMap<>();
        GeoCoordinate beijing = new GeoCoordinate(116.23,40.22);
        GeoCoordinate shanghai = new GeoCoordinate(121.48, 31.40);
        GeoCoordinate shenzhen = new GeoCoordinate(113.88, 22.55);
        GeoCoordinate hangzhou = new GeoCoordinate(120.21, 30.20);
        GeoCoordinate chongqing = new GeoCoordinate(106.54, 29.40);
        GeoCoordinate xian = new GeoCoordinate(108.93, 34.23);
        GeoCoordinate wuhan = new GeoCoordinate(114.02, 30.58);
        locations.put("beijing", beijing);
        locations.put("shanghai", shanghai);
        locations.put("shenzhen", shenzhen);
        locations.put("hangzhou", hangzhou);
        locations.put("chongqing", chongqing);
        locations.put("xian", xian);
        locations.put("wuhan", wuhan);
        return locations;
    }

    @Test
    public void sentinelTest() {
        String masterName = "mymaster";
        Set<String> sentinels = new HashSet<>();
        sentinels.add("192.168.0.10:26379");
        sentinels.add("192.168.0.11:26379");
        sentinels.add("192.168.0.12:26379");
        // 让客户端连接哨兵（通过哨兵去连redis服务
        JedisSentinelPool pool = new JedisSentinelPool(masterName, sentinels);
        while (true) { // 死循环（期间主动断开主节点 看看效果 是否会重新选出新的主节点 然后继续成功写入
            try(Jedis jedis = pool.getResource()) {
                jedis.incr("age");
                System.out.println("age = " + jedis.get("age"));
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void poolTest() {
        // 1. 创建连接池对象
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(10);
        config.setMinIdle(10);
        config.setMinIdle(10);
        config.setMaxWaitMillis(15_000);
        String host = "192.168.0.10";
        int port = 6379;
        // 这种方法依然是连接单机redis
        JedisPool pool = new JedisPool(config, host, port);
        
        // 2. 获取连接对象
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            jedis.set("hello", "world");
            System.out.println(jedis.get("hello"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}
