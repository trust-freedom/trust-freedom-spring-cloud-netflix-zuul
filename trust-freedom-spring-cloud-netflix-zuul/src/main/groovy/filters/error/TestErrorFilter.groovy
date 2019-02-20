package groovy.filters

import org.slf4j.Logger
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import com.netflix.zuul.ZuulFilter;


class TestErrorFilter extends ZuulFilter {
    private static final Logger logger = LoggerFactory.getLogger(TestErrorFilter.class);

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        logger.info("========= 这一个是动态加载的Error过滤器：TestErrorFilter");
        return null;
    }

    @Override
    public String filterType() {
        return FilterConstants.ERROR_TYPE;
    }

    @Override
    public int filterOrder() {
        return 102;
    }
}