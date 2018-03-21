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

package org.pentaho.di.core.encryption;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.util.StringUtil;

/**
 * This class handles basic encryption of passwords in Kettle. Note that it's not really encryption, it's more
 * obfuscation. Passwords are <b>difficult</b> to read, not impossible.
 *
 * @author Matt
 * @since 17-12-2003
 *
 */
public class KettleTwoWayPasswordEncoder implements TwoWayPasswordEncoderInterface {
  private static final int RADIX = 16;
  private static final String SEED = "0933910847463829827159347601486730416058";

  public KettleTwoWayPasswordEncoder() {
  }

  @Override
  public void init() throws KettleException {
    // Nothing to do here.
  }

  @Override
  public String encode( String rawPassword ) {
    return encode( rawPassword, true );
  }

  @Override
  public String encode( String rawPassword, boolean includePrefix ) {
    if ( includePrefix ) {
      return encryptPasswordIfNotUsingVariables( rawPassword );
    } else {
      return encryptPassword( rawPassword );
    }
  }

  @Override
  public String decode( String encodedPassword ) {

    if ( encodedPassword != null && encodedPassword.startsWith( PASSWORD_ENCRYPTED_PREFIX ) ) {
      encodedPassword = encodedPassword.substring( PASSWORD_ENCRYPTED_PREFIX.length() );
    }

    return decryptPassword( encodedPassword );
  }

  @Override
  public String decode( String encodedPassword, boolean optionallyEncrypted ) {

    if ( encodedPassword == null ) {
      return null;
    }

    if ( optionallyEncrypted ) {

      if ( encodedPassword.startsWith( PASSWORD_ENCRYPTED_PREFIX ) ) {
        encodedPassword = encodedPassword.substring( PASSWORD_ENCRYPTED_PREFIX.length() );
        return decryptPassword( encodedPassword );
      } else {
        return encodedPassword;
      }
    } else {
      return decryptPassword( encodedPassword );
    }
  }

  public static final String encryptPassword( String password ) {
    if ( password == null ) {
      return "";
    }
    if ( password.length() == 0 ) {
      return "";
    }

    BigInteger bi_passwd = new BigInteger( password.getBytes() );

    BigInteger bi_r0 = new BigInteger( SEED );
    BigInteger bi_r1 = bi_r0.xor( bi_passwd );

    return bi_r1.toString( RADIX );
  }

  public static final String decryptPassword( String encrypted ) {
    if ( encrypted == null ) {
      return "";
    }
    if ( encrypted.length() == 0 ) {
      return "";
    }

    BigInteger bi_confuse = new BigInteger( SEED );

    try {
      BigInteger bi_r1 = new BigInteger( encrypted, RADIX );
      BigInteger bi_r0 = bi_r1.xor( bi_confuse );

      return new String( bi_r0.toByteArray() );
    } catch ( Exception e ) {
      return "";
    }
  }

  /**
   * The word that is put before a password to indicate an encrypted form. If this word is not present, the password is
   * considered to be NOT encrypted
   */
  public static final String PASSWORD_ENCRYPTED_PREFIX = "Encrypted ";

  @Override
  public String[] getPrefixes() {
    return new String[] { PASSWORD_ENCRYPTED_PREFIX };
  }

  /**
   * Encrypt the password, but only if the password doesn't contain any variables.
   *
   * @param password
   *          The password to encrypt
   * @return The encrypted password or the
   */
  public static final String encryptPasswordIfNotUsingVariables( String password ) {
    String encrPassword = "";
    List<String> varList = new ArrayList<String>();
    StringUtil.getUsedVariables( password, varList, true );
    if ( varList.isEmpty() ) {
      encrPassword = PASSWORD_ENCRYPTED_PREFIX + KettleTwoWayPasswordEncoder.encryptPassword( password );
    } else {
      encrPassword = password;
    }

    return encrPassword;
  }

  /**
   * Decrypts a password if it contains the prefix "Encrypted "
   *
   * @param password
   *          The encrypted password
   * @return The decrypted password or the original value if the password doesn't start with "Encrypted "
   */
  public static final String decryptPasswordOptionallyEncrypted( String password ) {
    if ( !Const.isEmpty( password ) && password.startsWith( PASSWORD_ENCRYPTED_PREFIX ) ) {
      return KettleTwoWayPasswordEncoder.decryptPassword( password.substring( PASSWORD_ENCRYPTED_PREFIX.length() ) );
    }
    return password;
  }
}
