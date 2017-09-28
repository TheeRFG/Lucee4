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
package lucee.runtime.orm.hibernate.tuplizer;

import java.io.Serializable;
import java.util.HashMap;

import lucee.commons.lang.StringUtil;
import lucee.runtime.Component;
import lucee.runtime.ComponentScope;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.op.Caster;
import lucee.runtime.op.Decision;
import lucee.runtime.orm.hibernate.CommonUtil;
import lucee.runtime.orm.hibernate.HBMCreator;
import lucee.runtime.orm.hibernate.HibernateCaster;
import lucee.runtime.orm.hibernate.HibernateUtil;
import lucee.runtime.orm.hibernate.tuplizer.accessors.CFCAccessor;
import lucee.runtime.orm.hibernate.tuplizer.accessors.CFCGetter;
import lucee.runtime.orm.hibernate.tuplizer.accessors.CFCSetter;
import lucee.runtime.orm.hibernate.tuplizer.proxy.CFCHibernateProxyFactory;
import lucee.runtime.type.Struct;
import lucee.runtime.type.util.KeyConstants;

import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.entity.AbstractEntityTuplizer;
import org.hibernate.tuple.entity.EntityMetamodel;


public class AbstractEntityTuplizerImpl extends AbstractEntityTuplizer {


	public AbstractEntityTuplizerImpl(EntityMetamodel entityMetamodel, PersistentClass persistentClass) {
		super(entityMetamodel, persistentClass);
	}

	@Override
	public Serializable getIdentifier(Object entity, SharedSessionContractImplementor arg1) {
		return toIdentifier(super.getIdentifier(entity, arg1));
	}
	
	@Override
	public Serializable getIdentifier(Object entity) throws HibernateException {
		return toIdentifier(super.getIdentifier(entity));
	}

	private Serializable toIdentifier(Serializable id) {
		if(id instanceof Component) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			Component cfc=(Component) id;
			ComponentScope scope = cfc.getComponentScope();
			lucee.runtime.component.Property[] props = HibernateUtil.getIDProperties(cfc, true,true);
			lucee.runtime.component.Property p;
			String name;
			Object value;
			for(int i=0;i<props.length;i++){
				p=props[i];
				name=p.getName();
				value=scope.get(CommonUtil.createKey(name),null);
				String type=p.getType();
				if(Decision.isAnyType(type)) {
					type="string";
					try {
						Object o=p.getMetaData();
						if(o instanceof Struct) {
							Struct meta=(Struct) o;
							String gen = Caster.toString(meta.get(KeyConstants._generator, null),null);
							if(!StringUtil.isEmpty(gen)){
								type=HBMCreator.getDefaultTypeForGenerator(gen, "string");
							}
						}
					}
					catch (Throwable t) {}
				}

				try {
					value=HibernateCaster.toHibernateValue(ThreadLocalPageContext.get(), value, type);
				}
				catch (PageException pe) {}

				map.put(name, value);
			}
			return map;
		}
		return id;
	}

	protected Instantiator buildInstantiator(PersistentClass persistentClass) {
		return new CFCInstantiator(persistentClass);
	}


	@Override
	protected Instantiator buildInstantiator(EntityMetamodel entityMetamodel, PersistentClass persistentClass) {
		return new CFCInstantiator(persistentClass); //ignoring EntityMetamodel for now?
	}

	/**
	 * return accessors 
	 * @param mappedProperty
	 * @return
	 */
	/*
	private PropertyAccess buildPropertyAccess(Property mappedProperty) {
		if ( mappedProperty.isBackRef() ) {
			PropertyAccessor ac = mappedProperty.getPropertyAccessor(null); //need to return a PropertyAccess
			if(ac!=null) return ac;
		}
		return accessor;
	}
	*/

	
	@Override
	protected Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity) {
				//not sure I did this right. may need to first get propertyaccessstrategy and buildstrategy from there.
		//first attempt//return mappedProperty.getGetter(mappedEntity.getMappedClass());
		return new CFCGetter(mappedProperty.getName()); //will blow up if backref? maybe...
		//oldlucee//return buildPropertyAccess(mappedProperty).getGetter( null, mappedProperty.getName() );
	}

	@Override
	protected Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity) {
		return new CFCSetter(mappedProperty.getName()); //will blow up if backref? maybe...
		//return mappedProperty.getSetter(mappedEntity.getMappedClass());
		//oldlucee//return buildPropertyAccessor(mappedProperty).getSetter( null, mappedProperty.getName() );
	}

	@Override
	protected ProxyFactory buildProxyFactory(PersistentClass pc, Getter arg1,Setter arg2) {
		CFCHibernateProxyFactory pf = new CFCHibernateProxyFactory();
		pf.postInstantiate(pc);
		
		return pf;
	}

	@Override
	public String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory) {
		return CFCEntityNameResolver.INSTANCE.resolveEntityName(entityInstance);
	}

	@Override
	public EntityNameResolver[] getEntityNameResolvers() {
		return new EntityNameResolver[] { CFCEntityNameResolver.INSTANCE };
	}

	@Override
	public Class getConcreteProxyClass() {
		return Component.class;// ????
	}

	@Override
	public Class getMappedClass() {
		return Component.class; // ????
	}

	public EntityMode getEntityMode() {
		return EntityMode.MAP;
	}

	public boolean isInstrumented() {
		return false;
	}

}
