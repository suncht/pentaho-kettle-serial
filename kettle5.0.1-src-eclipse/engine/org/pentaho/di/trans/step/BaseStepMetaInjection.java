package org.pentaho.di.trans.step;

import java.util.List;

import org.pentaho.di.core.row.ValueMetaInterface;

public class BaseStepMetaInjection {

  protected StepInjectionMetaEntry createStepMetaInjectionEntry(StepMetaInjectionEnumEntry entry) {
    StepInjectionMetaEntry stepInjectionMetaEntry = new StepInjectionMetaEntry(entry.name(), entry.getValueType(), entry.getDescription());
    return stepInjectionMetaEntry;
  }
  
  protected void addNestedStepMetaInjectionEntries(List<StepInjectionMetaEntry> list, StepMetaInjectionEnumEntry[] allEntries, StepMetaInjectionEnumEntry itemsEntry, StepMetaInjectionEnumEntry itemEntry) {

    StepInjectionMetaEntry fieldsEntry = createStepMetaInjectionEntry(itemsEntry);
    list.add(fieldsEntry);
    StepInjectionMetaEntry fieldEntry = createStepMetaInjectionEntry(itemEntry);
    fieldsEntry.getDetails().add(fieldEntry);

    for (StepMetaInjectionEnumEntry entry : allEntries) {
      if (entry.getParent()==itemEntry){ 
        fieldEntry.getDetails().add(createStepMetaInjectionEntry(entry));
      }
    }
  }
  
  protected void addTopLevelStepMetaInjectionEntries(List<StepInjectionMetaEntry> list, StepMetaInjectionEnumEntry[] allEntries) {
    for (StepMetaInjectionEnumEntry entry : allEntries) {
      if (entry.getParent()==null && entry.getValueType()!=ValueMetaInterface.TYPE_NONE) { 
        list.add(createStepMetaInjectionEntry(entry));
      }
    }
  }
  
}
