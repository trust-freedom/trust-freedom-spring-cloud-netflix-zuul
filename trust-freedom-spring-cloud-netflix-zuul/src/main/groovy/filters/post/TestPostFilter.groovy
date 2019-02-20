package groovy.filters

import org.slf4j.Logger
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import com.netflix.zuul.ZuulFilter;


class TestPostFilter extends ZuulFilter {
    private static final Logger logger = LoggerFactory.getLogger(TestPostFilter.class);

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        logger.info("========= 这一个是动态加载的后置过滤器：TestPostFilter");
        return null;
    }

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 101;
    }
}