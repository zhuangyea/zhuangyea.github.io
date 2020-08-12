package com.isoftstone.ismart.analysis.util;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.*;

/**
 * redis工具类
 *
 * @author Colin.Ye
 * @version 1.0
 * @ClassName RedisController
 * @date 2018/1/12
 **/
@Component
public class RedisUtil {

    final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 数据源
     */
    private ShardedJedisPool shardedJedisPool;

    @Value("${datasource.redis.host}")
    private String host;
    @Value("${datasource.redis.port}")
    private int port;

    @Value("${datasource.redis.database}")
    private int db;

    /**
     * 切换redis库
     *
     * @param jedis
     */
    private void setDb(JedisShardInfo jedis) {
        Class<? extends JedisShardInfo> clz = jedis.getClass();
        Field declaredField = null;
        try {
            declaredField = clz.getDeclaredField("db");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        declaredField.setAccessible(true);
        try {
            declaredField.set(jedis, db);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void init() {
        JedisPoolConfig config = new JedisPoolConfig();// Jedis池配置
//        config.setMaxTotal(Integer.MAX_VALUE); // 最大连接数, 默认8个
        config.setMaxIdle(1000 * 60);// 对象最大空闲时间
        config.setMaxWaitMillis(1000 * 20);// 获取对象时最大等待时间
        config.setTestOnBorrow(false);

        List<JedisShardInfo> jdsInfoList = new ArrayList<JedisShardInfo>(1);
        JedisShardInfo infoA = new JedisShardInfo(host, port);
        setDb(infoA);
        jdsInfoList.add(infoA);

        JedisShardInfo infoB = new JedisShardInfo(host, port);
        setDb(infoB);
        jdsInfoList.add(infoB);

        JedisShardInfo infoC = new JedisShardInfo(host, port);
        setDb(infoC);
        jdsInfoList.add(infoC);

        this.shardedJedisPool = new ShardedJedisPool(config, jdsInfoList);
//		jedis = shardedJedisPool.getResource();
    }

    /**
     * 获取数据库连接
     *
     * @return conn
     */
    public ShardedJedis getConnection() {
        ShardedJedis jedis = null;
        try {
            jedis = shardedJedisPool.getResource();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jedis;
    }

    /**
     * 关闭数据库连接
     *
     * @param conn
     */
    public void closeConnection(ShardedJedis jedis) {
        if (null != jedis) {
            try {
//				shardedJedisPool.returnResource(jedis);
                jedis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//	/**
//	 * 获取jedis
//	 * @return
//	 */
//	public ShardedJedis jedis(){
//		return this.jedis;
//	}

    /**
     * 设置数据
     *
     * @param conn
     */
    public boolean setData(String key, String value) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            jedis.set(key, value);
            return true;
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            closeConnection(jedis);
        }
        return false;
    }

    public Long llen(String key) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            Long l = jedis.llen(key);
            return l;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return 0L;
    }

    /**
     * 设置数据
     *
     * @param conn
     */
    public boolean rpush(String key, String value) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            jedis.rpush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return false;
    }

    /**
     * 设置数据
     *
     * @param conn
     */
    public boolean sadd(String key, String value) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            jedis.sadd(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return false;
    }

    /**
     * 获取数据
     *
     * @param conn
     */
    public String getData(String key) {
        String value = null;
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            value = jedis.get(key);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return value;
    }

    public boolean isMember(String key, String member) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            boolean result = jedis.sismember(key, member);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return false;
    }

    public Set<String> smembers(String key) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            Set<String> s = jedis.smembers(key);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return null;
    }

    /**
     * 获取List集合
     *
     * @param key
     * @param clazz
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List getListJsonData(String key, Class clazz) {
        List<String> userList = null;
        List list = new ArrayList();
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            userList = jedis.lrange(key, 0, -1);
            for (String str : userList) {
                list.add(JSON.parseObject(str, clazz));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return list;
    }

    /**
     * 使用rpush方式插入List集合
     *
     * @param key
     * @param clazz
     * @return
     */
    public boolean rpushJsonData(String key, Object o) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            jedis.rpush(key, JSON.toJSONString(o));
            logger.info(o.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            closeConnection(jedis);
        }
        return false;
    }

    /**
     * 存储map格式数据（map的key和value必须是String）
     *
     * @param key
     * @return
     */
    public boolean hmset(String key, Map<String, String> params) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            jedis.hmset(key, params);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return false;
    }

    /**
     * 查询map格式数据（map的key和value必须是String）
     *
     * @param key
     * @param obj 可以为LIST、SET、数组、字符串
     * @return 返回list集合
     */
    public List<Map<String, String>> hmget(String key, Object obj) {
        ShardedJedis jedis = getConnection();
        String[] array = {};
        String str = null;
        if (obj instanceof String) {
            str = obj.toString().replace(" ", "");
            array = str.split(",");
        } else if (obj instanceof List || obj instanceof Set) {
            str = obj.toString().replace(" ", "");
            str = str.substring(1, str.length() - 1);
            array = str.split(",");
        } else if (obj instanceof String[]) {
            array = (String[]) obj;
        }
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        List<String> cities = jedis.hmget(key, array);
        for (int i = 0; i < array.length; i++) {
            Map<String, String> map = new HashMap<String, String>();
            map.put(array[i], cities.get(i));
            list.add(map);
        }
        closeConnection(jedis);
        return list;
    }

    /**
     * 查询map格式数据（返回所有map）
     *
     * @param key
     * @return 返回list集合
     */
    public Map<String, String> hgetAll(String key) {
        ShardedJedis jedis = getConnection();
        Map<String, String> map = new HashMap<String, String>();
        try {
            map = jedis.hgetAll(key);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return map;
    }

    public boolean setJsonData(String key, Object o) {
        ShardedJedis jedis = null;
        try {
            jedis = shardedJedisPool.getResource();
            jedis.set(key, JSON.toJSONString(o));
            logger.info(o.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            closeConnection(jedis);
        }
        return false;
    }

    @SuppressWarnings("all")
    public Object getJsonData(String key, Class clazz) {
        String value = null;
        ShardedJedis jedis = null;
        try {
            jedis = shardedJedisPool.getResource();
            value = jedis.get(key);
//			shardedJedisPool.returnResource(jedis);
            return JSON.parseObject(value, clazz);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return null;
    }

    public String lpop(final String key) {
        String value = null;
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            value = jedis.lpop(key);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return value;
    }

    public Boolean sismember(final String key, final String member) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            Boolean value = jedis.sismember(key, member);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return false;
    }

    public Long zadd(final String key, int score, String member) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            Long l = jedis.zadd(key, score, member);
            return l;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }

        return 0L;

    }

    public Set<String> zrevrange(final String key, int start, int end) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            Set<String> s = jedis.zrevrange(key, start, end);
            return s;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return null;
    }

    public String zrevrangeByscore(final String key) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            Set<String> s = jedis.zrevrangeByScore(key, "+inf", "-inf");
            Iterator<String> it = s.iterator();
            while (it.hasNext()) {
                return (String) it.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return null;

    }

    public Set<Tuple> zrevrangeWithScores(final String key, int start, int end) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            Set<Tuple> set = jedis.zrevrangeWithScores(key, start, end);
            return set;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return null;
    }

    public int zincrby(final String key, int score, String member) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            Double d = jedis.zincrby(key, score, member);
            return d.intValue();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return 0;
    }

    public int zscore(final String key, String member) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            Double d = jedis.zscore(key, member);
            if (d == null) {
                return 0;
            }
            return d.intValue();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(jedis);
        }
        return 0;
    }

    /**
     * 删除数据
     *
     * @param key
     * @return
     */
    public long del(String key) {
        ShardedJedis jedis = null;
        try {
            jedis = getConnection();
            long count = jedis.del(key);
            return count;
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            closeConnection(jedis);
        }
        return 0;
    }

    /**
     * 设置过期时间
     *
     * @param key
     * @param seconds 秒
     * @return
     */
    public boolean expire(String key, int seconds) {
        ShardedJedis jedis = null;
        try {
            jedis = shardedJedisPool.getResource();
            jedis.expire(key, seconds);
            return true;
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            closeConnection(jedis);
        }
        return false;
    }


    /**
     * 设置连接池
     *
     * @return 数据源
     */
    public void setShardedJedisPool(ShardedJedisPool shardedJedisPool) {
        this.shardedJedisPool = shardedJedisPool;
    }

    /**
     * 获取连接池
     *
     * @return 数据源
     */
    public ShardedJedisPool getShardedJedisPool() {
        return shardedJedisPool;
    }
}
