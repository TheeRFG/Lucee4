/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package lucee.runtime.functions.cache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import lucee.commons.io.cache.Cache;
import lucee.commons.io.cache.CacheEntryFilter;
import lucee.commons.io.cache.exp.CacheException;
import lucee.commons.lang.ExceptionUtil;
import lucee.commons.lang.StringUtil;
import lucee.runtime.PageContext;
import lucee.runtime.cache.CacheConnection;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.config.ConfigWeb;
import lucee.runtime.engine.ThreadLocalPageContext;

public class Util {
	
	/**
	 * get the default cache for a certain type, also check definitions in application context (application.cfc/cfapplication)
	 * @param pc current PageContext
	 * @param type default type -> Config.CACHE_DEFAULT_...
	 * @param defaultValue value returned when there is no default cache for this type
	 * @return matching cache
	 */
	public static Cache getDefault(PageContext pc, int type,Cache defaultValue) {
		// get default from application conetx
		String name=null;
		if(pc!=null && pc.getApplicationContext()!=null)
			name=pc.getApplicationContext().getDefaultCacheName(type);
		Config config=ThreadLocalPageContext.getConfig(pc);
		if(!StringUtil.isEmpty(name)){
			Cache cc = getCache(config, name, null);
			if(cc!=null) return cc;
		}
		
		// get default from config
		CacheConnection cc= ((ConfigImpl)config).getCacheDefaultConnection(type);
		if(cc==null) return defaultValue;
		try {
			return cc.getInstance(config);
		} catch (Throwable t) {
			ExceptionUtil.rethrowIfNecessary(t);
			return defaultValue;
		}
	}
	
	/**
	 * get the default cache for a certain type, also check definitions in application context (application.cfc/cfapplication)
	 * @param pc current PageContext
	 * @param type default type -> Config.CACHE_DEFAULT_...
	 * @return matching cache
	 * @throws IOException 
	 */
	public static Cache getDefault(PageContext pc, int type) throws IOException {
		// get default from application conetx
		String name=pc!=null?pc.getApplicationContext().getDefaultCacheName(type):null;
		if(!StringUtil.isEmpty(name)){
			Cache cc = getCache(pc.getConfig(), name, null);
			if(cc!=null) return cc;
		}
		
		// get default from config
		Config config = ThreadLocalPageContext.getConfig(pc);
		CacheConnection cc= ((ConfigImpl)config).getCacheDefaultConnection(type);
		if(cc==null) throw new CacheException("there is no default "+toStringType(type,"")+" cache defined, you need to define this default cache in the Lucee Administrator");
		return cc.getInstance(config);
		
		
	}

	public static Cache getCache(PageContext pc,String cacheName, int type) throws IOException {
		if(StringUtil.isEmpty(cacheName)){
			return getDefault(pc, type);
		}
		return getCache(ThreadLocalPageContext.getConfig(pc), cacheName);
	}

	public static Cache getCache(PageContext pc,String cacheName, int type, Cache defaultValue)  {
		if(StringUtil.isEmpty(cacheName)){
			return getDefault(pc, type,defaultValue);
		}
		return getCache(ThreadLocalPageContext.getConfig(pc), cacheName,defaultValue);
	}
	
	
	public static Cache getCache(Config config,String cacheName) throws IOException {
		CacheConnection cc=  config.getCacheConnections().get(cacheName.toLowerCase().trim());
		if(cc==null) throw noCache(config,cacheName);
		return cc.getInstance(config);	
	}
	
	public static Cache getCache(Config config,String cacheName, Cache defaultValue) {
		CacheConnection cc= config.getCacheConnections().get(cacheName.toLowerCase().trim());
		if(cc==null) return defaultValue;
		try {
			return cc.getInstance(config);
		} catch (Throwable t) {
			ExceptionUtil.rethrowIfNecessary(t);
			return defaultValue;
		}	
	}
	public static CacheConnection getCacheConnection(Config config,String cacheName) throws IOException {
		CacheConnection cc= config.getCacheConnections().get(cacheName.toLowerCase().trim());
		if(cc==null) throw noCache(config,cacheName);
		return cc;	
	}

	public static CacheConnection getCacheConnection(Config config,String cacheName, CacheConnection defaultValue) {
		CacheConnection cc= config.getCacheConnections().get(cacheName.toLowerCase().trim());
		if(cc==null) return defaultValue;
		return cc;	
	}
	
	
	
	
	
	
	private static CacheException noCache(Config config, String cacheName) {
		StringBuilder sb=new StringBuilder("there is no cache defined with name [").append(cacheName).append("], available caches are [");
		Iterator<String> it = ((ConfigImpl)config).getCacheConnections().keySet().iterator();
		if(it.hasNext()){
			sb.append(it.next());
		}
		while(it.hasNext()){
			sb.append(", ").append(it.next());
		}
		sb.append("]");
		
		return new CacheException(sb.toString());
	}

	private static String toStringType(int type, String defaultValue) {
		if(type==ConfigImpl.CACHE_DEFAULT_OBJECT) return "object";
		if(type==ConfigImpl.CACHE_DEFAULT_TEMPLATE) return "template";
		if(type==ConfigImpl.CACHE_DEFAULT_QUERY) return "query";
		if(type==ConfigImpl.CACHE_DEFAULT_RESOURCE) return "resource";
		if(type==ConfigImpl.CACHE_DEFAULT_FUNCTION) return "function";
		if(type==ConfigImpl.CACHE_DEFAULT_INCLUDE) return "include";
		return defaultValue;
	}

	public static String key(String key) {
		return key.toUpperCase().trim();
	}


	public static boolean removeEL(ConfigWeb config, CacheConnection cc)  {
		try {
			remove(config,cc);
			return true;
		} catch (Throwable e) {
			ExceptionUtil.rethrowIfNecessary(e);
			return false;
		}
	}
	public static void remove(ConfigWeb config, CacheConnection cc) throws Throwable  {
		Cache c = cc.getInstance(config);
		// FUTURE no reflection needed
		
		
		Method remove=null;
		try{
			remove = c.getClass().getMethod("remove", new Class[0]);
			
		}
		catch(Exception ioe){
			c.remove((CacheEntryFilter)null);
			return;
		}
		
		try {
			remove.invoke(c, new Object[0]);
		}
		catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

	public static int toType(String type, int defaultValue) {
		type=type.trim().toLowerCase();
		if("object".equals(type)) return ConfigImpl.CACHE_DEFAULT_OBJECT;
		if("query".equals(type)) return ConfigImpl.CACHE_DEFAULT_QUERY;
		if("resource".equals(type)) return ConfigImpl.CACHE_DEFAULT_RESOURCE;
		if("template".equals(type)) return ConfigImpl.CACHE_DEFAULT_TEMPLATE;
		if("function".equals(type)) return ConfigImpl.CACHE_DEFAULT_FUNCTION;
		if("include".equals(type)) return ConfigImpl.CACHE_DEFAULT_INCLUDE;
		return defaultValue;
	}

	public static String toType(int type, String defaultValue) {
		if(ConfigImpl.CACHE_DEFAULT_OBJECT==type) return "object";
		if(ConfigImpl.CACHE_DEFAULT_QUERY==type) return "query";
		if(ConfigImpl.CACHE_DEFAULT_RESOURCE==type) return "resource";
		if(ConfigImpl.CACHE_DEFAULT_TEMPLATE==type) return "template";
		if(ConfigImpl.CACHE_DEFAULT_FUNCTION==type) return "function";
		if(ConfigImpl.CACHE_DEFAULT_INCLUDE==type) return "include";
		return defaultValue;
	}
}
