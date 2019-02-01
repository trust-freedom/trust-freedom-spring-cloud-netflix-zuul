package com.freedom.springcloud.zuul.config;

import com.freedom.springcloud.zuul.common.Constants;
import com.freedom.springcloud.zuul.filters.FilterScriptManagerServlet;
import com.freedom.springcloud.zuul.filters.ZuulFilterPoller;
import com.netflix.config.ConfigurationManager;
import com.netflix.zuul.FilterFileManager;
import com.netflix.zuul.FilterLoader;
import com.netflix.zuul.groovy.GroovyCompiler;
import com.netflix.zuul.groovy.GroovyFileFilter;
import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZuulDynamicFilterInitializerConfiguration implements SmartLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ZuulDynamicFilterInitializerConfiguration.class);

    private boolean running;

    /**
     * 加载zuul filter的路径
     */
    //@Value("${zuul.filter.path:}")
    //private String zuulFiltersPath;

    /**
     * 定时检查zuul filter变化的时间间隔（默认5秒）
     */
    @Value("${zuul.filter.polling.interval.seconds:5}")
    private int pollingIntervalSeconds;


    /**
     * scriptmanager servlet
     * @return
     */
    @Bean
    public ServletRegistrationBean createFilterScriptManagerServlet(){
        //final AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        //String test = config.getString(Constants.DATA_SOURCE_CLASS_NAME);

        ServletRegistrationBean servlet = new ServletRegistrationBean(new FilterScriptManagerServlet(), "/admin/scriptmanager");
        return servlet;
    }

    @Override
    public void start() {
        // 1、单例的FilterLoader配置Groovy编译器
        FilterLoader.getInstance().setCompiler(new GroovyCompiler());

        // 2、FilterFileManager  Filter文件管理器
        FilterFileManager.setFilenameFilter(new GroovyFileFilter()); //只读取.groovy结尾的文件

        final AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        final String preFiltersPath = config.getString(Constants.ZUUL_FILTER_PRE_PATH);
        final String postFiltersPath = config.getString(Constants.ZUUL_FILTER_POST_PATH);
        final String routeFiltersPath = config.getString(Constants.ZUUL_FILTER_ROUTE_PATH);
        final String errorFiltersPath = config.getString(Constants.ZUUL_FILTER_ERROR_PATH);
        final String customPath = config.getString(Constants.Zuul_FILTER_CUSTOM_PATH);

        try {
            if (customPath == null) {
                FilterFileManager.init(pollingIntervalSeconds, preFiltersPath, postFiltersPath, routeFiltersPath, errorFiltersPath);
            }
            else {
                FilterFileManager.init(pollingIntervalSeconds, preFiltersPath, postFiltersPath, routeFiltersPath, errorFiltersPath, customPath);
            }

            this.running = true;
        }
        catch (Exception e) {
            logger.error("", e);
            throw new RuntimeException(e);
        }

        // 3、ZuulFilterPoller start
        // load filters in DB
        ZuulFilterPoller.start();
    }

    @Override
    public void stop() {
        this.running = false;

        // TODO
    }

    @Override
    public void stop(Runnable callback) {
        callback.run();
    }

    /**
     * 是否在ApplicationContext#refresh()时自动启动
     * @return
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    /**
     * 返回此对象所处的phase阶段（可能参与分阶段的生命周期管理）
     * @return
     */
    @Override
    public int getPhase() {
        return 0;
    }
}
