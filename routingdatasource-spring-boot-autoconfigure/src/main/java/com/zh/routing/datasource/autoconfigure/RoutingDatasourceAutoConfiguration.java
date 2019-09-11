package com.zh.routing.datasource.autoconfigure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zh.common.datasource.RoutingDataSource;
import com.zh.common.datasource.RoutingDataSourceHolder;
import com.zh.common.datasource.RoutingDataSourceTransactionManager;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnClass({SqlSessionFactory.class, RoutingDataSource.class, HikariDataSource.class})
@EnableTransactionManagement
public class RoutingDatasourceAutoConfiguration {

    private final ConfigurableApplicationContext applicationContext;

    public RoutingDatasourceAutoConfiguration(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    @Autowired
    private Environment environment;

    @Bean
    public PlatformTransactionManager platformTransactionManager(@Qualifier("routingDataSource") DataSource routingDataSource) {
        return new RoutingDataSourceTransactionManager(routingDataSource);
    }

    @Bean(name = "masterDataSource")
    @Primary
    @ConditionalOnProperty(value = "spring.datasource.encypt", havingValue = "false", matchIfMissing = true)
    @ConfigurationProperties("spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "slaveDataSource")
    @ConditionalOnProperty(value = "spring.datasource.encypt", havingValue = "false", matchIfMissing = true)
    public Map<String, DataSource> slaveDataSource() {
        List<HikariConfig> hikariConfigList = Binder.get(environment).bind("spring.datasource.slave", Bindable.listOf(HikariConfig.class)).get();
        return initSlaveDataSourceList(hikariConfigList);
    }

    @Bean(name = "masterDataSource")
    @Primary
    @ConditionalOnProperty(value = "spring.datasource.encypt", havingValue = "true")
    public DataSource masterDataSourceEncypt() {
        EncryptionHikariConfig encryptionHikariConfig = Binder.get(environment).bind("spring.datasource.master", Bindable.of(EncryptionHikariConfig.class)).get();
        return new HikariDataSource(encryptionHikariConfig);
    }


    @Bean(name = "slaveDataSource")
    @ConditionalOnProperty(value = "spring.datasource.encypt", havingValue = "true")
    public Map<String, DataSource> slaveDataSourceEncypt() {
        List<EncryptionHikariConfig> encryptionHikariConfigList = Binder.get(environment).bind("spring.datasource.slave", Bindable.listOf(EncryptionHikariConfig.class)).get();
        return initSlaveDataSourceList(encryptionHikariConfigList);
    }

    private <T extends HikariConfig> Map<String, DataSource> initSlaveDataSourceList(List<T> hikariConfigList) {
        RoutingDataSourceHolder.SLAVE_DATASOURCE_COUNT = hikariConfigList.size();
        Map<String, DataSource> slaveDataSource = new HashMap<>();
        for (int i = 0; i < hikariConfigList.size(); i++) {
            HikariConfig hikariConfig = hikariConfigList.get(i);
            String dataSourceName = RoutingDataSourceHolder.SLAVE_DATASOURCE_NAME_PREFIX + i;
            //slaveDataSource.put(dataSourceName, new HikariDataSource(hikariConfig));
            //将数据源动态注册进Spring上下文中，否则spring无法管理
            slaveDataSource.put(dataSourceName, registerBean(dataSourceName, HikariDataSource.class, hikariConfig));
        }
        return slaveDataSource;
    }

    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(@Qualifier("masterDataSource") DataSource masterDataSource,
                                        @Qualifier("slaveDataSource") Map<String, DataSource> slaveDataSource) throws Exception {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(RoutingDataSourceHolder.MASTER_DATASOURCE_NAME, masterDataSource);
        targetDataSources.putAll(slaveDataSource);
        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        return routingDataSource;
    }

    /**
     * 动态注册bean
     * 这个方法应该放入SpringContextUtil 中
     *
     * @param name
     * @param clazz
     * @param args
     * @param <T>
     * @return
     */
    private <T> T registerBean(String name, Class<T> clazz, Object... args) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        if (args.length > 0) {
            for (Object arg : args) {
                beanDefinitionBuilder.addConstructorArgValue(arg);
            }
        }
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition(name, beanDefinition);
        return applicationContext.getBean(name, clazz);
    }

}
