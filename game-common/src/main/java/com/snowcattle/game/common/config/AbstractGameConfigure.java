package com.snowcattle.game.common.config;

import com.snowcattle.game.common.constant.Loggers;
import org.slf4j.Logger;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by jiangwenping on 17/3/30.
 * 获取properties配置文件
 */
public abstract class AbstractGameConfigure implements GameConfigurable{

    protected static Logger logger = Loggers.adminLogger;
    private Properties properties;
    private File configFile;
    private Resource resource;
    private final Object lock=new Object();
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public void init(){
        try {
            properties = new Properties();
            // 支持 jar 内资源读取：不要依赖 resource.getFile()（jar 内路径无法当作真实文件打开）
            try (InputStream in = resource.getInputStream()) {
                properties.load(in);
            }
            logger.info("加载配置文件:"+resource.getFilename()+"成功.");
        } catch (IOException e) {
            //这里需要记录日志
            logger.error("加载配置文件:"+resource.getFilename()+"失败.");
        }
    }

    public void reload(){
        synchronized (lock) {
            try {
                // 不依赖 getFile()，直接重新读取 resource stream
                try (InputStream in = resource.getInputStream()) {
                    properties.load(in);
                }
                logger.info("reload配置文件:"+resource.getFilename()+"成功.");
            }catch (Exception e) {
                logger.error("reload配置文件:"+resource.getFilename()+"失败.");
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getProperty(String key, String defaultVal) {
        String v = getProperty(key);
        if(v==null){
            return defaultVal;
        }
        return v;
    }

    @Override
    public int getProperty(String key, int defaultVal) {
        String v = getProperty(key);
        if(v==null){
            return defaultVal;
        }
        Integer i = Integer.parseInt(v);
        return i.intValue();
    }

    public String getProperty(String key){
        if(key==null){
            return null;
        }
        String v = properties.getProperty(key);
        if(v==null||v.isEmpty()){
            return null;
        }
        return v.trim();
    }

    @Override
    public boolean getProperty(String key, boolean defaultVal) {
        String v = getProperty(key);
        if(v==null){
            return defaultVal;
        }
        boolean result = Boolean.parseBoolean(v);
        return result;
    }

    @Override
    public long getProperty(String key, long defaultVal) {
        String v = getProperty(key);
        if(v==null){
            return defaultVal;
        }
        Long i = Long.parseLong(v);
        return i.intValue();
    }
}
