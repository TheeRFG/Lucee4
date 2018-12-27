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
package lucee.runtime.search.lucene2.docs;

import java.io.Reader;
import java.io.StringReader;

import lucee.commons.io.IOUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.lang.ExceptionUtil;
import lucee.commons.lang.StringUtil;
import lucee.runtime.op.Caster;
import lucee.runtime.search.lucene2.html.HTMLParser;

import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;

/** A utility for making Lucene Documents for HTML documents. */

public final class HTMLDocument {
    private static final char FILE_SEPARATOR = System.getProperty("file.separator").charAt(0);
  
  public static String uid(Resource f) {
    return f.getPath().replace(FILE_SEPARATOR, '\u0000') +
      "\u0000" +
      DateField.timeToString(f.lastModified());
  }

  public static String uid2url(String uid) {
    String url = uid.replace('\u0000', '/');	  // replace nulls with slashes
    return url.substring(0, url.lastIndexOf('/')); // remove date from end
  }
  
  public static Document getDocument(Resource res,String charset)  {
    Document doc = new Document();
    doc.add(FieldUtil.Text("uid", uid(res), false));
    
    HTMLParser parser = new HTMLParser();
    try {
    	parser.parse(res,charset);
    } 
    catch (Throwable t) {
		ExceptionUtil.rethrowIfNecessary(t);
        return doc;
    }
    addContent(doc,parser);
    return doc;
  }

  public static Document getDocument(StringBuffer content, Reader reader) {
      Document doc = new Document();
      
      HTMLParser parser = new HTMLParser();
      try {
    	  String str = IOUtil.toString(reader);
    	  if(content!=null)content.append(str);
    	  doc.add(FieldUtil.UnIndexed("size", Caster.toString(str.length())));
    	  StringReader sr = new StringReader(str);
          parser.parse(sr);
      } 
      catch (Throwable t) {
			ExceptionUtil.rethrowIfNecessary(t);
    	  //t.printStackTrace();
          return doc;
      }
      
      addContent(doc, parser);
      return doc;
  }
  
  	private static void addContent(Document doc, HTMLParser parser) {
	    
  		FieldUtil.setMimeType(doc,"text/html");
	    //doc.add(FieldUtil.UnIndexed("mime-type", "text/html"));
	    
	    String content = parser.getContent();

	    FieldUtil.setTitle(doc,parser.getTitle());
	    
	    String summary = parser.getSummary();
	    if(StringUtil.isEmpty(summary)){
	    	summary=(content.length()<=200)? content:content.substring(0,200);
		    FieldUtil.setSummary(doc,summary,false);
	    }
	    else{
	    	FieldUtil.setSummary(doc,summary,true);
	    }
	    FieldUtil.setRaw(doc,content);
	    FieldUtil.setContent(doc,content);
	    
	    //doc.add(FieldUtil.UnIndexed("charset", StringUtil.valueOf(parser.getCharset())));
	    
	    if(parser.hasKeywords()) {
	    	FieldUtil.setKeywords(doc,parser.getKeywords());
	    }
	    

	    if(parser.hasAuthor()){
	    	FieldUtil.setAuthor(doc,parser.getAuthor());
	    }
	    if(parser.hasCustom1()){
	    	FieldUtil.setCustom(doc,parser.getCustom1(),1);
	    }
	    if(parser.hasCustom2()){
	    	FieldUtil.setCustom(doc,parser.getCustom2(),2);
	    }
	    if(parser.hasCustom3()){
	    	FieldUtil.setCustom(doc,parser.getCustom3(),3);
	    }
	    if(parser.hasCustom4()){
	    	FieldUtil.setCustom(doc,parser.getCustom4(),4);
	    }
	    
	    
    
}

  private HTMLDocument() {}
}
    