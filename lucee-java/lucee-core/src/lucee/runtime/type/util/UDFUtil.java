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
package lucee.runtime.type.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import lucee.commons.lang.ExceptionUtil;
import lucee.commons.lang.StringUtil;
import lucee.runtime.Page;
import lucee.runtime.PageContext;
import lucee.runtime.PagePlus;
import lucee.runtime.PageSource;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpRow;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.dump.SimpleDumpData;
import lucee.runtime.exp.ExpressionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.exp.PageExceptionImpl;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.FunctionArgument;
import lucee.runtime.type.KeyImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.UDF;
import lucee.runtime.type.UDFGSProperty;
import lucee.runtime.type.UDFPlus;
import lucee.runtime.type.scope.Argument;
import lucee.runtime.type.scope.ArgumentIntKey;
import lucee.transformer.library.function.FunctionLibFunction;
import lucee.transformer.library.function.FunctionLibFunctionArg;

public class UDFUtil {

	public static final short TYPE_UDF=1;
	public static final short TYPE_BIF=2;
	public static final short TYPE_CLOSURE=4;

	private static final char CACHE_DEL = ';';
	private static final char CACHE_DEL2 = ':';

	private static final FunctionArgument[] EMPTY = new FunctionArgument[0];
	
	/**
	 * add detailed function documentation to the exception
	 * @param pe
	 * @param flf
	 */
	public static void addFunctionDoc(PageExceptionImpl pe,FunctionLibFunction flf) {
		ArrayList<FunctionLibFunctionArg> args=flf.getArg();
		Iterator<FunctionLibFunctionArg> it = args.iterator();
		
		// Pattern
		StringBuilder pattern=new StringBuilder(flf.getName());
		StringBuilder end=new StringBuilder();
		pattern.append("(");
		FunctionLibFunctionArg arg;
		int c=0;
		while(it.hasNext()){
			arg = it.next();
			if(!arg.isRequired()) {
				pattern.append(" [");
				end.append("]");
			}
			if(c++>0)pattern.append(", ");
			pattern.append(arg.getName());
			pattern.append(":");
			pattern.append(arg.getTypeAsString());
			
		}
		pattern.append(end);
		pattern.append("):");
		pattern.append(flf.getReturnTypeAsString());
		
		pe.setAdditional(KeyConstants._Pattern, pattern);
		
		// Documentation
		StringBuilder doc=new StringBuilder(flf.getDescription());
		StringBuilder req=new StringBuilder();
		StringBuilder opt=new StringBuilder();
		StringBuilder tmp;
		doc.append("\n");
		
		it = args.iterator();
		while(it.hasNext()){
			arg = it.next();
			tmp=arg.isRequired()?req:opt;
			
			tmp.append("- ");
			tmp.append(arg.getName());
			tmp.append(" (");
			tmp.append(arg.getTypeAsString());
			tmp.append("): ");
			tmp.append(arg.getDescription());
			tmp.append("\n");
		}

		if(req.length()>0)doc.append("\nRequired:\n").append(req);
		if(opt.length()>0)doc.append("\nOptional:\n").append(opt);
		
		
		pe.setAdditional(KeyImpl.init("Documentation"), doc);
		
	}

	public static Object getDefaultValue(PageContext pc, PageSource ps, int udfIndex, int index, Object defaultValue) throws PageException {
		Page p=ComponentUtil.getPage(pc,ps);
    	if(p instanceof PagePlus) return ((PagePlus)p).udfDefaultValue(pc,udfIndex,index,defaultValue);
    	Object rtn = p.udfDefaultValue(pc,udfIndex,index);
    	if(rtn==null) return defaultValue;// in that case it can make no diff between null and not existing, but this only happens with data from old ra files
    	return rtn;
	}
	

	public static Object getDefaultValue(PageContext pc, UDFPlus udf, int index, Object defaultValue) throws PageException {
		Page p=ComponentUtil.getPage(pc,udf.getPageSource());
		if(p instanceof PagePlus) return ((PagePlus)p).udfDefaultValue(pc,udf.getIndex(),index,defaultValue);
		Object rtn = p.udfDefaultValue(pc,udf.getIndex(),index);
    	if(rtn==null) return defaultValue;// in that case it can make no diff between null and not existing, but this only happens with data from old ra files
    	return rtn;
	}

	public static void argumentCollection(Struct values) {
		argumentCollection(values,EMPTY);
	}

	public static void argumentCollection(Struct values, FunctionArgument[] funcArgs) {
		Object value=values.removeEL(KeyConstants._argumentCollection);
		if(value !=null) {
			value=Caster.unwrap(value,value);
			
			if(value instanceof Argument) {
				Argument argColl=(Argument) value;
				Iterator<Key> it = argColl.keyIterator();
				Key k;
				int i=-1;
			    while(it.hasNext()) {
			    	i++;
			    	k = it.next();
			    	if(funcArgs.length>i && k instanceof ArgumentIntKey) {
	            		if(!values.containsKey(funcArgs[i].getName()))
	            			values.setEL(funcArgs[i].getName(),argColl.get(k,Argument.NULL));
	            		else 
	            			values.setEL(k,argColl.get(k,Argument.NULL));
			    	}
	            	else if(!values.containsKey(k)){
	            		values.setEL(k,argColl.get(k,Argument.NULL));
	            	}
	            }
		    }
			else if(value instanceof Collection) {
		        Collection argColl=(Collection) value;
			    //Collection.Key[] keys = argColl.keys();
				Iterator<Key> it = argColl.keyIterator();
				Key k;
				while(it.hasNext()) {
			    	k = it.next();
			    	if(!values.containsKey(k)){
	            		values.setEL(k,argColl.get(k,Argument.NULL));
	            	}
	            }
		    }
			else if(value instanceof Map) {
				Map map=(Map) value;
			    Iterator it = map.entrySet().iterator();
			    Map.Entry entry;
			    Key key;
			    while(it.hasNext()) {
			    	entry=(Entry) it.next();
			    	key = Caster.toKey(entry.getKey(),null);
			    	if(!values.containsKey(key)){
	            		values.setEL(key,entry.getValue());
	            	}
	            }
		    }
			else if(value instanceof java.util.List) {
				java.util.List list=(java.util.List) value;
			    Iterator it = list.iterator();
			    Object v;
			    int index=0;
			    Key k;
			    while(it.hasNext()) {
			    	v= it.next();
			    	k=ArgumentIntKey.init(++index);
			    	if(!values.containsKey(k)){
	            		values.setEL(k,v);
	            	}
	            }
		    }
		    else {
		        values.setEL(KeyConstants._argumentCollection,value);
		    }
		} 
	}
	
	public static String toReturnFormat(int returnFormat,String defaultValue) {
		if(UDF.RETURN_FORMAT_WDDX==returnFormat)		return "wddx";
		else if(UDF.RETURN_FORMAT_JSON==returnFormat)	return "json";
		else if(UDF.RETURN_FORMAT_PLAIN==returnFormat)	return "plain";
		else if(UDF.RETURN_FORMAT_SERIALIZE==returnFormat)	return "cfml";
		else if(UDFPlus.RETURN_FORMAT_JAVA==returnFormat)	return "java";
		// NO XML else if(UDFPlus.RETURN_FORMAT_XML==returnFormat)	return "xml";
		else return defaultValue;
	}
	

	public static boolean isValidReturnFormat(int returnFormat) {
		return toReturnFormat(returnFormat,null)!=null;
	}
	

	public static int toReturnFormat(String[] returnFormats, int defaultValue) {
		if(ArrayUtil.isEmpty(returnFormats)) return defaultValue;
		int rf;
		for(int i=0;i<returnFormats.length;i++){
			rf=toReturnFormat(returnFormats[i].trim(), -1);
			if(rf!=-1) return rf;
		}
		return defaultValue;
	}
	public static int toReturnFormat(String returnFormat, int defaultValue) {
		if(StringUtil.isEmpty(returnFormat,true)) return defaultValue;
		
		returnFormat=returnFormat.trim().toLowerCase();
		if("wddx".equals(returnFormat))				return UDF.RETURN_FORMAT_WDDX;
		else if("json".equals(returnFormat))		return UDF.RETURN_FORMAT_JSON;
		else if("plain".equals(returnFormat))		return UDF.RETURN_FORMAT_PLAIN;
		else if("text".equals(returnFormat))		return UDF.RETURN_FORMAT_PLAIN;
		else if("serialize".equals(returnFormat))	return UDF.RETURN_FORMAT_SERIALIZE;
		else if("cfml".equals(returnFormat))	return UDF.RETURN_FORMAT_SERIALIZE;
		else if("cfm".equals(returnFormat))	return UDF.RETURN_FORMAT_SERIALIZE;
		else if("xml".equals(returnFormat))	return UDF.RETURN_FORMAT_XML;
		else if("java".equals(returnFormat))	return UDFPlus.RETURN_FORMAT_JAVA;
		return defaultValue;
	}
	

	public static int toReturnFormat(String returnFormat) throws ExpressionException {
		int rf = toReturnFormat(returnFormat,-1);
		if(rf!=-1) return rf;
		throw new ExpressionException("invalid returnFormat definition ["+returnFormat+"], valid values are [wddx,plain,json,cfml]");
	}

	public static String toReturnFormat(int returnFormat) throws ExpressionException {
		if(UDF.RETURN_FORMAT_WDDX==returnFormat)		return "wddx";
		else if(UDF.RETURN_FORMAT_JSON==returnFormat)	return "json";
		else if(UDF.RETURN_FORMAT_PLAIN==returnFormat)	return "plain";
		else if(UDF.RETURN_FORMAT_SERIALIZE==returnFormat)	return "cfml";
		else if(UDFPlus.RETURN_FORMAT_JAVA==returnFormat)	return "java";
		else throw new ExpressionException("invalid returnFormat definition, valid values are [wddx,plain,json,cfml]");
	}
	

	public static DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp,UDF udf, short type) {
	
		if(!dp.getShowUDFs()) {
			if(TYPE_UDF==type) return new SimpleDumpData("<UDF>");
			if(TYPE_BIF==type) return new SimpleDumpData("<BIF>");
			if(TYPE_CLOSURE==type) return new SimpleDumpData("<Closure>");
		}
		// arguments
		FunctionArgument[] args = udf.getFunctionArguments();
        
        DumpTable dtArgs;
        if(TYPE_UDF==type) 			dtArgs = new DumpTable("udf","#cc66ff","#ffccff","#000000");
        else if(TYPE_CLOSURE==type) dtArgs = new DumpTable("udf","#ff00ff","#ffccff","#000000");
        else						dtArgs = new DumpTable("udf","#ffff66","#ffffcc","#000000");

		dtArgs.appendRow(new DumpRow(63, new DumpData[]{new SimpleDumpData("label"), new SimpleDumpData("name"), new SimpleDumpData("required"), new SimpleDumpData("type"), new SimpleDumpData("default"), new SimpleDumpData("hint")}));
		for(int i=0;i<args.length;i++) {
			FunctionArgument arg=args[i];
			DumpData def;
			try {
				Object oa=null;
                try {
                    oa = UDFUtil.getDefaultValue(pageContext, (UDFPlus)udf, i, null);//udf.getDefaultValue(pageContext,i,null);
                } catch (Throwable t) {
        			ExceptionUtil.rethrowIfNecessary(t);
                }
                if(oa==null)oa="null";
				def=new SimpleDumpData(Caster.toString(oa));
			} catch (PageException e) {
				def=new SimpleDumpData("");
			}
			dtArgs.appendRow(new DumpRow(0, new DumpData[]{
                    new SimpleDumpData(arg.getDisplayName()),
                    new SimpleDumpData(arg.getName().getString()),
                    new SimpleDumpData(arg.isRequired()),
                    new SimpleDumpData(arg.getTypeAsString()),
                    def,
                    new SimpleDumpData(arg.getHint())}));
			//dtArgs.setRow(0,arg.getHint());
			
		}
		DumpTable func;
		if(TYPE_CLOSURE==type) {
			func=new DumpTable("function", "#ff00ff", "#ffccff", "#000000");
			func.setTitle("Closure");
		}
		else if(TYPE_UDF==type) {
			func=new DumpTable("function", "#cc66ff", "#ffccff", "#000000");
			String f="Function ";
			try {
				f=StringUtil.ucFirst(ComponentUtil.toStringAccess(udf.getAccess()).toLowerCase())+" "+f;
			} 
			catch (ExpressionException e) {}
			f+=udf.getFunctionName();
			if(udf instanceof UDFGSProperty) f+=" (generated)";
			func.setTitle(f);
		}
		else {
			String f="Build in Function "+udf.getFunctionName();
			
			
			func=new DumpTable("#ffff66","#ffffcc","#000000");
			func.setTitle(f);
		}

		if(udf instanceof UDFPlus) {
			PageSource ps = ((UDFPlus)udf).getPageSource();
			if(ps!=null)
				func.setComment("source:"+ps.getDisplayPath());
		}

		if(!StringUtil.isEmpty(udf.getDescription()))func.setComment(udf.getDescription());
		
		func.appendRow(1,new SimpleDumpData("arguments"),dtArgs);
		func.appendRow(1,new SimpleDumpData("return type"),new SimpleDumpData(udf.getReturnTypeAsString()));
		
		boolean hasLabel=!StringUtil.isEmpty(udf.getDisplayName());//displayName!=null && !displayName.equals("");
		boolean hasHint=!StringUtil.isEmpty(udf.getHint());//hint!=null && !hint.equals("");
		
		if(hasLabel || hasHint) {
			DumpTable box = new DumpTable("#ffffff","#cccccc","#000000");
			box.setTitle(hasLabel?udf.getDisplayName():udf.getFunctionName());
			if(hasHint)box.appendRow(0,new SimpleDumpData(udf.getHint()));
			box.appendRow(0,func);
			return box;
		}
		return func;
	}


}
