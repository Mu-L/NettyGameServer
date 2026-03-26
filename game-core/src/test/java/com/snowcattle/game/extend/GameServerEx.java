package com.snowcattle.game.extend;

import com.snowcattle.game.bootstrap.GameServer;

/**
 * Created by jwp on 2017/5/5.
 */
public class GameServerEx extends GameServer{

    @org.junit.Test
    public void legacyMain() {
        GameServerEx gameServerEx = new GameServerEx();
        GlobalManagerEx globalManagerEx = new GlobalManagerEx();
        gameServerEx.setGlobalManager(globalManagerEx);
        gameServerEx.startServer();
    }
}
