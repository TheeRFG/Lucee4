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
package lucee.runtime.tag;

import java.io.File;

import lucee.commons.lang.StringUtil;
import lucee.runtime.PageContextImpl;
import lucee.runtime.PageSource;
import lucee.runtime.config.ConfigWeb;
import lucee.runtime.customtag.CustomTagUtil;
import lucee.runtime.customtag.InitFile;
import lucee.runtime.exp.ExpressionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.util.ListUtil;

/**
 * To create cfimport custom tags
 */
public final class CFImportTag extends CFTag {

    
	@Override
	public void initFile() throws PageException {
		ConfigWeb config = pageContext.getConfig();
        
		String[] filenames=CustomTagUtil.getFileNames(config, getAppendix());// = appendix+'.'+config.getCFMLExtension();
        
		
		String strRelPathes=attributesScope.remove("__custom_tag_path").toString();
		String[] relPathes=ListUtil.listToStringArray(strRelPathes, File.pathSeparatorChar);
	    for(int i=0;i<relPathes.length;i++){
	    	if(!StringUtil.endsWith(relPathes[i],'/'))relPathes[i]=relPathes[i]+"/";
	    }
	    
	    // MUSTMUST use cache like regular ct
		// page source
	    PageSource ps;
	    for(int rp=0;rp<relPathes.length;rp++){
		    for(int fn=0;fn<filenames.length;fn++){
	            ps=((PageContextImpl)pageContext).getRelativePageSourceExisting(relPathes[rp]+filenames[fn]);
	            if(ps!=null){
	            	source=new InitFile(ps,filenames[fn],filenames[fn].endsWith('.'+config.getCFCExtension()));
	            	return;
	            }
			} 
	    }
	    
	// EXCEPTION
	    // message
	    
        StringBuffer msg=new StringBuffer("could not find template [");
        msg.append(CustomTagUtil.getDisplayName(config, getAppendix()));
        msg.append("] in the following directories [");
        msg.append(strRelPathes.replace(File.pathSeparatorChar, ','));
        msg.append(']');
        
	    
		throw new ExpressionException(msg.toString(),CustomTagUtil.getDetail(config));
	    
	}

}