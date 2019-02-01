package com.freedom.springcloud.zuul.dao;

import com.freedom.springcloud.zuul.common.Constants;
import com.google.common.collect.Maps;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

import java.util.concurrent.ConcurrentMap;

public class ZuulFilterDaoFactory {
	/**
	 * zuul.filter.dao.type
	 * dao类型（默认jdbc）
	 */
    private static final DynamicStringProperty daoType =
			DynamicPropertyFactory.getInstance().getStringProperty(Constants.ZUUL_FILTER_DAO_TYPE, "jdbc");
    
    private static ConcurrentMap<String, IZuulFilterDao> daoCache = Maps.newConcurrentMap();

    private ZuulFilterDaoFactory(){
    	
    }
    
    public static IZuulFilterDao getZuulFilterDao(){
    	IZuulFilterDao dao = daoCache.get(daoType.get());
    	
    	if(dao != null){
    		return dao;
    	}
    	
    	if("jdbc".equalsIgnoreCase(daoType.get())){
    		dao = new JDBCZuulFilterDaoBuilder().build();  
    	}
    	//else if("http".equalsIgnoreCase(daoType.get())){
    	//	dao =  new HttpZuulFilterDaoBuilder().build();
    	//}
    	else{
    		dao =  new JDBCZuulFilterDaoBuilder().build();
    	}
    	
    	daoCache.putIfAbsent(daoType.get(), dao);
    	
    	return dao;
    }
    
    public static String getCurrentType(){
    	return daoType.get();
    }
    
}
