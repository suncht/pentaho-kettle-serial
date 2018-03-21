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

package org.pentaho.di.trans.step;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.pentaho.di.core.BlockingRowSet;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.encryption.CertificateGenEncryptUtil;
import org.pentaho.di.core.exception.KettleEOFException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.core.xml.XMLInterface;
import org.pentaho.di.www.SocketRepository;
import org.w3c.dom.Node;

/**
 * Defines and handles communication to and from remote steps.
 *
 * TODO: add compression as a parameter/option TODO add buffer size as a parameter
 *
 * @author Matt
 *
 */
public class RemoteStep implements Cloneable, XMLInterface, Comparable<RemoteStep> {

  public static final String XML_TAG = "remotestep";

  private static final long TIMEOUT_IN_SECONDS = 30;

  /** The target or source slave server with which we're exchanging data */
  private String targetSlaveServerName;

  /** The target or source host name */
  private String hostname;

  /** The remote host name */
  private String remoteHostname;

  /** The target or source port number for the data socket */
  private String port;

  private ServerSocket serverSocket;
  private Socket socket;

  private DataOutputStream outputStream;

  public AtomicBoolean stopped = new AtomicBoolean( false );

  private BaseStep baseStep;

  private DataInputStream inputStream;

  private String sourceStep;

  private int sourceStepCopyNr;

  private String targetStep;

  private int targetStepCopyNr;

  private int bufferSize;
  private boolean compressingStreams;

  private boolean encryptingStreams;
  private byte[] key;
  private CipherInputStream cipherInputStream;
  private CipherOutputStream cipherOutputStream;

  private GZIPOutputStream gzipOutputStream;

  private String sourceSlaveServerName;

  private GZIPInputStream gzipInputStream;

  private BufferedInputStream bufferedInputStream;

  protected BufferedOutputStream bufferedOutputStream;

  protected RowMetaInterface rowMeta;

  /**
   * @param hostname
   * @param remoteHostname
   * @param port
   * @param sourceStep
   * @param sourceStepCopyNr
   * @param targetStep
   * @param targetStepCopyNr
   * @param sourceSlaveServerName
   * @param targetSlaveServerName
   * @param bufferSize
   * @param compressingStreams
   * @param rowMeta
   *          The expected row layout to pass through this step. (input or output)
   */
  public RemoteStep( String hostname, String remoteHostname, String port, String sourceStep, int sourceStepCopyNr,
    String targetStep, int targetStepCopyNr, String sourceSlaveServerName, String targetSlaveServerName,
    int bufferSize, boolean compressingStreams, RowMetaInterface rowMeta ) {
    super();
    this.hostname = hostname;
    this.remoteHostname = remoteHostname;
    this.port = port;
    this.sourceStep = sourceStep;
    this.sourceStepCopyNr = sourceStepCopyNr;
    this.targetStep = targetStep;
    this.targetStepCopyNr = targetStepCopyNr;
    this.bufferSize = bufferSize;
    this.compressingStreams = compressingStreams;

    this.sourceSlaveServerName = sourceSlaveServerName;
    this.targetSlaveServerName = targetSlaveServerName;

    this.rowMeta = rowMeta;

    if ( sourceStep.equals( targetStep ) && sourceStepCopyNr == targetStepCopyNr ) {
      throw new RuntimeException(
        "The source and target step/copy can't be the same for a remote step definition." );
    }
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch ( CloneNotSupportedException e ) {
      return null;
    }
  }

  public String getXML() {
    StringBuffer xml = new StringBuffer();
    xml.append( XMLHandler.openTag( XML_TAG ) );

    xml.append( XMLHandler.addTagValue( "hostname", hostname, false ) );
    xml.append( XMLHandler.addTagValue( "remote_hostname", remoteHostname, false ) );
    xml.append( XMLHandler.addTagValue( "port", port, false ) );
    xml.append( XMLHandler.addTagValue( "buffer_size", bufferSize, false ) );
    xml.append( XMLHandler.addTagValue( "compressed_streams", compressingStreams, false ) );

    xml.append( XMLHandler.addTagValue( "source_step_name", sourceStep, false ) );
    xml.append( XMLHandler.addTagValue( "source_step_copy", sourceStepCopyNr, false ) );
    xml.append( XMLHandler.addTagValue( "target_step_name", targetStep, false ) );
    xml.append( XMLHandler.addTagValue( "target_step_copy", targetStepCopyNr, false ) );

    xml.append( XMLHandler.addTagValue( "source_slave_server_name", sourceSlaveServerName, false ) );
    xml.append( XMLHandler.addTagValue( "target_slave_server_name", targetSlaveServerName, false ) );

    if ( rowMeta != null ) {
      try {
        xml.append( rowMeta.getMetaXML() );
      } catch ( IOException e ) {
        throw new RuntimeException( "Unexpected error encountered, probably encoding/decoding base64 data", e );
      }
    }
    xml.append( XMLHandler.addTagValue( "encrypted_streams", encryptingStreams, false ) );
    try {
      xml.append( XMLHandler.addTagValue( "key", key ) );
    } catch ( Exception ex ) {
      baseStep.logError( "Unable to parse key", ex );
    }
    xml.append( XMLHandler.closeTag( XML_TAG ) );
    return xml.toString();
  }

  public RemoteStep( Node node ) throws KettleException {

    hostname = XMLHandler.getTagValue( node, "hostname" );
    remoteHostname = XMLHandler.getTagValue( node, "remote_hostname" );
    port = XMLHandler.getTagValue( node, "port" );
    bufferSize = Integer.parseInt( XMLHandler.getTagValue( node, "buffer_size" ) );
    compressingStreams = "Y".equalsIgnoreCase( XMLHandler.getTagValue( node, "compressed_streams" ) );

    sourceStep = XMLHandler.getTagValue( node, "source_step_name" );
    sourceStepCopyNr = Integer.parseInt( XMLHandler.getTagValue( node, "source_step_copy" ) );
    targetStep = XMLHandler.getTagValue( node, "target_step_name" );
    targetStepCopyNr = Integer.parseInt( XMLHandler.getTagValue( node, "target_step_copy" ) );

    sourceSlaveServerName = XMLHandler.getTagValue( node, "source_slave_server_name" );
    targetSlaveServerName = XMLHandler.getTagValue( node, "target_slave_server_name" );

    Node rowMetaNode = XMLHandler.getSubNode( node, RowMeta.XML_META_TAG );
    if ( rowMetaNode == null ) {
      rowMeta = new RowMeta();
    } else {
      rowMeta = new RowMeta( rowMetaNode );
    }
    encryptingStreams = "Y".equalsIgnoreCase( XMLHandler.getTagValue( node, "encrypted_streams" ) );
    key = XMLHandler.stringToBinary( XMLHandler.getTagValue( node, "key" ) );
  }

  @Override
  public String toString() {
    return hostname
      + ":" + port + " (" + sourceSlaveServerName + "/" + sourceStep + "." + sourceStepCopyNr + " --> "
      + targetSlaveServerName + "/" + targetStep + "." + targetStepCopyNr + ")";
  }

  @Override
  public boolean equals( Object obj ) {
    return toString().equalsIgnoreCase( obj.toString() );
  }

  public int compareTo( RemoteStep remoteStep ) {
    return toString().compareTo( remoteStep.toString() );
  }

  /**
   * @return the host name
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * @param hostname
   *          the host name to set
   */
  public void setHostname( String hostname ) {
    this.hostname = hostname;
  }

  /**
   * int
   *
   * @return the port
   */
  public String getPort() {
    return port;
  }

  /**
   * @param port
   *          the port to set
   */
  public void setPort( String port ) {
    this.port = port;
  }

  public synchronized void openServerSocket( BaseStep baseStep ) throws IOException {
    this.baseStep = baseStep;
    int portNumber = Integer.parseInt( baseStep.environmentSubstitute( port ) );

    SocketRepository socketRepository = baseStep.getSocketRepository();
    serverSocket =
      socketRepository.openServerSocket( portNumber, baseStep.getTransMeta().getName()
        + " - " + baseStep.toString() );

    // Add this socket to the steps server socket list
    // That way, the socket can be closed during transformation cleanup
    // That is called when the cluster has finished processing.
    //
    baseStep.getServerSockets().add( serverSocket );
  }

  /**
   * @return the serverSocket that is created by the open server socket method.
   */
  public ServerSocket getServerSocket() {
    return serverSocket;
  }

  /**
   * @return the socket
   */
  public Socket getSocket() {
    return socket;
  }

  /**
   * @param socket
   *          the socket to set
   */
  public void setSocket( Socket socket ) {
    this.socket = socket;
  }

  /**
   * Open a socket for writing.
   *
   * @return the RowSet created that will accept the rows for the remote step
   * @throws IOException
   */
  public synchronized BlockingRowSet openWriterSocket() throws IOException {

    // Create an output row set: to be added to BaseStep.outputRowSets
    //
    final BlockingRowSet rowSet = new BlockingRowSet( baseStep.getTransMeta().getSizeRowset() );

    // Set the details for the source and target step as well as the target slave server.
    // This will help us determine the pre-calculated partition nr later in the game. (putRow())
    //
    rowSet.setThreadNameFromToCopy( sourceStep, sourceStepCopyNr, targetStep, targetStepCopyNr );
    rowSet.setRemoteSlaveServerName( targetSlaveServerName );

    // Start a thread that will read out the output row set and send the data over the wire...
    // This will make everything else transparent, copying, distributing, including partitioning, etc.
    //
    Runnable runnable = new Runnable() {

      public void run() {
        try {
          // Accept the socket, create a connection
          // This blocks until something comes through...
          //
          socket = serverSocket.accept();

          // Create the output stream...
          OutputStream socketOut = socket.getOutputStream();

          if ( compressingStreams ) {
            gzipOutputStream = new GZIPOutputStream( socketOut, 50000 );
            bufferedOutputStream = new BufferedOutputStream( gzipOutputStream, bufferSize );
          } else {
            bufferedOutputStream = new BufferedOutputStream( socketOut, bufferSize );
          }
          socketOut = bufferedOutputStream;
          if ( encryptingStreams && key != null ) {
            byte[] transKey = baseStep.getTransMeta().getKey();
            Key unwrappedKey = null;
            try {
              unwrappedKey = CertificateGenEncryptUtil.decodeTransmittedKey( transKey, key,
                baseStep.getTransMeta().isPrivateKey() );
            } catch ( InvalidKeyException ex ) {
              baseStep.logError( "Invalid key was received", ex );
            } catch ( InvalidKeySpecException ex ) {
              baseStep.logError( "Invalid key specification was received. Most probably public key was "
                  + "sent instead of private or vice versa", ex );
            } catch ( Exception ex ) {
              baseStep.logError( "Error occurred during encryption initialization", ex );
            }
            try {
              Cipher decryptionCip = CertificateGenEncryptUtil.initDecryptionCipher( unwrappedKey, key );
              socketOut = cipherOutputStream = new CipherOutputStream( bufferedOutputStream, decryptionCip );
            } catch ( InvalidKeyException ex ) {
              baseStep.logError( "Invalid key was received", ex );
            } catch ( Exception ex ) {
              baseStep.logError( "Error occurred during encryption initialization", ex );
            }
          }
          outputStream = new DataOutputStream( socketOut );

          baseStep.logBasic( "Server socket accepted for port ["
            + port + "], reading from server " + targetSlaveServerName );

          // get a row of data...
          Object[] rowData = baseStep.getRowFrom( rowSet );
          if ( rowData != null ) {
            rowSet.getRowMeta().writeMeta( outputStream );
          }

          // Send that row to the remote step
          //
          while ( rowData != null && !baseStep.isStopped() ) {
            // It's too confusing to count these twice, so decrement
            baseStep.decrementLinesRead();
            baseStep.decrementLinesWritten();

            // Write the row to the remote step via the output stream....
            //
            rowSet.getRowMeta().writeData( outputStream, rowData );
            baseStep.incrementLinesOutput();

            if ( baseStep.log.isDebug() ) {
              baseStep.logDebug( "Sent row to port " + port + " : " + rowSet.getRowMeta().getString( rowData ) );
            }
            rowData = baseStep.getRowFrom( rowSet );
          }

          if ( compressingStreams ) {
            outputStream.flush();
            gzipOutputStream.finish();
          } else {
            outputStream.flush();
          }

        } catch ( Exception e ) {
          baseStep.logError( "Error writing to remote step", e );
          baseStep.setErrors( 1 );
          baseStep.stopAll();
        } finally {
          try {
            if ( socket != null ) {
              socket.shutdownOutput();
            }
          } catch ( Exception e ) {
            baseStep.logError( "Error shutting down output channel on the server socket of remote step", e );
            baseStep.setErrors( 1L );
            baseStep.stopAll();
          }
          try {
            if ( outputStream != null ) {
              outputStream.flush();
              outputStream.close();
              if ( cipherOutputStream != null ) {
                cipherOutputStream.close();
              }
              bufferedOutputStream.close();
              if ( gzipOutputStream != null ) {
                gzipOutputStream.close();
              }
            }
          } catch ( Exception e ) {
            baseStep.logError( "Error shutting down output streams on the server socket of remote step", e );
            baseStep.setErrors( 1L );
            baseStep.stopAll();
          }
          outputStream = null;
          bufferedOutputStream = null;
          gzipOutputStream = null;
          cipherOutputStream = null;

          //
          // Now we can't close the server socket.
          // This would immediately kill all the remaining data on the client side.
          // The close of the server socket will happen when all the transformation in the cluster have finished.
          // Then Trans.cleanup() will be called.
        }
      }
    };

    // Fire this off in the in a separate thread...
    //
    new Thread( runnable ).start();

    // Return the rowSet to be added to the output row set of baseStep
    //
    return rowSet;
  }

  /**
   * Close left-over sockets, streams and so on.
   */
  public void cleanup() {
    if ( socket != null && socket.isConnected() && !socket.isClosed() ) {
      try {
        if ( socket != null && !socket.isOutputShutdown() ) {
          socket.shutdownOutput();
        }
        if ( socket != null && !socket.isInputShutdown() ) {
          socket.shutdownInput();
        }
        if ( socket != null && !socket.isClosed() ) {
          socket.close();
        }

        if ( bufferedInputStream != null ) {
          bufferedInputStream.close();
          bufferedInputStream = null;
        }
        if ( gzipInputStream != null ) {
          gzipInputStream.close();
          gzipInputStream = null;
        }
        if ( cipherInputStream != null ) {
          cipherInputStream.close();
          cipherInputStream = null;
        }
        if ( inputStream != null ) {
          inputStream.close();
          inputStream = null;
        }
        if ( gzipOutputStream != null ) {
          gzipOutputStream.close();
          gzipOutputStream = null;
        }
        if ( bufferedOutputStream != null ) {
          bufferedOutputStream.close();
          bufferedOutputStream = null;
        }
        if ( cipherOutputStream != null ) {
          cipherOutputStream.close();
          cipherOutputStream = null;
        }
        if ( outputStream != null ) {
          outputStream.close();
          outputStream = null;
        }
      } catch ( Exception e ) {
        baseStep.logError( "Error closing socket", e );
      }
    }
  }

  private Object[] getRowOfData( RowMetaInterface rowMeta ) throws KettleFileException {
    Object[] rowData = null;

    while ( !baseStep.isStopped() && rowData == null ) {
      try {
        rowData = rowMeta.readData( inputStream );
      } catch ( SocketTimeoutException e ) {
        rowData = null; // try again.
      }
    }

    return rowData;
  }

  public synchronized BlockingRowSet openReaderSocket( final BaseStep baseStep ) throws IOException,
    KettleException {
    this.baseStep = baseStep;

    final BlockingRowSet rowSet = new BlockingRowSet( baseStep.getTransMeta().getSizeRowset() );

    // Make sure we handle the case with multiple step copies running on a
    // slave...
    //
    rowSet.setThreadNameFromToCopy( sourceStep, sourceStepCopyNr, targetStep, targetStepCopyNr );
    rowSet.setRemoteSlaveServerName( targetSlaveServerName );

    final int portNumber = Integer.parseInt( baseStep.environmentSubstitute( port ) );
    final String realHostname = baseStep.environmentSubstitute( hostname );

    // Connect to the server socket (started during BaseStep.init())
    // Because the accept() call on the server socket can be called after we
    // reached this code
    // it is best to build in a retry loop with a time-out here.
    //
    long startTime = System.currentTimeMillis();
    boolean connected = false;
    KettleException lastException = null;

    // // timeout with retry until connected
    while ( !connected
      && ( TIMEOUT_IN_SECONDS > ( System.currentTimeMillis() - startTime ) / 1000 ) && !baseStep.isStopped() ) {
      try {
        socket = new Socket();
        socket.setReuseAddress( true );

        baseStep.logDetailed( "Step variable MASTER_HOST : [" + baseStep.getVariable( "MASTER_HOST" ) + "]" );
        baseStep.logDetailed( "Opening client (reader) socket to server ["
          + Const.NVL( realHostname, "" ) + ":" + port + "]" );
        socket.connect( new InetSocketAddress( realHostname, portNumber ), 5000 );

        connected = true;

        InputStream socketStream = socket.getInputStream();
        if ( compressingStreams ) {
          gzipInputStream = new GZIPInputStream( socketStream );
          bufferedInputStream = new BufferedInputStream( gzipInputStream, bufferSize );
        } else {
          bufferedInputStream = new BufferedInputStream( socketStream, bufferSize );
        }
        socketStream = bufferedInputStream;

        if ( encryptingStreams && key != null ) {
          byte[] transKey = baseStep.getTransMeta().getKey();
          Key unwrappedKey = null;
          try {
            unwrappedKey = CertificateGenEncryptUtil.decodeTransmittedKey( transKey, key,
              baseStep.getTransMeta().isPrivateKey() );
          } catch ( InvalidKeyException ex ) {
            baseStep.logError( "Invalid key was received", ex );
          } catch ( InvalidKeySpecException ex ) {
            baseStep.logError( "Invalid key specification was received. Most probably public key was "
                + "sent instead of private or vice versa", ex );
          } catch ( Exception ex ) {
            baseStep.logError( "Error occurred during encryption initialization", ex );
          }
          try {
            Cipher decryptionCip = CertificateGenEncryptUtil.initDecryptionCipher( unwrappedKey, key );
            socketStream = cipherInputStream = new CipherInputStream( bufferedInputStream, decryptionCip );
          } catch ( InvalidKeyException ex ) {
            baseStep.logError( "Invalid key was received", ex );
          } catch ( Exception ex ) {
            baseStep.logError( "Error occurred during encryption initialization", ex );
          }
        }
        inputStream = new DataInputStream( socketStream );

        lastException = null;
      } catch ( Exception e ) {
        lastException =
          new KettleException( "Unable to open socket to server " + realHostname + " port " + portNumber, e );
      }
      if ( lastException != null ) {
        // Sleep for a while
        try {
          Thread.sleep( 250 );
        } catch ( InterruptedException e ) {
          if ( socket != null ) {
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
            baseStep.logDetailed( "Closed connection to server socket to read rows from remote step on server "
              + realHostname + " port " + portNumber + " - Local port=" + socket.getLocalPort() );
          }

          throw new KettleException( "Interrupted while trying to connect to server socket: " + e.toString() );
        }
      }
    }

    // See if all was OK...
    if ( lastException != null ) {

      baseStep.logError( "Error initialising step: " + lastException.toString() );
      if ( socket != null ) {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
        baseStep.logDetailed( "Closed connection to server socket to read rows from remote step on server "
          + realHostname + " port " + portNumber + " - Local port=" + socket.getLocalPort() );
      }
      throw lastException;
    } else {
      if ( inputStream == null ) {
        throw new KettleException( "Unable to connect to the SocketWriter in the "
          + TIMEOUT_IN_SECONDS + "s timeout period." );
      }
    }

    baseStep.logDetailed( "Opened connection to server socket to read rows from remote step on server "
      + realHostname + " port " + portNumber + " - Local port=" + socket.getLocalPort() );

    // Create a thread to take care of the reading from the client socket.
    // The rows read will be put in a RowSet buffer.
    // That buffer will hand over the rows to the step that has this RemoteStep
    // object defined
    // as a remote input step.
    //
    Runnable runnable = new Runnable() {
      public void run() {
        try {

          // First read the row meta data from the socket...
          //
          RowMetaInterface rowMeta = null;
          while ( !baseStep.isStopped() && rowMeta == null ) {
            try {
              rowMeta = new RowMeta( inputStream );
            } catch ( SocketTimeoutException e ) {
              rowMeta = null;
            }
          }

          if ( rowMeta == null ) {
            throw new KettleEOFException(); // leave now.
          }

          // And a first row of data...
          //
          Object[] rowData = getRowOfData( rowMeta );

          // Now get the data itself, row by row...
          //
          while ( rowData != null && !baseStep.isStopped() ) {
            baseStep.incrementLinesInput();
            baseStep.decrementLinesRead();

            if ( baseStep.log.isDebug() ) {
              baseStep.logDebug( "Received row from remote step: " + rowMeta.getString( rowData ) );
            }

            baseStep.putRowTo( rowMeta, rowData, rowSet );
            baseStep.decrementLinesWritten();
            rowData = getRowOfData( rowMeta );
          }
        } catch ( KettleEOFException e ) {
          // Nothing, we're simply done reading...
          //
          if ( baseStep.log.isDebug() ) {
            baseStep.logDebug( "Finished reading from remote step on server " + hostname + " port " + portNumber );
          }

        } catch ( Exception e ) {
          baseStep.logError( "Error reading from client socket to remote step", e );
          baseStep.setErrors( 1 );
          baseStep.stopAll();
        } finally {
          // Close the input socket
          if ( socket != null && !socket.isClosed() && !socket.isInputShutdown() ) {
            try {
              socket.shutdownInput();
            } catch ( Exception e ) {
              baseStep
                .logError( "Error shutting down input channel on client socket connection to remote step", e );
            }
          }
          if ( socket != null && !socket.isClosed() && !socket.isOutputShutdown() ) {
            try {
              socket.shutdownOutput();
            } catch ( Exception e ) {
              baseStep.logError(
                "Error shutting down output channel on client socket connection to remote step", e );
            }
          }
          if ( socket != null && !socket.isClosed() ) {
            try {
              socket.close();
            } catch ( Exception e ) {
              baseStep.logError( "Error shutting down client socket connection to remote step", e );
            }
          }
          if ( inputStream != null ) {
            try {
              inputStream.close();
            } catch ( Exception e ) {
              baseStep.logError( "Error closing input stream on socket connection to remote step", e );
            }
            inputStream = null;
          }
          if ( cipherInputStream != null ) {
            try {
              cipherInputStream.close();
            } catch ( Exception e ) {
              baseStep.logError( "Error closing input stream on socket connection to remote step", e );
            }
          }
          cipherInputStream = null;
          if ( bufferedInputStream != null ) {
            try {
              bufferedInputStream.close();
            } catch ( Exception e ) {
              baseStep.logError( "Error closing input stream on socket connection to remote step", e );
            }
          }
          bufferedInputStream = null;
          if ( gzipInputStream != null ) {
            try {
              gzipInputStream.close();
            } catch ( Exception e ) {
              baseStep.logError( "Error closing input stream on socket connection to remote step", e );
            }
          }
          gzipInputStream = null;
          baseStep.logDetailed( "Closed connection to server socket to read rows from remote step on server "
            + realHostname + " port " + portNumber + " - Local port=" + socket.getLocalPort() );
        }

        // signal baseStep that nothing else comes from this step.
        //
        rowSet.setDone();
      }
    };
    new Thread( runnable ).start();

    return rowSet;
  }

  /**
   * @return the sourceStep
   */
  public String getSourceStep() {
    return sourceStep;
  }

  /**
   * @param sourceStep
   *          the sourceStep to set
   */
  public void setSourceStep( String sourceStep ) {
    this.sourceStep = sourceStep;
  }

  /**
   * @return the targetStep
   */
  public String getTargetStep() {
    return targetStep;
  }

  /**
   * @param targetStep
   *          the targetStep to set
   */
  public void setTargetStep( String targetStep ) {
    this.targetStep = targetStep;
  }

  /**
   * @return the targetSlaveServerName
   */
  public String getTargetSlaveServerName() {
    return targetSlaveServerName;
  }

  /**
   * @param targetSlaveServerName
   *          the targetSlaveServerName to set
   */
  public void setTargetSlaveServerName( String targetSlaveServerName ) {
    this.targetSlaveServerName = targetSlaveServerName;
  }

  /**
   * @return the sourceStepCopyNr
   */
  public int getSourceStepCopyNr() {
    return sourceStepCopyNr;
  }

  /**
   * @param sourceStepCopyNr
   *          the sourceStepCopyNr to set
   */
  public void setSourceStepCopyNr( int sourceStepCopyNr ) {
    this.sourceStepCopyNr = sourceStepCopyNr;
  }

  /**
   * @return the targetStepCopyNr
   */
  public int getTargetStepCopyNr() {
    return targetStepCopyNr;
  }

  /**
   * @param targetStepCopyNr
   *          the targetStepCopyNr to set
   */
  public void setTargetStepCopyNr( int targetStepCopyNr ) {
    this.targetStepCopyNr = targetStepCopyNr;
  }

  /**
   * @return the bufferSize
   */
  public int getBufferSize() {
    return bufferSize;
  }

  /**
   * @param bufferSize
   *          the bufferSize to set
   */
  public void setBufferSize( int bufferSize ) {
    this.bufferSize = bufferSize;
  }

  /**
   * @return the compressingStreams
   */
  public boolean isCompressingStreams() {
    return compressingStreams;
  }

  /**
   * @param compressingStreams
   *          the compressingStreams to set
   */
  public void setCompressingStreams( boolean compressingStreams ) {
    this.compressingStreams = compressingStreams;
  }

  /**
   * @return the remoteHostname
   */
  public String getRemoteHostname() {
    return remoteHostname;
  }

  /**
   * @param remoteHostname
   *          the remoteHostname to set
   */
  public void setRemoteHostname( String remoteHostname ) {
    this.remoteHostname = remoteHostname;
  }

  /**
   * @return the sourceSlaveServer name
   */
  public String getSourceSlaveServerName() {
    return sourceSlaveServerName;
  }

  /**
   * @param sourceSlaveServerName
   *          the sourceSlaveServerName to set
   */
  public void setSourceSlaveServerName( String sourceSlaveServerName ) {
    this.sourceSlaveServerName = sourceSlaveServerName;
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      if ( socket != null ) {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
      }
      if ( serverSocket != null ) {
        serverSocket.close();
      }
    } catch ( IOException e ) {
      // Ignore errors
    } finally {
      super.finalize();
    }
  }

  public RowMetaInterface getRowMeta() {
    return rowMeta;
  }

  public void setRowMeta( RowMetaInterface rowMeta ) {
    this.rowMeta = rowMeta;
  }

  public boolean isEncryptingStreams() {
    return encryptingStreams;
  }

  public void setEncryptingStreams( boolean encryptingStreams ) {
    this.encryptingStreams = encryptingStreams;
  }

  public byte[] getKey() {
    return key;
  }

  public void setKey( byte[] key ) {
    this.key = key;
  }

}
