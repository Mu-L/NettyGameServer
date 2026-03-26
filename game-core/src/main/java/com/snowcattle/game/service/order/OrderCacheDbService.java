package com.snowcattle.game.service.order;

import com.snowcattle.game.db.service.entity.EntityService;
import com.snowcattle.game.db.service.jdbc.entity.Order;
import com.snowcattle.game.db.service.proxy.EntityServiceProxyFactory;
import com.snowcattle.game.db.sharding.EntityServiceShardingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Order DB + Redis cache write service.
 */
@Service
@Lazy
public class OrderCacheDbService extends EntityService<Order> {

    @Autowired
    private EntityServiceProxyFactory entityServiceProxyFactory;

    private volatile OrderCacheDbService proxyService;

    public long insertOrderWithCache(long userId, long orderId, String status) throws Exception {
        Order order = new Order();
        order.setUserId(userId);
        order.setId(orderId);
        order.setStatus(status);
        return getProxyService().insertEntity(order);
    }

    private OrderCacheDbService getProxyService() throws Exception {
        if (proxyService == null) {
            synchronized (this) {
                if (proxyService == null) {
                    proxyService = entityServiceProxyFactory.createProxyService(this);
                }
            }
        }
        return proxyService;
    }

    @Override
    public EntityServiceShardingStrategy getEntityServiceShardingStrategy() {
        return getDefaultEntityServiceShardingStrategy();
    }
}
