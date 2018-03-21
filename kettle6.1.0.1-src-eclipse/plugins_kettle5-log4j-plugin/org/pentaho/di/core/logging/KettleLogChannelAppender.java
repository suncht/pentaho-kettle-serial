/*! ******************************************************************************
*
* Pentaho Data Integration
*
* Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
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

package org.pentaho.di.core.logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Logs Log4j log events to a Kettle log channel
 *
 * @deprecated Use the {@link KettleLogChannelAppender} from Kettle core. This is to be deleted when we no longer need
 * to support Kettle <= 4.3.0.
 */
@Deprecated
public class KettleLogChannelAppender extends AppenderSkeleton {
  /**
   * Maps Kettle LogLevels to Log4j Levels
   */
  public static Map<LogLevel, Level> LOG_LEVEL_MAP;

  static {
    Map<LogLevel, Level> map = new HashMap<LogLevel, Level>();
    map.put( LogLevel.BASIC, Level.INFO );
    map.put( LogLevel.MINIMAL, Level.INFO );
    map.put( LogLevel.DEBUG, Level.DEBUG );
    map.put( LogLevel.ERROR, Level.ERROR );
    map.put( LogLevel.DETAILED, Level.INFO );
    map.put( LogLevel.ROWLEVEL, Level.DEBUG );
    map.put( LogLevel.NOTHING, Level.OFF );
    LOG_LEVEL_MAP = Collections.unmodifiableMap( map );
  }

  private LogChannelInterface log;

  public KettleLogChannelAppender( LogChannelInterface log ) {
    this( log, new Log4jKettleLayout() );
  }

  /**
   * Create an appender that logs to the provided log channel
   *
   * @param log    Log channel to log to
   * @param layout layout to use
   * @throws NullPointerException If {@code log} is null
   */
  public KettleLogChannelAppender( LogChannelInterface log, Layout layout ) {
    if ( log == null || layout == null ) {
      throw new NullPointerException();
    }
    setLayout( layout );
    this.log = log;
  }

  @Override
  protected void append( LoggingEvent event ) {
    String s = layout.format( event );

    if ( Level.DEBUG.equals( event.getLevel() ) ) {
      log.logDebug( s );
    } else if ( Level.ERROR.equals( event.getLevel() )
      || Level.FATAL.equals( event.getLevel() ) ) {
      Throwable t = event.getThrowableInformation() == null ? null : event.getThrowableInformation().getThrowable();
      if ( t == null ) {
        log.logError( s );
      } else {
        log.logError( s, t );
      }
    } else if ( Level.TRACE.equals( event.getLevel() ) ) {
      log.logRowlevel( s );
    } else if ( Level.OFF.equals( event.getLevel() ) ) {
      log.logMinimal( s );
    } else {
      // ALL, WARN, INFO, or others
      log.logBasic( s );
    }
  }

  @Override
  public boolean requiresLayout() {
    // We may or may not have a layout
    return true;
  }

  @Override
  public void close() {
    // no-op
  }
}
