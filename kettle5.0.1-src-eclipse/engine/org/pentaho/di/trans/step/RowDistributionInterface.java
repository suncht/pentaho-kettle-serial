package org.pentaho.di.trans.step;

import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.gui.PrimitiveGCInterface.EImage;
import org.pentaho.di.core.row.RowMetaInterface;

public interface RowDistributionInterface {
  
  /**
   * @return The row distribution code (plugin id)
   */
  public String getCode();
  
  /**
   * @return The row distribution description (plugin description)
   */
  public String getDescription();

  /**
   * Do the actual row distribution in the step
   * @param rowMeta the meta-data of the row to distribute
   * @param row the data of the row data to distribute
   * @param stepInterface The step to distribute the rows in
   * @throws KettleStepException
   */
  public void distributeRow(RowMetaInterface rowMeta, Object[] row, StepInterface stepInterface) throws KettleStepException;
 
  /**
   * Which mini-icon needs to be shown on the hop?
   * 
   * @return the available code EImage or null if the standard icon needs to be used.
   */
  public EImage getDistributionImage();
}
