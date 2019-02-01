package com.freedom.springcloud.zuul.dao;

import com.alibaba.druid.pool.DruidDataSource;
import com.freedom.springcloud.zuul.common.Constants;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Zuul Filter 的 JDBC Dao Builder
 */
public class JDBCZuulFilterDaoBuilder implements IZuulFilterDaoBuilder {
	private static final Logger logger = LoggerFactory.getLogger(JDBCZuulFilterDaoBuilder.class);

	/** 连接配置 */
	private static final DynamicStringProperty dataSourceClass = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.DATA_SOURCE_CLASS_NAME, null);
	private static final DynamicStringProperty url = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.DATA_SOURCE_URL, null);
	private static final DynamicStringProperty user = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.DATA_SOURCE_USER, null);
	private static final DynamicStringProperty password = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.DATA_SOURCE_PASSWORD, null);

	/** pool配置 */
	private static final DynamicIntProperty minPoolSize = DynamicPropertyFactory.getInstance()
			.getIntProperty(Constants.DATA_SOURCE_MIN_POOL_SIZE, 10);
	private static final DynamicIntProperty maxPoolSize = DynamicPropertyFactory.getInstance()
			.getIntProperty(Constants.DATA_SOURCE_MAX_POOL_SIZE, 20);
	//private static final DynamicLongProperty connectionTimeout = DynamicPropertyFactory.getInstance()
	//		.getLongProperty(Constants.DATA_SOURCE_CONNECT_TIMEOUT, 1000);
	//private static final DynamicLongProperty idleTimeout = DynamicPropertyFactory.getInstance()
	//		.getLongProperty(Constants.DATA_SOURCE_IDLE_TIMEOUT, 600000);
	//private static final DynamicLongProperty maxLifetime = DynamicPropertyFactory.getInstance()
	//		.getLongProperty(Constants.DATA_SOURCE_MAX_LIFETIME, 1800000);

//	private static final DynamicStringProperty environment = DynamicPropertyFactory.getInstance()
//			.getStringProperty(Constants.DEPLOY_ENVIRONMENT, "test");

	private static final DynamicStringProperty filterTableName = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.FILTER_TABLE_NAME, "zuul_filter");
	
	private static final DynamicStringProperty appName = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.DEPLOYMENT_APPLICATION_ID, Constants.APPLICATION_NAME);

	private DruidDataSource dataSource; //数据源
	private String filterTable; //表名


	public JDBCZuulFilterDaoBuilder() {
		//HikariConfig config = new HikariConfig();
		//config.setDataSourceClassName(dataSourceClass.get());
		//config.addDataSourceProperty("url", url.get());
		//config.addDataSourceProperty("user", user.get());
		//config.addDataSourceProperty("password", password.get());
        //
		//config.setMinimumPoolSize(minPoolSize.get());
		//config.setMaximumPoolSize(maxPoolSize.get());
		//config.setConnectionTimeout(connectionTimeout.get());
		//config.setIdleTimeout(idleTimeout.get());
		//config.setMaxLifetime(maxLifetime.get());
        //
		//this.dataSource = new HikariDataSource(config);


		logger.info("开始配置druidDataSource");
		DruidDataSource druidDataSource = new DruidDataSource();

		// 连接配置
		druidDataSource.setDriverClassName(dataSourceClass.get());
		druidDataSource.setUrl(url.get());
		druidDataSource.setUsername(user.get());
		druidDataSource.setPassword(password.get());

		// pool配置
		druidDataSource.setInitialSize(minPoolSize.get());
		druidDataSource.setMinIdle(minPoolSize.get());
		druidDataSource.setMaxActive(maxPoolSize.get());

		druidDataSource.setTestOnBorrow(true);

		//datasource.setMaxWait(maxWait);
		//datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		//datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		//datasource.setValidationQuery(validationQuery);
		//datasource.setTestWhileIdle(testWhileIdle);
		//datasource.setTestOnBorrow(testOnBorrow);
		//datasource.setTestOnReturn(testOnReturn);
		//datasource.setPoolPreparedStatements(poolPreparedStatements);



		this.dataSource = druidDataSource;

		this.filterTable = filterTableName.get(); //+ "_" + environment.get();

		logger.info("配置druidDataSource结束");
	}


	@Override
	public IZuulFilterDao build() {
        return new JDBCZuulFilterDao(filterTable, dataSource, appName.get());
	}

}
