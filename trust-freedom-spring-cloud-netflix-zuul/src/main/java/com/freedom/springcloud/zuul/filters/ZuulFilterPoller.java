package com.freedom.springcloud.zuul.filters;


import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.freedom.springcloud.zuul.common.Constants;
import com.freedom.springcloud.zuul.common.FilterInfo;
import com.freedom.springcloud.zuul.dao.ZuulFilterDaoFactory;
import com.google.common.collect.Maps;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.zuul.FilterLoader;
import com.netflix.zuul.filters.FilterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ZuulFilterPoller {
	private static final Logger logger = LoggerFactory.getLogger(ZuulFilterPoller.class);

	private Map<String, FilterInfo> runningFilters = Maps.newHashMap();

	/**
	 * zuul.filter.poller.enabled
	 * 是否启动轮询线程（默认true）
	 */
	private DynamicBooleanProperty pollerEnabled = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(Constants.ZUUL_FILTER_POLLER_ENABLED, true);
	/**
	 * zuul.filter.evict.poller.enabled
	 * 是否启动轮询驱逐线程（默认true）
	 */
	private DynamicBooleanProperty evictPollerEnabled = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(Constants.ZUUL_FILTER_EVICT_POLLER_ENABLED, true);
	/**
	 * zuul.filter.poller.interval
	 * 轮询线程执行间隔（默认30s）
	 */
	private DynamicLongProperty pollerInterval = DynamicPropertyFactory.getInstance()
			.getLongProperty(Constants.ZUUL_FILTER_POLLER_INTERVAL, 30000);
	/**
	 * zuul.use.active.filters
	 * 是否使用激活的filter（默认true）
	 */
	private DynamicBooleanProperty active = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(Constants.ZUUL_USE_ACTIVE_FILTERS, true);
	/**
	 * zuul.use.canary.filters
	 * 是否使用金丝雀fliter（默认false）
	 */
	private DynamicBooleanProperty canary = DynamicPropertyFactory.getInstance()
			.getBooleanProperty(Constants.ZUUL_USE_CANARY_FILTERS, false);

	/**
	 * 各种类型filter路径
	 * zuul.filter.pre.path
	 * zuul.filter.route.path
	 * zuul.filter.post.path
	 * zuul.filter.error.path
	 * zuul.filter.custom.path
	 */
	private DynamicStringProperty preFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.ZUUL_FILTER_PRE_PATH, null);
	private DynamicStringProperty routeFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.ZUUL_FILTER_ROUTE_PATH, null);
	private DynamicStringProperty postFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.ZUUL_FILTER_POST_PATH, null);
	private DynamicStringProperty errorFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.ZUUL_FILTER_ERROR_PATH, null);
	private DynamicStringProperty customFiltersPath = DynamicPropertyFactory.getInstance()
			.getStringProperty(Constants.Zuul_FILTER_CUSTOM_PATH, null);


	/**
	 * apollo配置
	 */
	private DynamicStringProperty portalUrl = DynamicPropertyFactory.getInstance()
			.getStringProperty("apollo.portal.url", null);
	private DynamicStringProperty token = DynamicPropertyFactory.getInstance()
			.getStringProperty("apollo.openapi.token", null);
	private DynamicStringProperty operationBy = DynamicPropertyFactory.getInstance()
			.getStringProperty("apollo.operation.by", "apollo");

	// 根据-D参数获取
	private DynamicStringProperty appId = DynamicPropertyFactory.getInstance()
			.getStringProperty("app.id", null);
	private DynamicStringProperty env = DynamicPropertyFactory.getInstance()
			.getStringProperty("env", null);


	private static ZuulFilterPoller instance = null;  //单实例

	private volatile boolean running = true;

	private ApolloOpenApiClient apolloOpenApiClient;  // apollo open api client


	/** 轮询线程 */
	private Thread checkerThread = new Thread("ZuulFilterPoller") {

		public void run() {
			while (running) {
				try {
					// 是否启用轮询
					if (!pollerEnabled.get())
						continue;

					// 是否使用金丝雀filter（可控制某一个zuul开启使用金丝雀filter）
					if (canary.get()) {
						try{
							Map<String, FilterInfo> filterMap = Maps.newHashMap();

							// 查询所有激活filter
							List<FilterInfo> activeScripts = ZuulFilterDaoFactory.getZuulFilterDao().getAllActiveFilters();
	
							if (!activeScripts.isEmpty()) {
								for (FilterInfo filterInfo : activeScripts) {
									filterMap.put(filterInfo.getFilterId(), filterInfo);
								}
							}

							// 查询所有金丝雀filter
							// 如果filterId同时存在激活版，和金丝雀版，版本号不同，按此处逻辑，只按照filterId为key，故金丝雀版会生效（金丝雀版本号应该较大）
							List<FilterInfo> canaryScripts = ZuulFilterDaoFactory.getZuulFilterDao().getAllCanaryFilters();
							if (!canaryScripts.isEmpty()) {
								for (FilterInfo filterInfo : canaryScripts) {
									filterMap.put(filterInfo.getFilterId(), filterInfo);
								}
							}

							// 遍历filterMap，检查FilterInfo，如果是新增或更新，写到磁盘
							for (FilterInfo filterInfo : filterMap.values()) {
								doFilterCheck(filterInfo);
							}
						}
						catch(Throwable t){
							logger.error("pooling zuul filter(active&canary) error: ", t);
						}
					}
					// 是否使用激活的filter
					else if (active.get()) {
						try{
							// 查询所有激活filter
							List<FilterInfo> newFilters = ZuulFilterDaoFactory.getZuulFilterDao().getAllActiveFilters();
							
							if (newFilters.isEmpty())
								continue;

							// 遍历filterMap，检查FilterInfo，如果是新增或更新，写到磁盘
							for (FilterInfo newFilter : newFilters) {
								doFilterCheck(newFilter);
							}
						}
						catch(Throwable t){
							logger.error("pooling zuul filter(active) error: ", t);
						}
					}
				}
				catch (Throwable t) {
					logger.error("ZuulFilterPoller run error!", t);
				}
				finally {
					try {
						sleep(pollerInterval.get());  //休眠30s
					} catch (InterruptedException e) {
						logger.error("ZuulFilterPoller sleep error!", e);
					}
				}
			}
		}
	};

	/** 轮询驱逐线程 */
	private Thread checkerForEvictThread = new Thread("ZuulFilterEvictPoller") {

		public void run() {
			try {
				Thread.sleep(pollerInterval.get() / 2);
			} catch (InterruptedException e) {
				logger.error("", e);
			}

			while (running) {
				try {
					// 是否启用轮询驱逐
					if (!evictPollerEnabled.get())
						continue;

					// 是否使用金丝雀filter（可控制某一个zuul开启使用金丝雀filter）
					if (canary.get()) {
						try{
							// 查询既没有激活版，又没有金丝雀版的filter
							List<FilterInfo> evictFilters = ZuulFilterDaoFactory.getZuulFilterDao().getEvictFilters(true);

							if(evictFilters!=null && evictFilters.size()>0){
								for(FilterInfo evictFilter : evictFilters){
									// 先判断，后驱逐
									doFilterEvict(evictFilter);
								}
							}
						}
						catch(Throwable t){
							logger.error("evict zuul filter(active&canary) error: ", t);
						}
					}
					// 是否使用激活的filter
					else if (active.get()) {
						try{
							// 查询没有激活版的filter
							List<FilterInfo> evictFilters = ZuulFilterDaoFactory.getZuulFilterDao().getEvictFilters(false);

							if(evictFilters!=null && evictFilters.size()>0){
								for(FilterInfo evictFilter : evictFilters){
									// 先判断，后驱逐
									doFilterEvict(evictFilter);
								}
							}
						}
						catch(Throwable t){
							logger.error("evict zuul filter(active) error: ", t);
						}
					}
				}
				catch (Throwable t) {
					logger.error("ZuulFilterEvictPoller run error!", t);
				}
				finally {
					try {
						sleep(pollerInterval.get());  //休眠30s
					} catch (InterruptedException e) {
						logger.error("ZuulFilterEvictPoller sleep error!", e);
					}
				}
			}
		}
	};

	
	private ZuulFilterPoller() {
		// 轮询更新线程
		this.checkerThread.start();

		// 轮询驱逐线程
		this.checkerForEvictThread.start();

		// 创建 apollo open api client
		apolloOpenApiClient = ApolloOpenApiClient.newBuilder()
				.withPortalUrl(portalUrl.get())
				.withToken(token.get())
				.build();
	}

	/**
	 * 同步创建单实例，并启动轮询线程
	 */
	public static void start(){
		if(instance == null){
			synchronized(ZuulFilterPoller.class){
				if(instance == null){
					instance = new ZuulFilterPoller() ;
				}
			}
		}
	}
	
	public static ZuulFilterPoller getInstance(){
		return instance;
	}

	public void stop(){
		this.running = false;
	}

	private void doFilterCheck(FilterInfo newFilter) throws IOException {
		FilterInfo existFilter = runningFilters.get(newFilter.getFilterId());

		// 如果filter不存在，或发生了变化
		// 如果filterId同时存在激活版，和金丝雀版，版本号不同，金丝雀版会生效，写入磁盘
		if (existFilter == null || !existFilter.equals(newFilter)) {
			logger.info("adding filter to disk" + newFilter.toString());
			writeFilterToDisk(newFilter);
			runningFilters.put(newFilter.getFilterId(), newFilter);

			// 更新disable property为false
			updateDisableProperty(newFilter, "false");
		}
	}

	/**
	 * filter写入指定目录
	 * @param newFilter
	 * @throws IOException
	 */
	private void writeFilterToDisk(FilterInfo newFilter) throws IOException {
		String filterType = newFilter.getFilterType();

		String path = preFiltersPath.get();
		if (filterType.equals("post")) {
			path = postFiltersPath.get();
		}
		else if (filterType.equals("route")) {
			path = routeFiltersPath.get();
		}
		else if (filterType.equals("error")) {
			path = errorFiltersPath.get();
		}
		else if (!filterType.equals("pre") && customFiltersPath.get() != null) {
			path = customFiltersPath.get();
		}

		File f = new File(path, newFilter.getFilterName() + ".groovy");
		FileWriter file = new FileWriter(f);
		BufferedWriter out = new BufferedWriter(file);
		out.write(newFilter.getFilterCode());
		out.close();
		file.close();

		logger.info("filter written " + f.getPath());
	}


	/**
	 * 驱逐此filter
	 * @param evictFilter
	 */
	private void doFilterEvict(FilterInfo evictFilter){
		// 1、判断磁盘中是否存在文件，存在即删除，并继续后续步骤
		String filterType = evictFilter.getFilterType();

		String path = preFiltersPath.get();
		if (filterType.equals("post")) {
			path = postFiltersPath.get();
		}
		else if (filterType.equals("route")) {
			path = routeFiltersPath.get();
		}
		else if (filterType.equals("error")) {
			path = errorFiltersPath.get();
		}
		else if (!filterType.equals("pre") && customFiltersPath.get() != null) {
			path = customFiltersPath.get();
		}

		File file = new File(path, evictFilter.getFilterName() + ".groovy");

		// 文件存在，继续
		if(file.exists()){
			// 2、将filter从内存中的 FilterRegistry 和 FilterLoader#hashFiltersByType 删除， easy for gc
			clearReference(evictFilter, file);

			// 3、删除文件
			try {
				file.delete();
			}
			catch (Exception e){
				logger.error("delete filter error", e);
			}

			// 3、判断是否更新disable property
			updateDisableProperty(evictFilter, "true");

			logger.info("evict filter success： " + evictFilter.getFilterId());
		}
	}


	/**
	 * 清理引用
	 * @param evictFilter
	 * @param file
	 */
	private void clearReference(FilterInfo evictFilter, File file) {
		// 清除filterRegistry中引用
		FilterRegistry filterRegistry = FilterRegistry.instance();
		filterRegistry.remove(file.getAbsolutePath() + file.getName());

		// 清除FilterLoader中的缓存
		FilterLoader filterLoader = FilterLoader.getInstance();
		Field field = ReflectionUtils.findField(FilterLoader.class, "hashFiltersByType");
		ReflectionUtils.makeAccessible(field);
		@SuppressWarnings("rawtypes")
		Map cache = (Map) ReflectionUtils.getField(field, filterLoader);
		cache.remove(evictFilter.getFilterType()); //清除整个filterType list

		// 清除runningFilters中引用
		runningFilters.remove(evictFilter.getFilterId());
	}


	/**
	 * 更新配置中的disable property
	 *
	 * @param filterInfo
	 */
	private void updateDisableProperty(FilterInfo filterInfo, String value) {
		// 构造item
		OpenItemDTO openItemDTO = new OpenItemDTO();
		openItemDTO.setKey(disablePropertyName(filterInfo));
		openItemDTO.setValue(value);
		openItemDTO.setComment("update by ZuulFilterPoller");
		openItemDTO.setDataChangeCreatedBy(operationBy.get());
		openItemDTO.setDataChangeLastModifiedBy(operationBy.get());

		// 为了兼容apollo历史版本，先update再create
		try {
			// 尝试更新
			apolloOpenApiClient.updateItem(appId.get(),env.get(),null, null, openItemDTO);
		}
		catch(Exception e){
			logger.info("apollo openapi: key=" + openItemDTO.getKey() + "is not found, perform a create operation. exception: " + e.getMessage());

			// 新增
			apolloOpenApiClient.createItem(appId.get(),env.get(),null, null, openItemDTO);
		}

		// 构造发布dto
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		String releaseTitle = df.format(new Date()) + "-zuul-release";

		NamespaceReleaseDTO namespaceReleaseDTO = new NamespaceReleaseDTO();
		namespaceReleaseDTO.setReleaseTitle(releaseTitle);
		namespaceReleaseDTO.setReleasedBy(operationBy.get());
		//namespaceReleaseDTO.setReleaseComment();

		// 发布namespace
		apolloOpenApiClient.publishNamespace(appId.get(),env.get(), null, null, namespaceReleaseDTO);
	}


	public String disablePropertyName(FilterInfo filterInfo) {
		return "zuul." + filterInfo.getFilterName() + "." + filterInfo.getFilterType() + ".disable";
	}

}
