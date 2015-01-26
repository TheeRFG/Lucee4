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
package lucee.runtime.cache.ram;

import java.util.Date;

import lucee.runtime.cache.CacheUtil;
import lucee.runtime.type.Struct;
import lucee.commons.io.cache.CacheEntry;

public class RamCacheEntry implements CacheEntry {

	private String key;
	private Object value;
	private long idleTime;
	private long until;
	private long created;
	private long modifed;
	private long accessed;
	private int hitCount;

	public RamCacheEntry(String key, Object value, long idleTime, long until) {
		this.key=key;
		this.value=value;
		this.idleTime=idleTime;
		this.until=until;
		created=modifed=accessed=System.currentTimeMillis();
		hitCount=1;
	}

	@Override
	public Date created() {
		return new Date(created);
	}

	@Override
	public Struct getCustomInfo() {
		return CacheUtil.getInfo(this);
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public int hitCount() {
		return hitCount;
	}

	@Override
	public long idleTimeSpan() {
		return idleTime;
	}

	@Override
	public Date lastHit() {
		return new Date(accessed);
	}

	@Override
	public Date lastModified() {
		return new Date(modifed);
	}

	@Override
	public long liveTimeSpan() {
		return until;
	}

	public long size() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void update(Object value) {
		this.value=value;
		modifed=accessed=System.currentTimeMillis();
		hitCount++;
	}

	public RamCacheEntry read() {
		accessed=System.currentTimeMillis();
		hitCount++;
		return this;
	}
}
