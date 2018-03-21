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

package org.pentaho.di.trans.steps.excelinput;

import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.StepInjectionMetaEntry;
import org.pentaho.di.trans.step.StepMetaInjectionInterface;

/**
 * To keep it simple, this metadata injection interface only supports the fields in the spreadsheet for the time being.
 * 
 * @author Matt
 */
public class ExcelInputMetaInjection implements StepMetaInjectionInterface {
  
  private ExcelInputMeta meta;

  public ExcelInputMetaInjection(ExcelInputMeta meta) {
    this.meta = meta;
  }

  @Override
  public List<StepInjectionMetaEntry> getStepInjectionMetadataEntries() throws KettleException {
    List<StepInjectionMetaEntry> all = new ArrayList<StepInjectionMetaEntry>();

    // Add the fields...
    //
    {
      StepInjectionMetaEntry fieldsEntry = new StepInjectionMetaEntry(Entry.FIELDS.name(), Entry.FIELDS.getValueType(), Entry.FIELDS.getDescription());
      all.add(fieldsEntry);
  
      StepInjectionMetaEntry fieldEntry = new StepInjectionMetaEntry(Entry.FIELD.name(), Entry.FIELD.getValueType(), Entry.FIELD.getDescription());
      fieldsEntry.getDetails().add(fieldEntry);
  
      
      for (Entry entry : Entry.values()) {
        if (entry.getParent()==Entry.FIELD){ 
          StepInjectionMetaEntry metaEntry = new StepInjectionMetaEntry(entry.name(), entry.getValueType(), entry.getDescription());
          fieldEntry.getDetails().add(metaEntry);
        }
      }
    }

    // And the sheets
    //
    {
      StepInjectionMetaEntry sheetsEntry = new StepInjectionMetaEntry(Entry.SHEETS.name(), Entry.SHEETS.getValueType(), Entry.SHEETS.getDescription());
      all.add(sheetsEntry);
  
      StepInjectionMetaEntry sheetEntry = new StepInjectionMetaEntry(Entry.SHEET.name(), Entry.SHEET.getValueType(), Entry.SHEET.getDescription());
      sheetsEntry.getDetails().add(sheetEntry);
  
      
      for (Entry entry : Entry.values()) {
        if (entry.getParent()==Entry.SHEET){ 
          StepInjectionMetaEntry metaEntry = new StepInjectionMetaEntry(entry.name(), entry.getValueType(), entry.getDescription());
          sheetEntry.getDetails().add(metaEntry);
        }
      }
    }
    
    // And the filenames...
    //
    // The file name lines
    //
    {
      StepInjectionMetaEntry filesEntry = new StepInjectionMetaEntry(Entry.FILENAME_LINES.name(), ValueMetaInterface.TYPE_NONE, Entry.FILENAME_LINES.description);
      all.add(filesEntry);
      StepInjectionMetaEntry fileEntry = new StepInjectionMetaEntry(Entry.FILENAME_LINE.name(), ValueMetaInterface.TYPE_NONE, Entry.FILENAME_LINE.description);
      filesEntry.getDetails().add(fileEntry);
      
      Entry[] fieldsEntries = new Entry[] { Entry.FILENAME, Entry.FILEMASK, Entry.EXCLUDE_FILEMASK, 
          Entry.FILE_REQUIRED, Entry.INCLUDE_SUBFOLDERS, };
      for (Entry entry : fieldsEntries) {
        StepInjectionMetaEntry metaEntry = new StepInjectionMetaEntry(entry.name(), entry.getValueType(), entry.getDescription());
        fileEntry.getDetails().add(metaEntry);
      }
    }

    return all;
  }

  private class FileLine {
    String filename;
    String includeMask;
    String excludeMask;
    String required;
    String includeSubfolders;
  }
  
  @Override
  public void injectStepMetadataEntries(List<StepInjectionMetaEntry> all) throws KettleException {
    
    List<ExcelInputField> excelInputFields = new ArrayList<ExcelInputField>();
    List<ExcelInputSheet> sheets = new ArrayList<ExcelInputMetaInjection.ExcelInputSheet>();
    List<FileLine> fileLines = new ArrayList<FileLine>();
  
    // Parse the fields, inject into the meta class..
    //
    for (StepInjectionMetaEntry lookFields : all) {
      Entry fieldsEntry = Entry.findEntry(lookFields.getKey());
      if (fieldsEntry!=null) {
        if (fieldsEntry== Entry.FIELDS) {
          for (StepInjectionMetaEntry lookField : lookFields.getDetails()) {
            Entry fieldEntry = Entry.findEntry(lookField.getKey());
            if (fieldEntry!=null) {
              if (fieldEntry == Entry.FIELD) {
                
                ExcelInputField inputField = new ExcelInputField();
                
                List<StepInjectionMetaEntry> entries = lookField.getDetails();
                for (StepInjectionMetaEntry entry : entries) {
                  Entry metaEntry = Entry.findEntry(entry.getKey());
                  if (metaEntry!=null) {
                    String value = (String)entry.getValue();
                    switch(metaEntry) {
                    case NAME:      inputField.setName(value); break;
                    case TYPE:      inputField.setType( ValueMeta.getType(value) ); break;
                    case LENGTH:    inputField.setLength(Const.toInt(value, -1)); break;
                    case PRECISION: inputField.setPrecision(Const.toInt(value, -1)); break;
                    case CURRENCY:  inputField.setCurrencySymbol(value); break;
                    case GROUP:     inputField.setGroupSymbol(value); break;
                    case DECIMAL:   inputField.setDecimalSymbol(value); break;
                    case FORMAT:    inputField.setFormat(value); break;
                    case TRIM_TYPE: inputField.setTrimType(ValueMeta.getTrimTypeByCode(value)); break;
                    case REPEAT:    inputField.setRepeated(ValueMeta.convertStringToBoolean(value)); break;
                    default:
                      break;
                    }
                  }
                }
                
                excelInputFields.add(inputField);
              }
            }
          }
        }
        if (fieldsEntry== Entry.SHEETS) {
          for (StepInjectionMetaEntry lookField : lookFields.getDetails()) {
            Entry fieldEntry = Entry.findEntry(lookField.getKey());
            if (fieldEntry!=null) {
              if (fieldEntry == Entry.SHEET) {
                
                String sheetName = null;
                int startCol = 0;
                int startRow = 0;
                
                List<StepInjectionMetaEntry> entries = lookField.getDetails();
                for (StepInjectionMetaEntry entry : entries) {
                  Entry metaEntry = Entry.findEntry(entry.getKey());
                  if (metaEntry!=null) {
                    String value = (String)entry.getValue();
                    switch(metaEntry) {
                    case SHEET_NAME: sheetName = value; break;
                    case SHEET_START_ROW:  startRow = Const.toInt(Const.trim(value), 0); break;
                    case SHEET_START_COL:  startCol = Const.toInt(Const.trim(value), 0); break;
                    default:
                      break;
                    }
                  }
                }
                
                sheets.add(new ExcelInputSheet(sheetName, startCol, startRow));
              }
            }
          }
        }
        if (fieldsEntry == Entry.FILENAME_LINES) {
          for (StepInjectionMetaEntry lookField : lookFields.getDetails()) {
            Entry fieldEntry = Entry.findEntry(lookField.getKey());
            if (fieldEntry == Entry.FILENAME_LINE) {
              FileLine fileLine = new FileLine();
              
              List<StepInjectionMetaEntry> entries = lookField.getDetails();
              for (StepInjectionMetaEntry entry : entries) {
                Entry metaEntry = Entry.findEntry(entry.getKey());
                if (metaEntry!=null) {
                  String value = (String)entry.getValue();
                  switch(metaEntry) {
                  case FILENAME: fileLine.filename = value; break;
                  case FILEMASK: fileLine.includeMask = value; break;
                  case EXCLUDE_FILEMASK: fileLine.excludeMask = value; break;
                  case FILE_REQUIRED: fileLine.required= value; break;
                  case INCLUDE_SUBFOLDERS: fileLine.includeSubfolders= value; break;
                  default:
                    break;
                  }
                }
              }
              fileLines.add(fileLine);
            }
          }
        }
      }
    }

    // Pass the grid to the step metadata
    //
    if (excelInputFields.size()>0) {
      meta.setField(excelInputFields.toArray(new ExcelInputField[excelInputFields.size()]));
    }

    if (sheets.size()>0) {
      // Set the sheet names too..
      //
      String[] sheetNames = new String[sheets.size()];
      int[] startCols = new int[sheets.size()];
      int[] startRows = new int[sheets.size()];
      
      for (int i=0;i<sheets.size();i++) {
        sheetNames[i] = sheets.get(i).sheetName;
        startCols[i] = sheets.get(i).startCol;
        startRows[i] = sheets.get(i).startRow;
      }
      meta.setSheetName(sheetNames);
      meta.setStartColumn(startCols);
      meta.setStartRow(startRows);
    }
    
    if (fileLines.size() > 0) {
      meta.allocateFiles(fileLines.size());
      for (int i = 0; i < fileLines.size(); i++) {
        FileLine fileLine = fileLines.get(i);
        meta.getFileName()[i] = fileLine.filename;
        meta.getFileMask()[i] = fileLine.includeMask;
        meta.getExludeFileMask()[i] = fileLine.excludeMask;
        meta.getExludeFileMask()[i] = fileLine.excludeMask;
        meta.getFileRequired()[i] = fileLine.required;
        meta.getIncludeSubFolders()[i] = fileLine.includeSubfolders;
      }
    }

  }

  public ExcelInputMeta getMeta() {
    return meta;
  }


  private enum Entry {

    FIELDS(ValueMetaInterface.TYPE_NONE, "All the fields on the spreadsheets"),
    FIELD(ValueMetaInterface.TYPE_NONE, "One field"),

    NAME(FIELD, ValueMetaInterface.TYPE_STRING, "Field name"),
    TYPE(FIELD, ValueMetaInterface.TYPE_STRING, "Field data type"),
    LENGTH(FIELD, ValueMetaInterface.TYPE_STRING, "Field length"),
    PRECISION(FIELD, ValueMetaInterface.TYPE_STRING, "Field precision"),
    TRIM_TYPE(FIELD, ValueMetaInterface.TYPE_STRING, "Field trim type (none, left, right, both)"),
    FORMAT(FIELD, ValueMetaInterface.TYPE_STRING, "Field conversion format"),
    CURRENCY(FIELD, ValueMetaInterface.TYPE_STRING, "Field currency symbol"),
    DECIMAL(FIELD, ValueMetaInterface.TYPE_STRING, "Field decimal symbol"),
    GROUP(FIELD, ValueMetaInterface.TYPE_STRING, "Field group symbol"),
    REPEAT(FIELD, ValueMetaInterface.TYPE_STRING, "Field repeat (Y/N)"),

    FILENAME_LINES(ValueMetaInterface.TYPE_NONE, "The list of file definitions"),
    FILENAME_LINE(ValueMetaInterface.TYPE_NONE, "One file definition line"),
    FILENAME(ValueMetaInterface.TYPE_STRING, "The filename or directory"),
    FILEMASK(ValueMetaInterface.TYPE_STRING, "The file mask (regex)"),
    EXCLUDE_FILEMASK(ValueMetaInterface.TYPE_STRING, "The mask for the files to exclude (regex)"),
    FILE_REQUIRED(ValueMetaInterface.TYPE_STRING, "Is this a required file (Y/N)"),
    INCLUDE_SUBFOLDERS(ValueMetaInterface.TYPE_STRING, "Include sub-folders when searching files? (Y/N)"),
    
    SHEETS(ValueMetaInterface.TYPE_NONE, "All the sheets in the spreadsheets"),
    SHEET(ValueMetaInterface.TYPE_NONE, "One sheet in the spreadsheet"),

    SHEET_NAME(SHEET, ValueMetaInterface.TYPE_STRING, "Sheet name"),
    SHEET_START_ROW(SHEET, ValueMetaInterface.TYPE_STRING, "Sheet start row"),
    SHEET_START_COL(SHEET, ValueMetaInterface.TYPE_STRING, "Sheet start col"),
    ;

    private int valueType;
    private String description;
    private Entry parent;

    private Entry(int valueType, String description) {
      this.valueType = valueType;
      this.description = description;
    }

    private Entry(Entry parent, int valueType, String description) {
      this.parent = parent;
      this.valueType = valueType;
      this.description = description;
    }

    /**
     * @return the valueType
     */
    public int getValueType() {
      return valueType;
    }

    /**
     * @return the description
     */
    public String getDescription() {
      return description;
    }
    
    public static Entry findEntry(String key) {
      return Entry.valueOf(key);
    }
    
    public Entry getParent() {
      return parent;
    }
  }

  
  public class ExcelInputSheet {
    public String sheetName;
    public int startCol;
    public int startRow;
    /**
     * @param sheetName
     * @param startCol
     * @param startRow
     */
    private ExcelInputSheet(String sheetName, int startCol, int startRow) {
      this.sheetName = sheetName;
      this.startCol = startCol;
      this.startRow = startRow;
    }
  }

}
