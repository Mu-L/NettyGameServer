package com.snowcattle.game.db.service.jdbc.test.stringEntity.onetoone;

import com.snowcattle.game.db.service.jdbc.entity.Tocken;
import com.snowcattle.game.db.service.jdbc.service.entity.impl.TockenService;
import com.snowcattle.game.db.service.jdbc.test.TestConstants;
import com.snowcattle.game.db.service.proxy.EntityProxyFactory;
import org.junit.Assert;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiangwenping on 17/3/20.
 * <p>
 * {@link #legacyMain()} 会执行 {@link #insertTest}，需本机 MySQL：库 {@code db_0}、{@code db_1}、{@code db_2} 及分表（见 {@code src/test/resources/sql/init.sql}）。
 * 数据源仅使用 {@code game-spring-xml-placeholder.properties} 中的 MySQL 配置，不使用 H2。
 */
public class JdbcTest {
    @org.junit.Test
    public void legacyMain() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"bean/*.xml"});
        try {
            TockenService svc = getTockenService(ctx);
            Assert.assertNotNull(svc);
//            insertTest(ctx, svc);
//            Tocken t = getTest(ctx, svc);
//            updateTest(ctx, svc, t);
        } finally {
            ctx.close();
        }
    }



    public static void deleteBatchTest(ClassPathXmlApplicationContext classPathXmlApplicationContext, TockenService tockenService, List<Tocken> tockenList) throws Exception {
       //test2
        tockenService.deleteEntityBatch(tockenList);
    }

    public static void updateBatchTest(ClassPathXmlApplicationContext classPathXmlApplicationContext, TockenService tockenService, List<Tocken> tockenList) throws Exception {
        EntityProxyFactory entityProxyFactory = (EntityProxyFactory) classPathXmlApplicationContext.getBean("entityProxyFactory");
        List<Tocken> updateList = new ArrayList<>();
        for (Tocken tocken : tockenList) {
            Tocken proxyTocken = entityProxyFactory.createProxyEntity(tocken);
            proxyTocken.setStatus("dddd");
            proxyTocken.setUserId(TestConstants.userId);
            proxyTocken.setId(tocken.getId());
            updateList.add(proxyTocken);
        }
        tockenService.updateEntityBatch(updateList);
    }

    public static void insertBatchTest(ClassPathXmlApplicationContext classPathXmlApplicationContext, TockenService tockenService) throws Exception {
        int startSize = TestConstants.batchStart;
        int endSize = startSize + 10;
        List<Tocken> list = new ArrayList<>();
        for (int i = startSize; i < endSize; i++) {
            Tocken tocken = new Tocken();
            tocken.setUserId(TestConstants.userId);
            tocken.setId(TestConstants.stringId);
            tocken.setStatus("测试列表插入" + i);
            list.add(tocken);
        }

        tockenService.insertEntityBatch(list);
    }

    public static List<Tocken> getTockenList(ClassPathXmlApplicationContext classPathXmlApplicationContext, TockenService tockenService) throws Exception {
        List<Tocken> tocken = tockenService.getTockenList(TestConstants.userId);
        System.out.println(tocken);
        return tocken;
    }

    public static void insertTest(ClassPathXmlApplicationContext classPathXmlApplicationContext, TockenService tockenService) {

        int startSize = TestConstants.batchStart;
        int endSize = startSize+1;

        for (int i = startSize; i < endSize; i++) {

            Tocken tocken = new Tocken();
            tocken.setUserId(TestConstants.userId);
            tocken.setId(TestConstants.stringId);
            tocken.setStatus("测试插入" + i);
            tockenService.insertTocken(tocken);
        }
    }

    public static Tocken getTest(ClassPathXmlApplicationContext classPathXmlApplicationContext, TockenService tockenService) {
        Tocken tocken = tockenService.getTocken(TestConstants.userId, TestConstants.stringId);
        System.out.println(tocken);
        return tocken;
    }


    public static void updateTest(ClassPathXmlApplicationContext classPathXmlApplicationContext, TockenService TockenService, Tocken tocken) throws Exception {
        EntityProxyFactory entityProxyFactory = (EntityProxyFactory) classPathXmlApplicationContext.getBean("entityProxyFactory");
        Tocken proxyTocken = entityProxyFactory.createProxyEntity(tocken);
        proxyTocken.setStatus("修改了3");
        TockenService.updateTocken(proxyTocken);

        Tocken queryTocken = TockenService.getTocken(TestConstants.userId, TestConstants.stringId);
        System.out.println(queryTocken.getStatus());
    }

    public static void deleteTest(ClassPathXmlApplicationContext classPathXmlApplicationContext, TockenService TockenService, Tocken tocken) throws Exception {
        TockenService.deleteTocken(tocken);
        Tocken queryTocken = TockenService.getTocken(TestConstants.userId, TestConstants.stringId);
        System.out.println(queryTocken);
    }

    public static TockenService getTockenService(ClassPathXmlApplicationContext classPathXmlApplicationContext) {
        TockenService TockenService = (TockenService) classPathXmlApplicationContext.getBean("tockenService");
        return TockenService;
    }
}
