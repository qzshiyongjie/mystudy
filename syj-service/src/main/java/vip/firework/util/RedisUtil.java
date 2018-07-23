package vip.firework.util;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Resource
    private RedisTemplate<String, ?> redisTemplate;

    private static RedisTemplate<String, ?> staticRedisTemplate;
    @PostConstruct
    public void init(){
        staticRedisTemplate=redisTemplate;
    }

    public static boolean set(final String key, final String value) {
        boolean result = staticRedisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                RedisSerializer<String> serializer = staticRedisTemplate.getStringSerializer();
                connection.set(serializer.serialize(key), serializer.serialize(value));
                return true;
            }
        });
        return result;
    }

    public static String get(final String key){
        String result = staticRedisTemplate.execute(new RedisCallback<String>() {
            public String doInRedis(RedisConnection connection) throws DataAccessException {
                RedisSerializer<String> serializer = staticRedisTemplate.getStringSerializer();
                byte[] value =  connection.get(serializer.serialize(key));
                return serializer.deserialize(value);
            }
        });
        return result;
    }

    public static boolean expire(final String key, long expire) {
        return staticRedisTemplate.expire(key, expire, TimeUnit.SECONDS);
    }

    public static boolean exits(final String key){
        if(get(key)!=null){
            return true;
        }else {
            return false;
        }
    }

}
