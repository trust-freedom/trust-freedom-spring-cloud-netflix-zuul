package groovy.filters

import org.slf4j.Logger
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import com.netflix.zuul.ZuulFilter;


class TestRouteFilter extends ZuulFilter {
    private static final Logger logger = LoggerFactory.getLogger(TestRouteFilter.class);

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        logger.info("========= 这一个是动态加载的Route过滤器：TestRouteFilter");
        return null;
    }

    @Override
    public String filterType() {
        return FilterConstants.ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 103;
    }
}