package com.zh.routing.datasource.autoconfigure;

import com.zaxxer.hikari.HikariConfig;

/**
 * @ProjectName: zh-project
 * @Package: com.zh.common.datasource.config
 * @ClassName: EncryptionHikariConfig
 * @Author: zh
 * @Description: 数据库密码加密
 * @Date: 2019/8/22 10:03
 * @Version: 1.0
 */
public class EncryptionHikariConfig extends HikariConfig {

    /**
     * 这里可以处理密码解密
     *
     * @param username
     */
    @Override
    public void setUsername(String username) {
        super.setUsername(username);
    }

    /**
     * 这里可以处理密码解密
     *
     * @param password
     */
    @Override
    public void setPassword(String password) {
        super.setPassword(password);
    }
}