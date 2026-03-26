package com.snowcattle.game.common.template;

/**
 * Created by jiangwenping on 17/4/10.
 */
public final class Test {

    @org.junit.Test
    public void legacyMain() {
//        ChileTemplate<String> chileTemplate = new ChileTemplate<String>();
        ChileTemplate chileTemplate = new ChileTemplate();
        System.out.println(chileTemplate.getTClass(0));

        UserDao dao = new UserDao();
    }
}
