/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2012 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.trans.steps.xmloutput;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.vfs.FileObject;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.ResultFile;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.xmloutput.XMLField.ContentType;

/**
 * Converts input rows to one or more XML files.
 * 
 * @author Matt
 * @since 14-jan-2006
 */
public class XMLOutput extends BaseStep implements StepInterface
{
	private XMLOutputMeta meta;

	private XMLOutputData data;

	public XMLOutput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
			Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta = (XMLOutputMeta) smi;
		data = (XMLOutputData) sdi;

		Object[] r;
		boolean result = true;

		r = getRow(); // This also waits for a row to be finished.
		
		if(first && meta.isDoNotOpenNewFileInit())
		{
			// no more input to be expected...
			// In this case, no file was opened.
			if (r == null) 
			{
				setOutputDone();
				return false;
			}
			
			if (openNewFile())
			{
				data.OpenedNewFile=true;
			} else
			{
				logError("Couldn't open file " + meta.getFileName()); 
				setErrors(1L);
				return false;
			}
		}
		
		
		if ((r != null && getLinesOutput() > 0 && meta.getSplitEvery() > 0 && (getLinesOutput() % meta.getSplitEvery()) == 0))
		{
			// Done with this part or with everything.
			closeFile();

			// Not finished: open another file...
			if (r != null)
			{
				if (!openNewFile())
				{
					logError("Unable to open new file (split #" + data.splitnr + "...");  
					setErrors(1);
					return false;
				}
			}
		}

		if (r == null) // no more input to be expected...
		{
			setOutputDone();
			return false;
		}

		writeRowToFile(getInputRowMeta(), r);
		
		data.outputRowMeta = getInputRowMeta().clone();
		meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);
		putRow(data.outputRowMeta, r); // in case we want it to go further...

		if (checkFeedback(getLinesOutput())) logBasic("linenr " + getLinesOutput()); 

		return result;
	}

	private void writeRowToFile(RowMetaInterface rowMeta, Object[] r) throws KettleException
	{
		try
		{
			if (first)
			{
				data.formatRowMeta = rowMeta.clone();

				first = false;

				data.fieldnrs = new int[meta.getOutputFields().length];
				for (int i = 0; i < meta.getOutputFields().length; i++)
				{
					data.fieldnrs[i] = data.formatRowMeta.indexOfValue(meta.getOutputFields()[i].getFieldName());
					if (data.fieldnrs[i] < 0)
					{
						throw new KettleException("Field [" + meta.getOutputFields()[i].getFieldName()+ "] couldn't be found in the input stream!");  
					}
				
					// Apply the formatting settings to the valueMeta object...
					//
					ValueMetaInterface valueMeta = data.formatRowMeta.getValueMeta(data.fieldnrs[i]);
					XMLField field = meta.getOutputFields()[i];
					valueMeta.setConversionMask(field.getFormat());
					valueMeta.setLength(field.getLength(), field.getPrecision());
					valueMeta.setDecimalSymbol(field.getDecimalSymbol());
					valueMeta.setGroupingSymbol(field.getGroupingSymbol());
					valueMeta.setCurrencySymbol(field.getCurrencySymbol());
				}
			}

			if (meta.getOutputFields() == null || meta.getOutputFields().length == 0)
			{
				/*
				 * Write all values in stream to text file.
				 */

				// OK, write a new row to the XML file:
				data.writer.write((" <" + meta.getRepeatElement() + ">").toCharArray());  

				for (int i = 0; i < data.formatRowMeta.size(); i++)
				{
					// Put a space between the XML elements of the row
					//
					if (i > 0) data.writer.write(' ');

					ValueMetaInterface valueMeta = data.formatRowMeta.getValueMeta(i);
					Object valueData = r[i];
					
					writeField(valueMeta, valueData, valueMeta.getName());
				}
			} 
			else
			{
				/*
				 * Only write the fields specified!
				 */
        // Write a new row to the XML file:
        data.writer.write((" <" + meta.getRepeatElement()).toCharArray());  
			  
			  // First do the attributes...
			  //
			  for(int i=0;i<meta.getOutputFields().length;i++) {
			    XMLField xmlField = meta.getOutputFields()[i];
			    if (xmlField.getContentType() == ContentType.Attribute) {
			      ValueMetaInterface valueMeta = data.formatRowMeta.getValueMeta(data.fieldnrs[i]);
	          Object valueData = r[data.fieldnrs[i]];

	          String elementName = xmlField.getElementName();
	          if ( Const.isEmpty(elementName) )
	          {
	            elementName = xmlField.getFieldName();
	          }
            data.writer.write((" "+elementName+"=\""+valueMeta.getString(valueData)+"\"").toCharArray());
			    }
			  }

			  data.writer.write(">".toCharArray());  
			  
			  // Now write the elements
			  //
				for (int i = 0; i < meta.getOutputFields().length; i++)
				{
					XMLField outputField = meta.getOutputFields()[i];
					if (outputField.getContentType()==ContentType.Element) {
  					if (i > 0)
  						data.writer.write(' '); // a space between
  					// elements
  
  					ValueMetaInterface valueMeta = data.formatRowMeta.getValueMeta(data.fieldnrs[i]);
  					Object valueData = r[data.fieldnrs[i]];
  
  					String elementName = outputField.getElementName();
  					if ( Const.isEmpty(elementName) )
  					{
  						elementName = outputField.getFieldName();
  					}
  
  					if (!(valueMeta.isNull(valueData) && meta.isOmitNullValues())) {
  					  writeField(valueMeta, valueData, elementName);
  					}
					}
				}
			}
			
			data.writer.write((" </" + meta.getRepeatElement() + ">").toCharArray());  
			data.writer.write(Const.CR.toCharArray());
		} 
		catch (Exception e)
		{
			throw new KettleException("Error writing XML row :" + e.toString() + Const.CR + "Row: " + getInputRowMeta().getString(r), e);  
		}

		incrementLinesOutput();
	}

	private void writeField(ValueMetaInterface valueMeta, Object valueData, String element) throws KettleStepException
	{
		try
		{
			String str = XMLHandler.addTagValue(element, valueMeta.getString(valueData), false);
			if (str != null)
				data.writer.write(str.toCharArray());
		} catch (Exception e)
		{
			throw new KettleStepException("Error writing line :", e); 
		}
	}

	public String buildFilename(boolean ziparchive)
	{
		return meta.buildFilename(this, getCopy(), data.splitnr, ziparchive);
	}

	public boolean openNewFile()
	{
		boolean retval = false;
		data.writer = null;

		try
		{
		  if (meta.isServletOutput()) {
		    data.writer = getTrans().getServletPrintWriter();
        if (meta.getEncoding() != null && meta.getEncoding().length() > 0)
        {
          data.writer.write(XMLHandler.getXMLHeader(meta.getEncoding()).toCharArray());
        } else
        {
          data.writer.write(XMLHandler.getXMLHeader(Const.XML_ENCODING).toCharArray());
        }
		  } else {
  			
  			FileObject file = KettleVFS.getFileObject(buildFilename(true), getTransMeta());
  			
  
  		    if(meta.isAddToResultFiles())
              {
  				// Add this to the result file names...
  				ResultFile resultFile = new ResultFile(ResultFile.FILE_TYPE_GENERAL, file, getTransMeta().getName(), getStepname());
  				resultFile.setComment("This file was created with a xml output step"); 
  	            addResultFile(resultFile);
              }
  
  			OutputStream outputStream;
  			if (meta.isZipped())
  			{
  				OutputStream fos = KettleVFS.getOutputStream(file, false);
  				data.zip = new ZipOutputStream(fos);
  				File entry = new File(buildFilename(false));
  				ZipEntry zipentry = new ZipEntry(entry.getName());
  				zipentry.setComment("Compressed by Kettle"); 
  				data.zip.putNextEntry(zipentry);
  				outputStream = data.zip;
  			} else
  			{
  				OutputStream fos = KettleVFS.getOutputStream(file, false);
  				outputStream = fos;
  			}
  			if (meta.getEncoding() != null && meta.getEncoding().length() > 0)
  			{
  				logBasic("Opening output stream in encoding: " + meta.getEncoding()); 
  				data.writer = new OutputStreamWriter(outputStream, meta.getEncoding());
  				data.writer.write(XMLHandler.getXMLHeader(meta.getEncoding()).toCharArray());
  			} else
  			{
  				logBasic("Opening output stream in default encoding : " + Const.XML_ENCODING); 
  				data.writer = new OutputStreamWriter(outputStream);
  				data.writer.write(XMLHandler.getXMLHeader(Const.XML_ENCODING).toCharArray());
  			}
		  }

			// Add the name space if defined
			StringBuffer nameSpace = new StringBuffer();
			if ((meta.getNameSpace() != null) && (!"".equals(meta.getNameSpace())))  { 
				nameSpace.append(" xmlns=\""); 
				nameSpace.append(meta.getNameSpace());
				nameSpace.append("\""); 
			}

			// OK, write the header & the parent element:
			data.writer.write(("<" + meta.getMainElement() + nameSpace.toString() + ">" + Const.CR).toCharArray());  

			retval = true;
		} catch (Exception e)
		{
			logError("Error opening new file : " + e.toString()); 
		}
		// System.out.println("end of newFile(), splitnr="+splitnr);

		data.splitnr++;

		return retval;
	}

	private boolean closeFile()
	{
		boolean retval = false;
		if(data.OpenedNewFile)
		{
			try
			{
				// Close the parent element
				data.writer.write(("</" + meta.getMainElement() + ">" + Const.CR).toCharArray());  

				// System.out.println("Closed xml file...");

				data.writer.close();

				if (meta.isZipped())
				{
					// System.out.println("close zip entry ");
					data.zip.closeEntry();
					// System.out.println("finish file...");
					data.zip.finish();
					data.zip.close();
				}
	
				retval = true;
			} catch (Exception e)
			{
			}
		}
		return retval;
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta = (XMLOutputMeta) smi;
		data = (XMLOutputData) sdi;

		if (super.init(smi, sdi))
		{
			data.splitnr = 0;
			if(!meta.isDoNotOpenNewFileInit())
			{
				if (openNewFile())
				{
					data.OpenedNewFile=true;
					return true;
				} else
				{
					logError("Couldn't open file " + meta.getFileName()); 
					setErrors(1L);
					stopAll();
				}
			}else
				return true;
		}
		return false;
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta = (XMLOutputMeta) smi;
		data = (XMLOutputData) sdi;

		closeFile();

		super.dispose(smi, sdi);
	}

}