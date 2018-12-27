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
package lucee.runtime.search.lucene2.net;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lucee.commons.io.log.Log;
import lucee.commons.io.log.LogUtil;
import lucee.commons.io.res.util.ResourceUtil;
import lucee.commons.lang.ExceptionUtil;
import lucee.commons.lang.HTMLUtil;
import lucee.commons.lang.StringUtil;
import lucee.commons.lang.SystemOut;
import lucee.commons.net.HTTPUtil;
import lucee.commons.net.http.HTTPEngine;
import lucee.commons.net.http.HTTPResponse;
import lucee.runtime.config.Config;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.search.lucene2.DocumentUtil;
import lucee.runtime.tag.Index;
import lucee.runtime.type.util.ArrayUtil;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;

/**
 * 
 */
public final class WebCrawler {
    
    private static HTMLUtil htmlUtil=new HTMLUtil();
	private Log log;
	
    
    
    public WebCrawler(Log log) {
    	this.log=log;
	}

	
    
    public void parse(IndexWriter writer, URL current, String[] extensions, boolean recurse, long timeout) throws IOException {
    	translateExtension(extensions);
    	if(ArrayUtil.isEmpty(extensions))extensions=Index.EXTENSIONS;
        _parse(log,writer,null,current,new ArrayList(), extensions,recurse,0,timeout);
    }
    

	private static URL translateURL(URL url) throws MalformedURLException {
		
		
		//print.out(url.toExternalForm());
		String path=url.getPath();
		int dotIndex = path.lastIndexOf('.');
		// no dot
		if(dotIndex==-1){
			if(path.endsWith("/")) return HTTPUtil.removeRef(url);
			
			
			return HTTPUtil.removeRef(new URL(
					url.getProtocol(),
					url.getHost(),
					url.getPort(),
					path+"/"+StringUtil.emptyIfNull(url.getQuery())));
		}
		//print.out("rem:"+HTTPUtil.removeRef(url));
		return HTTPUtil.removeRef(url);
	}   
    

    private void translateExtension(String[] extensions) {
		for(int i=0;i<extensions.length;i++){
			if(extensions[i].startsWith("*."))extensions[i]=extensions[i].substring(2);
			else if(extensions[i].startsWith("."))extensions[i]=extensions[i].substring(1);
		}
	}


        

	/**
     * @param writer
     * @param current
	 * @param content 
	 * @throws IOException 
     */

    private static Document toDocument(StringBuffer content,IndexWriter writer, String root, URL current,long timeout) throws IOException {
    	HTTPResponse rsp = HTTPEngine.get(current, null, null, timeout,HTTPEngine.MAX_REDIRECT, null, "LuceeBot", null, null);
    	Document doc = DocumentUtil.toDocument(content,root,current, rsp);
    	
		return doc;
	}

    protected static void _parse(Log log,IndexWriter writer, String root, URL current, List urlsDone, String[] extensions, boolean recurse,int deep,long timeout) throws IOException  {
    	
    	StringBuffer content = _parseItem(log,writer, root, current, urlsDone, extensions, recurse, deep,timeout);
        if(content!=null)_parseChildren(log,content,writer, root, current, urlsDone, extensions, recurse, deep,timeout);
    }
    
    public static StringBuffer _parseItem(Log log,IndexWriter writer, String root, URL url, List urlsDone, String[] extensions, boolean recurse,int deep,long timeout) throws IOException{
    	try{
	    	url=translateURL(url);
	    	if(urlsDone.contains(url.toExternalForm())) return null;
	        urlsDone.add(url.toExternalForm());
	    	
	    	StringBuffer content=new StringBuffer();            
	    	Document doc=toDocument(content,writer, root, url,timeout);
	    	
	        if(doc==null) return null;
	        if(writer!=null)writer.addDocument(doc);
	        
	        // Test
	        /*Resource dir = ResourcesImpl.getFileResourceProvider().getResource("/Users/mic/Temp/leeway3/");
	        if(!dir.isDirectory())dir.mkdirs();
	        Resource file=dir.getRealResource(url.toExternalForm().replace("/", "_"));
	        IOUtil.write(file, content.toString(), "UTF-8", false);*/
	        
	        info(log,url.toExternalForm());
	        return content;
    	}
    	catch(IOException ioe){
    		error(log,url.toExternalForm(),ioe);
    		throw ioe;
    	}
    }
    


	protected static void _parseChildren(Log log,StringBuffer content,IndexWriter writer, String root, URL base, List urlsDone, String[] extensions, boolean recurse,int deep,long timeout) throws IOException  {
    	

		
        if(recurse) {
            List urls = htmlUtil.getURLS(content.toString(),base);
	        
            // loop through all children
            int len=urls.size();
            List childIndexer=len>1?new ArrayList():null;
            ChildrenIndexer ci;
            //print.out("getting content");
            
    		for(int i=0;i<len;i++) {
                URL url=(URL) urls.get(i);
                /*if(url.toExternalForm().indexOf("80")!=-1){
        			SystemOut.printDate("base:"+base);
        			SystemOut.printDate("url:"+url);
        		}*/
                
                url=translateURL(url);
               
                if(urlsDone.contains(url.toExternalForm())) continue;
                //urlsDone.add(url.toExternalForm());
            	
                String protocol=url.getProtocol().toLowerCase();
                String file=url.getPath();
                if((protocol.equals("http") || protocol.equals("https")) && validExtension(extensions,file) &&
                   base.getHost().equalsIgnoreCase(url.getHost())) {
                	try {
                		ci=new ChildrenIndexer(log,writer,root,url,urlsDone,extensions,recurse,deep+1,timeout);
                		
                		childIndexer.add(ci);
                		ci.start();
                    }
                    catch(Throwable t) {
            			ExceptionUtil.rethrowIfNecessary(t);
                    	//print.printST(t);
                    }
                }
            }
    		
    		if(childIndexer!=null && !childIndexer.isEmpty()){
    			Iterator it = childIndexer.iterator();
	    		while(it.hasNext()) {
	    			ci=(ChildrenIndexer) it.next();
	    			if(ci.isAlive()) {
	        			try {
	        				ci.join(timeout);
	        				
	    				} 
	        			catch (InterruptedException e) {
	        				//print.printST(e);
	        			}
	        		}
	    			// timeout exceptionif(ci.isAlive()) throw new IOException("timeout occur while invoking page ["+ci.url+"]");
	    			
	    			if(ci.isAlive()){
	    				ci.interrupt();
	    				Config config = ThreadLocalPageContext.getConfig();
	    				SystemOut.printDate(config!=null?config.getErrWriter():new PrintWriter(System.err),"timeout ["+timeout+" ms] occur while invoking page ["+ci.url+"]");
	    			}
	    		}
	    		
	    		//print.out("exe child");
	    		it = childIndexer.iterator();
	    		while(it.hasNext()) {
	    			ci=(ChildrenIndexer) it.next();
	    			//print.out("exec-child:"+ci.url);
	    			//print.out(content);
	    			if(ci.content!=null)_parseChildren(log,ci.content,writer, root, ci.url, urlsDone, extensions, recurse, deep,timeout);
	    		}
	    		
    		}
    		
    		
	        urls.clear();
        }
        //print.out("end:"+base);
    }
    


    /*protected static void _sssparse(IndexWriter writer, String root, URL current, List urlsDone, String[] extensions, boolean recurse,int deep,long timeout) throws IOException  {
    	current=translateURL(current);
    	print.out("start:"+current);
    	if(urlsDone.contains(current.toExternalForm())) return;
    	
    	HttpMethod method = HTTPUtil.invoke(current, null, null, -1, null, "LuceeBot", null, -1, null, null, null);
    	StringBuffer content=new StringBuffer();
    	Document doc = DocumentUtil.toDocument(content,root,current, method);
    	
        urlsDone.add(current.toExternalForm());
        if(doc==null) return;
        if(writer!=null)writer.addDocument(doc);
        
        
        if(recurse) {
            List urls = htmlUtil.getURLS(content.toString(),current);
	        
            // loop through all children
            int len=urls.size();
            List childIndexer=len>1?new ArrayList():null;
            ChildrenIndexer ci;
    		for(int i=0;i<len;i++) {
                URL url=(URL) urls.get(i);
                String protocol=url.getProtocol().toLowerCase();
                String file=url.getPath();
                if((protocol.equals("http") || protocol.equals("https")) && validExtension(extensions,file) &&
                   current.getHost().equalsIgnoreCase(url.getHost())) {
                	
                	//_parse(writer,root,url,urlsDone,extensions,recurse,deep+1);
                	
                    try {
                    	if(len==1 || true)_parse(writer,root,url,urlsDone,extensions,recurse,deep+1,timeout);
                    	else {
                    		ci=new ChildrenIndexer(writer,root,url,urlsDone,extensions,recurse,deep+1);
                    		ci.start();
                    		childIndexer.add(ci);
                    	}
                    }
                    catch(Throwable t) {
						ExceptionUtil.rethrowIfNecessary(t);
                    }
                }
            }
    		
    		if(!childIndexer.isEmpty()){
    			Iterator it = childIndexer.iterator();
	    		while(it.hasNext()) {
	    			ci=(ChildrenIndexer) it.next();
	    			if(ci.isAlive()) {
	        			try {
	        				ci.join(20*1000);
	    				} 
	        			catch (InterruptedException e) {}
	        		}
	    		}
    		}
    		
    		
	        urls.clear();
        }
        print.out("end:"+current);
    }*/
    
    


	private static boolean validExtension(String[] extensions, String file) {
		
		String ext = ResourceUtil.getExtension(file,"");
		ext=lucee.runtime.type.util.ListUtil.first(ext,"/",true);
		
		if(StringUtil.isEmpty(ext))return true;
		for(int i=0;i<extensions.length;i++){
			if(ext.equalsIgnoreCase(extensions[i]))return true;
		}
		return false;
	}


    private static void info(Log log,String doc) {
		if(log==null) return;
		log.log(Log.LEVEL_INFO,"Webcrawler", "invoke "+doc);
	}

    private static void error(Log log,String doc, Exception e) {
		if(log==null) return;
		LogUtil.log(log,Log.LEVEL_ERROR,"Webcrawler", "invoke "+doc+":",e);
	}
}


class ChildrenIndexer extends Thread {
	protected IndexWriter writer;
	protected String root;
	protected URL url;
	protected List urlsDone;
	protected String[] extensions;
	protected boolean recurse;
	protected int deep;
	protected StringBuffer content;
	private long timeout;
	private Log log;

	public ChildrenIndexer(Log log,IndexWriter writer, String root, URL url,List urlsDone, String[] extensions,boolean recurse, int deep,long timeout) {
		this.writer=writer;
		this.root=root;
		this.url=url;
		this.urlsDone=urlsDone;
		this.extensions=extensions;
		this.recurse=recurse;
		this.deep=deep;
		this.timeout=timeout;
		this.log=log;
	}

	public void run(){
		try {
			//WebCrawler._parse(writer, root, url, urlsDone, extensions, recurse, deep);
			
			this.content=WebCrawler._parseItem(log,writer, root, url, urlsDone, extensions, recurse, deep,timeout+1);
			
		} catch (IOException e) {}
	}
	
	
}