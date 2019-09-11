package com.zh.mybatis.plugin.autoconfigure;

import com.zh.common.mybatis.plugin.RoutingDatasourcePlugin;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({SqlSessionFactory.class, RoutingDatasourcePlugin.class})
@AutoConfigureAfter({MybatisAutoConfiguration.class})
public class RoutingDatasourcePluginAutoConfiguration {
    @Bean
    public RoutingDatasourcePlugin sqlStatsInterceptor() {
        RoutingDatasourcePlugin sqlStatsInterceptor = new RoutingDatasourcePlugin();
        return sqlStatsInterceptor;
    }
}
