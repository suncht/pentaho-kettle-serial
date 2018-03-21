 /* Copyright (c) 2011 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is Pentaho 
 * Data Integration.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.*/

package org.pentaho.di.trans.steps.gpload;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.i18n.BaseMessages;


/**
 * Does the opening of the output "stream". It's either a file or inter process
 * communication which is transparent to users of this class.
 *
 * Copied from Sven Boden's Oracle version
 *
 * @author Luke Lonergan
 * @since  28-mar-2008
 */
public class GPLoadDataOutput
{
   private static Class<?> PKG = GPLoadDataOutput.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$
   
   protected LogChannelInterface        log;
   
   private GPLoad            gpLoad = null;
	private GPLoadMeta        meta;
	private PrintWriter       output = null;
	private boolean           first = true;
	private int               fieldNumbers[] = null;
	private String            enclosure = null;
	private String            delimiter = null;
	private SimpleDateFormat  sdfDate = null;
	private SimpleDateFormat  sdfDateTime = null;

	public GPLoadDataOutput(GPLoad gpLoad, GPLoadMeta meta)
	{
		this.meta = meta;
		this.gpLoad = gpLoad;
	}
	
	public GPLoadDataOutput(GPLoad gpLoad, GPLoadMeta meta, LogLevel logLevel) 
	{
      this(gpLoad, meta);
      log = new LogChannel(this);
      log.setLogLevel(logLevel);
   }
	
	public void open(VariableSpace space, Process sqlldrProcess) throws KettleException
	{
		// String loadMethod = meta.getLoadMethod();
		try 
		{
			OutputStream os = null;

		//	if ( GPLoadMeta.METHOD_AUTO_CONCURRENT.equals(loadMethod))
		//	{
		//		String dataFile = meta.getControlFile();
		//		dataFile = StringUtil.environmentSubstitute(dataFile);
				
        //      os = new FileOutputStream(dataFile, true);
		//	}
		//	else
		//	{
				// Else open the data file filled in.
				String dataFile = meta.getDataFile();
				if (Const.isEmpty(dataFile)) {
				   throw new KettleException(BaseMessages.getString(PKG, "GPload.Exception.DataFileMissing"));
				}
				
				dataFile = space.environmentSubstitute(dataFile);
				if (Const.isEmpty(dataFile)) {
				   throw new KettleException(BaseMessages.getString(PKG, "GPload.Exception.DataFileMissing"));
				}
				
            log.logDetailed("Creating temporary load file "+dataFile);  
	         os = new FileOutputStream(dataFile, false);
		//	}	
			
            String encoding = meta.getEncoding();
            if ( Const.isEmpty(encoding) )
            {
            	// Use the default encoding.
			    output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
            }
            else
            {
            	// Use the specified encoding
			    output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, encoding)));
            }
		}
		catch ( IOException e )
		{
			throw new KettleException("GPLoadDataOutput.Exception"+e.getMessage(), e);
		}
	}
	
	public void close() throws IOException
	{
		if ( output != null )
		{
			output.close();
		}
	}
	
	PrintWriter getOutput()
	{
	    return output;
	}
	
	private String createEscapedString(String orig, String enclosure)
	{
		StringBuffer buf = new StringBuffer(orig);
		
		Const.repl(buf, enclosure, enclosure + enclosure);
		return buf.toString();
	}
	
	public void writeLine(RowMetaInterface mi, Object row[]) throws KettleException
	{
        if ( first )
        {
            first = false;
     
            enclosure = meta.getEnclosure();
            if (enclosure == null) {
               enclosure = "";
            }
            else {
               enclosure = gpLoad.environmentSubstitute(enclosure);
            }
            
            delimiter = meta.getDelimiter();
            if (delimiter == null) {
               throw new KettleException(BaseMessages.getString(PKG, "GPload.Exception.DelimiterMissing"));
            }
            else {
               delimiter = gpLoad.environmentSubstitute(delimiter);
               if (Const.isEmpty(delimiter)) {
                  throw new KettleException(BaseMessages.getString(PKG, "GPload.Exception.DelimiterMissing"));
               }
            }
            
            // Setup up the fields we need to take for each of the rows
            // as this speeds up processing.
            fieldNumbers=new int[meta.getFieldStream().length];
			   for (int i=0;i<fieldNumbers.length;i++) 
			   {
				   fieldNumbers[i]=mi.indexOfValue(meta.getFieldStream()[i]);
			   	if (fieldNumbers[i]<0)
				   {
					    throw new KettleException(BaseMessages.getString(PKG, "GPLoadDataOutput.Exception.FieldNotFound",  meta.getFieldStream()[i]));
				   }
			   }
			
			   sdfDate = new SimpleDateFormat("yyyy-MM-dd");
			   sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
         }
        
        // Write the data to the output
        ValueMetaInterface v = null;
        int number = 0;
        
		for (int i=0;i<fieldNumbers.length;i++) 
		{
		   //  TODO:  variable substitution
			if ( i!=0 ) output.print(delimiter);
			number = fieldNumbers[i];
			v = mi.getValueMeta(number);
			if ( row[number] == null)
			{
				// TODO (SB): special check for null in case of Strings.
				output.print(enclosure);
				output.print(enclosure);
			}
			else
			{
				switch ( v.getType() )
				{
				case ValueMetaInterface.TYPE_STRING:
					String s = mi.getString(row, number);
					if ( s.indexOf(enclosure) >= 0 )
						s = createEscapedString(s, enclosure);
					output.print(enclosure);
					output.print(s);
					output.print(enclosure);
					break;
				case ValueMetaInterface.TYPE_INTEGER:
					Long l = mi.getInteger(row, number);
					if (meta.getEncloseNumbers()) {
					   output.print(enclosure);
					   output.print(l);
					   output.print(enclosure);
					}
					else {
					   output.print(l);
					}
					break;
				case ValueMetaInterface.TYPE_NUMBER:
					Double d = mi.getNumber(row, number);
					if (meta.getEncloseNumbers()) {
   					output.print(enclosure);
	   				output.print(d);
		   			output.print(enclosure);
					}
		   		else {
		   		   output.print(d);
		   			}
					break;
				case ValueMetaInterface.TYPE_BIGNUMBER:
					BigDecimal bd = mi.getBigNumber(row, number);
					if (meta.getEncloseNumbers()) {
   					output.print(enclosure);
	     				output.print(bd);
			   		output.print(enclosure);
					}
					else {
					   output.print(bd);
					}
					break;
				case ValueMetaInterface.TYPE_DATE:
					Date dt = mi.getDate(row, number);
					output.print(enclosure);
					String mask = meta.getDateMask()[i];
					if ( GPLoadMeta.DATE_MASK_DATETIME.equals(mask))
					{
						output.print(sdfDateTime.format(dt));	
					}
					else
					{
						// Default is date format
						output.print(sdfDate.format(dt));
					}					   
					output.print(enclosure);
					break;
				case ValueMetaInterface.TYPE_BOOLEAN:
					Boolean b = mi.getBoolean(row, number);
					output.print(enclosure);
					if ( b.booleanValue() )
						output.print("Y");
					else
						output.print("N");
					output.print(enclosure);
					break;			    	
				case ValueMetaInterface.TYPE_BINARY:
					byte byt[] = mi.getBinary(row, number);
					output.print("<startlob>");
					output.print(byt);
					output.print("<endlob>");
					break;			    
				default:
					throw new KettleException(BaseMessages.getString(PKG, "GPLoadDataOutput.Exception.TypeNotSupported", v.getType()));
				}
			}
		}
		output.print(Const.CR);
	}
}