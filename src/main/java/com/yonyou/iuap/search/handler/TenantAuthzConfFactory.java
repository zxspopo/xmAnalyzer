package com.yonyou.iuap.search.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.solr.common.SolrException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zengxs on 2016/9/20.
 */
public class TenantAuthzConfFactory {

    private static final Logger logger = LoggerFactory.getLogger(TenantAuthzConfFactory.class);

    private static final String BASE_PATH = "/iuap/searchAuthz";

    private static volatile CuratorFramework client;

    private static final Map<String, String> queryConditionMap = new ConcurrentHashMap<>();

    public static String createQuery(String q, String authzValue, String tenantId, String systemCode) {
        String authzQuery = getQueryCondition(tenantId, systemCode);
        if (StringUtils.isBlank(authzQuery)) {
            throw new SolrException(SolrException.ErrorCode.FORBIDDEN, "Not found authz query condition by tenant:"
                    + tenantId + " and systemCode:" + systemCode);
        }
        return q + " AND " + authzQuery.replace(AuthzParams.IUAP_AUTHZ_QUERY_KEY, authzValue);
    }

    private static String getQueryCondition(String tenantId, String systemCode) {
        String key = createKey(tenantId, systemCode);
        String condition = queryConditionMap.get(key);
        if (StringUtils.isBlank(condition)) {
            condition = getConf(key);
            if (StringUtils.isNotBlank(condition)) {
                queryConditionMap.put(key, condition);
            }
        }
        return condition;
    }

    private static String createKey(String tenantId, String systemCode) {
        return BASE_PATH + "/" + tenantId + "/" + systemCode;
    }

    private static String getConf(String key) {
        client = createClient();
        try {
            Stat stat = client.checkExists().forPath(key);
            if (stat != null) {
                byte[] data = client.getData().forPath(key);
                return new String(data);
            }

        } catch (Exception e) {
            logger.error("Fail to get data from path " + key, e);
        }
        return null;
    }

    private static CuratorFramework createClient() {
        if (client == null) {
            synchronized (TenantAuthzConfFactory.class) {
                if (client == null) {
                    client =
                            CuratorFrameworkFactory.builder().connectionTimeoutMs(15000)
                                    .connectString(System.getProperty("zkHost"))
                                    .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, 1000)).build();
                    client.start();
                }
            }
        }
        return client;
    }

    public static void init() {
        client = createClient();
        TreeCache treeCache = new TreeCache(client, BASE_PATH);
        // 设置监听器和处理过程
        treeCache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                ChildData data = event.getData();
                if (data != null) {
                    String path = data.getPath();
                    String dataValue = new String(data.getData());
                    switch (event.getType()) {
                        case NODE_ADDED:
                            logger.info("NODE_ADDED :{}, data:{} ", path, dataValue);
                            queryConditionMap.put(path, dataValue);
                            break;
                        case NODE_REMOVED:
                            logger.info("NODE_REMOVED :{}, data:{} ", path, dataValue);
                            queryConditionMap.remove(path);
                            break;
                        case NODE_UPDATED:
                            logger.info("NODE_UPDATED :{}, data:{}", path, dataValue);
                            queryConditionMap.put(path, dataValue);
                            break;
                        default:
                            break;
                    }
                } else {
                    logger.warn("data is null : " + event.getType());
                }
            }
        });
        // 开始监听
        try {
            treeCache.start();
        } catch (Exception e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Fail to add listener to path " + BASE_PATH,
                    e);
        }
    }
}
