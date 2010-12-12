/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for working with character sets. It is somewhat similar to
 * the Java 1.4 <code>java.nio.charset.Charset</code> class but knows many
 * more aliases and is compatible with Java 1.3. It will use a simple detection
 * mechanism to detect what character sets the current VM supports. This will
 * be a sub-set of the character sets listed in the
 * <a href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">
 * Java 1.5 (J2SE5.0) Supported Encodings</a> document.
 * <p>
 * The <a href="http://www.iana.org/assignments/character-sets">
 * IANA Character Sets</a> document has been used to determine the preferred
 * MIME character set names and to get a list of known aliases.
 * <p>
 * This is a complete list of the character sets known to this class:
 * <table>
 *     <tr>
 *         <td>Canonical (Java) name</td>
 *         <td>MIME preferred</td>
 *         <td>Aliases</td>
 *     </tr>
 *     <tr>
 *         <td>ASCII</td>
 *         <td>US-ASCII</td>
 *         <td>ANSI_X3.4-1968 iso-ir-6 ANSI_X3.4-1986 ISO_646.irv:1991 ISO646-US us IBM367 cp367 csASCII ascii7 646 iso_646.irv:1983 </td>
 *     </tr>
 *     <tr>
 *         <td>Big5</td>
 *         <td>Big5</td>
 *         <td>csBig5 CN-Big5 BIG-FIVE BIGFIVE </td>
 *     </tr>
 *     <tr>
 *         <td>Big5_HKSCS</td>
 *         <td>Big5-HKSCS</td>
 *         <td>big5hkscs </td>
 *     </tr>
 *     <tr>
 *         <td>Big5_Solaris</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp037</td>
 *         <td>IBM037</td>
 *         <td>ebcdic-cp-us ebcdic-cp-ca ebcdic-cp-wt ebcdic-cp-nl csIBM037 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1006</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1025</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1026</td>
 *         <td>IBM1026</td>
 *         <td>csIBM1026 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1046</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1047</td>
 *         <td>IBM1047</td>
 *         <td>IBM-1047 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1097</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1098</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1112</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1122</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1123</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1124</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1140</td>
 *         <td>IBM01140</td>
 *         <td>CCSID01140 CP01140 ebcdic-us-37+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1141</td>
 *         <td>IBM01141</td>
 *         <td>CCSID01141 CP01141 ebcdic-de-273+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1142</td>
 *         <td>IBM01142</td>
 *         <td>CCSID01142 CP01142 ebcdic-dk-277+euro ebcdic-no-277+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1143</td>
 *         <td>IBM01143</td>
 *         <td>CCSID01143 CP01143 ebcdic-fi-278+euro ebcdic-se-278+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1144</td>
 *         <td>IBM01144</td>
 *         <td>CCSID01144 CP01144 ebcdic-it-280+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1145</td>
 *         <td>IBM01145</td>
 *         <td>CCSID01145 CP01145 ebcdic-es-284+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1146</td>
 *         <td>IBM01146</td>
 *         <td>CCSID01146 CP01146 ebcdic-gb-285+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1147</td>
 *         <td>IBM01147</td>
 *         <td>CCSID01147 CP01147 ebcdic-fr-297+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1148</td>
 *         <td>IBM01148</td>
 *         <td>CCSID01148 CP01148 ebcdic-international-500+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1149</td>
 *         <td>IBM01149</td>
 *         <td>CCSID01149 CP01149 ebcdic-is-871+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp1250</td>
 *         <td>windows-1250</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1251</td>
 *         <td>windows-1251</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1252</td>
 *         <td>windows-1252</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1253</td>
 *         <td>windows-1253</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1254</td>
 *         <td>windows-1254</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1255</td>
 *         <td>windows-1255</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1256</td>
 *         <td>windows-1256</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1257</td>
 *         <td>windows-1257</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1258</td>
 *         <td>windows-1258</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1381</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp1383</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp273</td>
 *         <td>IBM273</td>
 *         <td>csIBM273 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp277</td>
 *         <td>IBM277</td>
 *         <td>EBCDIC-CP-DK EBCDIC-CP-NO csIBM277 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp278</td>
 *         <td>IBM278</td>
 *         <td>CP278 ebcdic-cp-fi ebcdic-cp-se csIBM278 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp280</td>
 *         <td>IBM280</td>
 *         <td>ebcdic-cp-it csIBM280 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp284</td>
 *         <td>IBM284</td>
 *         <td>ebcdic-cp-es csIBM284 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp285</td>
 *         <td>IBM285</td>
 *         <td>ebcdic-cp-gb csIBM285 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp297</td>
 *         <td>IBM297</td>
 *         <td>ebcdic-cp-fr csIBM297 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp33722</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp420</td>
 *         <td>IBM420</td>
 *         <td>ebcdic-cp-ar1 csIBM420 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp424</td>
 *         <td>IBM424</td>
 *         <td>ebcdic-cp-he csIBM424 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp437</td>
 *         <td>IBM437</td>
 *         <td>437 csPC8CodePage437 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp500</td>
 *         <td>IBM500</td>
 *         <td>ebcdic-cp-be ebcdic-cp-ch csIBM500 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp737</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp775</td>
 *         <td>IBM775</td>
 *         <td>csPC775Baltic </td>
 *     </tr>
 *     <tr>
 *         <td>Cp838</td>
 *         <td>IBM-Thai</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp850</td>
 *         <td>IBM850</td>
 *         <td>850 csPC850Multilingual </td>
 *     </tr>
 *     <tr>
 *         <td>Cp852</td>
 *         <td>IBM852</td>
 *         <td>852 csPCp852 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp855</td>
 *         <td>IBM855</td>
 *         <td>855 csIBM855 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp856</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp857</td>
 *         <td>IBM857</td>
 *         <td>857 csIBM857 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp858</td>
 *         <td>IBM00858</td>
 *         <td>CCSID00858 CP00858 PC-Multilingual-850+euro </td>
 *     </tr>
 *     <tr>
 *         <td>Cp860</td>
 *         <td>IBM860</td>
 *         <td>860 csIBM860 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp861</td>
 *         <td>IBM861</td>
 *         <td>861 cp-is csIBM861 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp862</td>
 *         <td>IBM862</td>
 *         <td>862 csPC862LatinHebrew </td>
 *     </tr>
 *     <tr>
 *         <td>Cp863</td>
 *         <td>IBM863</td>
 *         <td>863 csIBM863 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp864</td>
 *         <td>IBM864</td>
 *         <td>cp864 csIBM864 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp865</td>
 *         <td>IBM865</td>
 *         <td>865 csIBM865 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp866</td>
 *         <td>IBM866</td>
 *         <td>866 csIBM866 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp868</td>
 *         <td>IBM868</td>
 *         <td>cp-ar csIBM868 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp869</td>
 *         <td>IBM869</td>
 *         <td>cp-gr csIBM869 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp870</td>
 *         <td>IBM870</td>
 *         <td>ebcdic-cp-roece ebcdic-cp-yu csIBM870 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp871</td>
 *         <td>IBM871</td>
 *         <td>ebcdic-cp-is csIBM871 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp875</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp918</td>
 *         <td>IBM918</td>
 *         <td>ebcdic-cp-ar2 csIBM918 </td>
 *     </tr>
 *     <tr>
 *         <td>Cp921</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp922</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp930</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp933</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp935</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp937</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp939</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp942</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp942C</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp943</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp943C</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp948</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp949</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp949C</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp950</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp964</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>Cp970</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>EUC_CN</td>
 *         <td>GB2312</td>
 *         <td>x-EUC-CN csGB2312 euccn euc-cn gb2312-80 gb2312-1980 CN-GB CN-GB-ISOIR165 </td>
 *     </tr>
 *     <tr>
 *         <td>EUC_JP</td>
 *         <td>EUC-JP</td>
 *         <td>csEUCPkdFmtJapanese Extended_UNIX_Code_Packed_Format_for_Japanese eucjis x-eucjp eucjp x-euc-jp </td>
 *     </tr>
 *     <tr>
 *         <td>EUC_JP_LINUX</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>EUC_JP_Solaris</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>EUC_KR</td>
 *         <td>EUC-KR</td>
 *         <td>csEUCKR ksc5601 5601 ksc5601_1987 ksc_5601 ksc5601-1987 ks_c_5601-1987 euckr </td>
 *     </tr>
 *     <tr>
 *         <td>EUC_TW</td>
 *         <td>EUC-TW</td>
 *         <td>x-EUC-TW cns11643 euctw </td>
 *     </tr>
 *     <tr>
 *         <td>GB18030</td>
 *         <td>GB18030</td>
 *         <td>gb18030-2000 </td>
 *     </tr>
 *     <tr>
 *         <td>GBK</td>
 *         <td>windows-936</td>
 *         <td>CP936 MS936 ms_936 x-mswin-936 x-gbk</td>
 *     </tr>
 *     <tr>
 *         <td>ISCII91</td>
 *         <td>?</td>
 *         <td>x-ISCII91 iscii </td>
 *     </tr>
 *     <tr>
 *         <td>ISO2022CN</td>
 *         <td>ISO-2022-CN</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>ISO2022JP</td>
 *         <td>ISO-2022-JP</td>
 *         <td>csISO2022JP JIS jis_encoding csjisencoding </td>
 *     </tr>
 *     <tr>
 *         <td>ISO2022KR</td>
 *         <td>ISO-2022-KR</td>
 *         <td>csISO2022KR </td>
 *     </tr>
 *     <tr>
 *         <td>ISO2022_CN_CNS</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>ISO2022_CN_GB</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_1</td>
 *         <td>ISO-8859-1</td>
 *         <td>ISO_8859-1:1987 iso-ir-100 ISO_8859-1 latin1 l1 IBM819 CP819 csISOLatin1 8859_1 819 IBM-819 ISO8859-1 ISO_8859_1 </td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_13</td>
 *         <td>ISO-8859-13</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_15</td>
 *         <td>ISO-8859-15</td>
 *         <td>ISO_8859-15 Latin-9 8859_15 csISOlatin9 IBM923 cp923 923 L9 IBM-923 ISO8859-15 LATIN9 LATIN0 csISOlatin0 ISO8859_15_FDIS </td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_2</td>
 *         <td>ISO-8859-2</td>
 *         <td>ISO_8859-2:1987 iso-ir-101 ISO_8859-2 latin2 l2 csISOLatin2 8859_2 iso8859_2 </td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_3</td>
 *         <td>ISO-8859-3</td>
 *         <td>ISO_8859-3:1988 iso-ir-109 ISO_8859-3 latin3 l3 csISOLatin3 8859_3 </td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_4</td>
 *         <td>ISO-8859-4</td>
 *         <td>ISO_8859-4:1988 iso-ir-110 ISO_8859-4 latin4 l4 csISOLatin4 8859_4 </td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_5</td>
 *         <td>ISO-8859-5</td>
 *         <td>ISO_8859-5:1988 iso-ir-144 ISO_8859-5 cyrillic csISOLatinCyrillic 8859_5 </td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_6</td>
 *         <td>ISO-8859-6</td>
 *         <td>ISO_8859-6:1987 iso-ir-127 ISO_8859-6 ECMA-114 ASMO-708 arabic csISOLatinArabic 8859_6 </td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_7</td>
 *         <td>ISO-8859-7</td>
 *         <td>ISO_8859-7:1987 iso-ir-126 ISO_8859-7 ELOT_928 ECMA-118 greek greek8 csISOLatinGreek 8859_7 sun_eu_greek </td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_8</td>
 *         <td>ISO-8859-8</td>
 *         <td>ISO_8859-8:1988 iso-ir-138 ISO_8859-8 hebrew csISOLatinHebrew 8859_8 </td>
 *     </tr>
 *     <tr>
 *         <td>ISO8859_9</td>
 *         <td>ISO-8859-9</td>
 *         <td>ISO_8859-9:1989 iso-ir-148 ISO_8859-9 latin5 l5 csISOLatin5 8859_9 </td>
 *     </tr>
 *     <tr>
 *         <td>JISAutoDetect</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>JIS_C6626-1983</td>
 *         <td>JIS_C6626-1983</td>
 *         <td>x-JIS0208 JIS0208 csISO87JISX0208 x0208 JIS_X0208-1983 iso-ir-87 </td>
 *     </tr>
 *     <tr>
 *         <td>JIS_X0201</td>
 *         <td>JIS_X0201</td>
 *         <td>X0201 JIS0201 csHalfWidthKatakana </td>
 *     </tr>
 *     <tr>
 *         <td>JIS_X0212-1990</td>
 *         <td>JIS_X0212-1990</td>
 *         <td>iso-ir-159 x0212 JIS0212 csISO159JISX02121990 </td>
 *     </tr>
 *     <tr>
 *         <td>KOI8_R</td>
 *         <td>KOI8-R</td>
 *         <td>csKOI8R koi8 </td>
 *     </tr>
 *     <tr>
 *         <td>MS874</td>
 *         <td>windows-874</td>
 *         <td>cp874 </td>
 *     </tr>
 *     <tr>
 *         <td>MS932</td>
 *         <td>Windows-31J</td>
 *         <td>windows-932 csWindows31J x-ms-cp932 </td>
 *     </tr>
 *     <tr>
 *         <td>MS949</td>
 *         <td>windows-949</td>
 *         <td>windows949 ms_949 x-windows-949 </td>
 *     </tr>
 *     <tr>
 *         <td>MS950</td>
 *         <td>windows-950</td>
 *         <td>x-windows-950 </td>
 *     </tr>
 *     <tr>
 *         <td>MS950_HKSCS</td>
 *         <td></td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacArabic</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacCentralEurope</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacCroatian</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacCyrillic</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacDingbat</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacGreek</td>
 *         <td>MacGreek</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacHebrew</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacIceland</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacRoman</td>
 *         <td>MacRoman</td>
 *         <td>Macintosh MAC csMacintosh </td>
 *     </tr>
 *     <tr>
 *         <td>MacRomania</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacSymbol</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacThai</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacTurkish</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>MacUkraine</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>SJIS</td>
 *         <td>Shift_JIS</td>
 *         <td>MS_Kanji csShiftJIS shift-jis x-sjis pck </td>
 *     </tr>
 *     <tr>
 *         <td>TIS620</td>
 *         <td>TIS-620</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>UTF-16</td>
 *         <td>UTF-16</td>
 *         <td>UTF_16 </td>
 *     </tr>
 *     <tr>
 *         <td>UTF8</td>
 *         <td>UTF-8</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>UnicodeBig</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>UnicodeBigUnmarked</td>
 *         <td>UTF-16BE</td>
 *         <td>X-UTF-16BE UTF_16BE ISO-10646-UCS-2 </td>
 *     </tr>
 *     <tr>
 *         <td>UnicodeLittle</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td>UnicodeLittleUnmarked</td>
 *         <td>UTF-16LE</td>
 *         <td>UTF_16LE X-UTF-16LE </td>
 *     </tr>
 *     <tr>
 *         <td>x-Johab</td>
 *         <td>johab</td>
 *         <td>johab cp1361 ms1361 ksc5601-1992 ksc5601_1992 </td>
 *     </tr>
 *     <tr>
 *         <td>x-iso-8859-11</td>
 *         <td>?</td>
 *         <td></td>
 *     </tr>
 * </table>
 * 
 * 
 * @version $Id: CharsetUtil.java,v 1.1 2004/10/25 07:26:46 ntherning Exp $
 */
public class CharsetUtil {
    private static Log log = LogFactory.getLog(CharsetUtil.class);
    
    private static class Charset implements Comparable {
        private String canonical = null;
        private String mime = null;
        private String[] aliases = null;
        
        private Charset(String canonical, String mime, String[] aliases) {
            this.canonical = canonical;
            this.mime = mime;
            this.aliases = aliases;
        }

        public int compareTo(Object o) {
            Charset c = (Charset) o;
            return this.canonical.compareTo(c.canonical);
        }
    }
    
    private static Charset[] JAVA_CHARSETS = {
        new Charset("ISO8859_1", "ISO-8859-1", 
                    new String[] {"ISO_8859-1:1987", "iso-ir-100", "ISO_8859-1", 
                                  "latin1", "l1", "IBM819", "CP819", 
                                  "csISOLatin1", "8859_1", "819", "IBM-819", 
                                  "ISO8859-1", "ISO_8859_1"}),
        new Charset("ISO8859_2", "ISO-8859-2", 
                    new String[] {"ISO_8859-2:1987", "iso-ir-101", "ISO_8859-2",  
                                  "latin2", "l2", "csISOLatin2", "8859_2", 
                                  "iso8859_2"}),
        new Charset("ISO8859_3", "ISO-8859-3", new String[] {"ISO_8859-3:1988", "iso-ir-109", "ISO_8859-3", "latin3", "l3", "csISOLatin3", "8859_3"}),
        new Charset("ISO8859_4", "ISO-8859-4", 
                    new String[] {"ISO_8859-4:1988", "iso-ir-110", "ISO_8859-4",
                                  "latin4", "l4", "csISOLatin4", "8859_4"}),
        new Charset("ISO8859_5", "ISO-8859-5", 
                    new String[] {"ISO_8859-5:1988", "iso-ir-144", "ISO_8859-5", 
                                  "cyrillic", "csISOLatinCyrillic", "8859_5"}),
        new Charset("ISO8859_6", "ISO-8859-6", new String[] {"ISO_8859-6:1987", "iso-ir-127", "ISO_8859-6", "ECMA-114", "ASMO-708", "arabic", "csISOLatinArabic", "8859_6"}),
        new Charset("ISO8859_7", "ISO-8859-7", 
                    new String[] {"ISO_8859-7:1987", "iso-ir-126", "ISO_8859-7", 
                                  "ELOT_928", "ECMA-118", "greek", "greek8", 
                                  "csISOLatinGreek", "8859_7", "sun_eu_greek"}),
        new Charset("ISO8859_8", "ISO-8859-8", new String[] {"ISO_8859-8:1988", "iso-ir-138", "ISO_8859-8", "hebrew", "csISOLatinHebrew", "8859_8"}),
        new Charset("ISO8859_9", "ISO-8859-9", 
                    new String[] {"ISO_8859-9:1989", "iso-ir-148", "ISO_8859-9",  
                                  "latin5", "l5", "csISOLatin5", "8859_9"}),

        new Charset("ISO8859_13", "ISO-8859-13", new String[] {}),
        new Charset("ISO8859_15", "ISO-8859-15", 
                    new String[] {"ISO_8859-15", "Latin-9", "8859_15", 
                                  "csISOlatin9", "IBM923", "cp923", "923", "L9",
                                  "IBM-923", "ISO8859-15", "LATIN9", "LATIN0", 
                                  "csISOlatin0", "ISO8859_15_FDIS"}),
        new Charset("KOI8_R", "KOI8-R", new String[] {"csKOI8R", "koi8"}),
        new Charset("ASCII", "US-ASCII", 
                    new String[] {"ANSI_X3.4-1968", "iso-ir-6", 
                                  "ANSI_X3.4-1986", "ISO_646.irv:1991", 
                                  "ISO646-US", "us", "IBM367", "cp367", 
                                  "csASCII", "ascii7", "646", "iso_646.irv:1983"}),
        new Charset("UTF8", "UTF-8", new String[] {}),
        new Charset("UTF-16", "UTF-16", new String[] {"UTF_16"}),
        new Charset("UnicodeBigUnmarked", "UTF-16BE", new String[] {"X-UTF-16BE", "UTF_16BE", "ISO-10646-UCS-2"}),
        new Charset("UnicodeLittleUnmarked", "UTF-16LE", new String[] {"UTF_16LE", "X-UTF-16LE"}),
        new Charset("Big5", "Big5", new String[] {"csBig5", "CN-Big5", "BIG-FIVE", "BIGFIVE"}),
        new Charset("Big5_HKSCS", "Big5-HKSCS", new String[] {"big5hkscs"}),
        new Charset("EUC_JP", "EUC-JP", 
                    new String[] {"csEUCPkdFmtJapanese", 
                              "Extended_UNIX_Code_Packed_Format_for_Japanese",
                              "eucjis", "x-eucjp", "eucjp", "x-euc-jp"}),
        new Charset("EUC_KR", "EUC-KR", 
                    new String[] {"csEUCKR", "ksc5601", "5601", "ksc5601_1987", 
                                  "ksc_5601", "ksc5601-1987", "ks_c_5601-1987", 
                                  "euckr"}),
        new Charset("GB18030", "GB18030", new String[] {"gb18030-2000"}),
        new Charset("EUC_CN", "GB2312", new String[] {"x-EUC-CN", "csGB2312", "euccn", "euc-cn", "gb2312-80", "gb2312-1980", "CN-GB", "CN-GB-ISOIR165"}),
        new Charset("GBK", "windows-936", new String[] {"CP936", "MS936", "ms_936", "x-mswin-936", "x-gbk"}),

        new Charset("Cp037", "IBM037", new String[] {"ebcdic-cp-us", "ebcdic-cp-ca", "ebcdic-cp-wt", "ebcdic-cp-nl", "csIBM037"}),
        new Charset("Cp273", "IBM273", new String[] {"csIBM273"}),
        new Charset("Cp277", "IBM277", new String[] {"EBCDIC-CP-DK", "EBCDIC-CP-NO", "csIBM277"}),
        new Charset("Cp278", "IBM278", new String[] {"CP278", "ebcdic-cp-fi", "ebcdic-cp-se", "csIBM278"}),
        new Charset("Cp280", "IBM280", new String[] {"ebcdic-cp-it", "csIBM280"}),
        new Charset("Cp284", "IBM284", new String[] {"ebcdic-cp-es", "csIBM284"}),
        new Charset("Cp285", "IBM285", new String[] {"ebcdic-cp-gb", "csIBM285"}),
        new Charset("Cp297", "IBM297", new String[] {"ebcdic-cp-fr", "csIBM297"}),
        new Charset("Cp420", "IBM420", new String[] {"ebcdic-cp-ar1", "csIBM420"}),
        new Charset("Cp424", "IBM424", new String[] {"ebcdic-cp-he", "csIBM424"}),
        new Charset("Cp437", "IBM437", new String[] {"437", "csPC8CodePage437"}),
        new Charset("Cp500", "IBM500", new String[] {"ebcdic-cp-be", "ebcdic-cp-ch", "csIBM500"}),
        new Charset("Cp775", "IBM775", new String[] {"csPC775Baltic"}),
        new Charset("Cp838", "IBM-Thai", new String[] {}),
        new Charset("Cp850", "IBM850", new String[] {"850", "csPC850Multilingual"}),
        new Charset("Cp852", "IBM852", new String[] {"852", "csPCp852"}),
        new Charset("Cp855", "IBM855", new String[] {"855", "csIBM855"}),
        new Charset("Cp857", "IBM857", new String[] {"857", "csIBM857"}),
        new Charset("Cp858", "IBM00858", 
                new String[] {"CCSID00858", "CP00858", 
                              "PC-Multilingual-850+euro"}),
        new Charset("Cp860", "IBM860", new String[] {"860", "csIBM860"}),
        new Charset("Cp861", "IBM861", new String[] {"861", "cp-is", "csIBM861"}),
        new Charset("Cp862", "IBM862", new String[] {"862", "csPC862LatinHebrew"}),
        new Charset("Cp863", "IBM863", new String[] {"863", "csIBM863"}),
        new Charset("Cp864", "IBM864", new String[] {"cp864", "csIBM864"}),
        new Charset("Cp865", "IBM865", new String[] {"865", "csIBM865"}),
        new Charset("Cp866", "IBM866", new String[] {"866", "csIBM866"}),
        new Charset("Cp868", "IBM868", new String[] {"cp-ar", "csIBM868"}),
        new Charset("Cp869", "IBM869", new String[] {"cp-gr", "csIBM869"}),
        new Charset("Cp870", "IBM870", new String[] {"ebcdic-cp-roece", "ebcdic-cp-yu", "csIBM870"}),
        new Charset("Cp871", "IBM871", new String[] {"ebcdic-cp-is", "csIBM871"}),
        new Charset("Cp918", "IBM918", new String[] {"ebcdic-cp-ar2", "csIBM918"}),
        new Charset("Cp1026", "IBM1026", new String[] {"csIBM1026"}),
        new Charset("Cp1047", "IBM1047", new String[] {"IBM-1047"}),
        new Charset("Cp1140", "IBM01140", 
                    new String[] {"CCSID01140", "CP01140", 
                                  "ebcdic-us-37+euro"}),
        new Charset("Cp1141", "IBM01141", 
                    new String[] {"CCSID01141", "CP01141", 
                                  "ebcdic-de-273+euro"}),
        new Charset("Cp1142", "IBM01142", new String[] {"CCSID01142", "CP01142", "ebcdic-dk-277+euro", "ebcdic-no-277+euro"}),
        new Charset("Cp1143", "IBM01143", new String[] {"CCSID01143", "CP01143", "ebcdic-fi-278+euro", "ebcdic-se-278+euro"}),
        new Charset("Cp1144", "IBM01144", new String[] {"CCSID01144", "CP01144", "ebcdic-it-280+euro"}),
        new Charset("Cp1145", "IBM01145", new String[] {"CCSID01145", "CP01145", "ebcdic-es-284+euro"}),
        new Charset("Cp1146", "IBM01146", new String[] {"CCSID01146", "CP01146", "ebcdic-gb-285+euro"}),
        new Charset("Cp1147", "IBM01147", new String[] {"CCSID01147", "CP01147", "ebcdic-fr-297+euro"}),
        new Charset("Cp1148", "IBM01148", new String[] {"CCSID01148", "CP01148", "ebcdic-international-500+euro"}),
        new Charset("Cp1149", "IBM01149", new String[] {"CCSID01149", "CP01149", "ebcdic-is-871+euro"}),
        new Charset("Cp1250", "windows-1250", new String[] {}),
        new Charset("Cp1251", "windows-1251", new String[] {}),
        new Charset("Cp1252", "windows-1252", new String[] {}),
        new Charset("Cp1253", "windows-1253", new String[] {}),
        new Charset("Cp1254", "windows-1254", new String[] {}),
        new Charset("Cp1255", "windows-1255", new String[] {}),
        new Charset("Cp1256", "windows-1256", new String[] {}),
        new Charset("Cp1257", "windows-1257", new String[] {}),
        new Charset("Cp1258", "windows-1258", new String[] {}),
        new Charset("ISO2022CN", "ISO-2022-CN", new String[] {}),
        new Charset("ISO2022JP", "ISO-2022-JP", new String[] {"csISO2022JP", "JIS", "jis_encoding", "csjisencoding"}),
        new Charset("ISO2022KR", "ISO-2022-KR", new String[] {"csISO2022KR"}),
        new Charset("JIS_X0201", "JIS_X0201", new String[] {"X0201", "JIS0201", "csHalfWidthKatakana"}),
        new Charset("JIS_X0212-1990", "JIS_X0212-1990", new String[] {"iso-ir-159", "x0212", "JIS0212", "csISO159JISX02121990"}),
        new Charset("JIS_C6626-1983", "JIS_C6626-1983", new String[] {"x-JIS0208", "JIS0208", "csISO87JISX0208", "x0208", "JIS_X0208-1983", "iso-ir-87"}),
        new Charset("SJIS", "Shift_JIS", new String[] {"MS_Kanji", "csShiftJIS", "shift-jis", "x-sjis", "pck"}),
        new Charset("TIS620", "TIS-620", new String[] {}),
        new Charset("MS932", "Windows-31J", new String[] {"windows-932", "csWindows31J", "x-ms-cp932"}),
        new Charset("EUC_TW", "EUC-TW", new String[] {"x-EUC-TW", "cns11643", "euctw"}),
        new Charset("x-Johab", "johab", new String[] {"johab", "cp1361", "ms1361", "ksc5601-1992", "ksc5601_1992"}),
        new Charset("MS950_HKSCS", "", new String[] {}),
        new Charset("MS874", "windows-874", new String[] {"cp874"}),
        new Charset("MS949", "windows-949", new String[] {"windows949", "ms_949", "x-windows-949"}),
        new Charset("MS950", "windows-950", new String[] {"x-windows-950"}),

        new Charset("Cp737", null, new String[] {}),
        new Charset("Cp856", null, new String[] {}),
        new Charset("Cp875", null, new String[] {}),
        new Charset("Cp921", null, new String[] {}),
        new Charset("Cp922", null, new String[] {}),
        new Charset("Cp930", null, new String[] {}),
        new Charset("Cp933", null, new String[] {}),
        new Charset("Cp935", null, new String[] {}),
        new Charset("Cp937", null, new String[] {}),
        new Charset("Cp939", null, new String[] {}),
        new Charset("Cp942", null, new String[] {}),
        new Charset("Cp942C", null, new String[] {}),
        new Charset("Cp943", null, new String[] {}),
        new Charset("Cp943C", null, new String[] {}),
        new Charset("Cp948", null, new String[] {}),
        new Charset("Cp949", null, new String[] {}),
        new Charset("Cp949C", null, new String[] {}),
        new Charset("Cp950", null, new String[] {}),
        new Charset("Cp964", null, new String[] {}),
        new Charset("Cp970", null, new String[] {}),
        new Charset("Cp1006", null, new String[] {}),
        new Charset("Cp1025", null, new String[] {}),    
        new Charset("Cp1046", null, new String[] {}),
        new Charset("Cp1097", null, new String[] {}),
        new Charset("Cp1098", null, new String[] {}),
        new Charset("Cp1112", null, new String[] {}),
        new Charset("Cp1122", null, new String[] {}),
        new Charset("Cp1123", null, new String[] {}),
        new Charset("Cp1124", null, new String[] {}),
        new Charset("Cp1381", null, new String[] {}),
        new Charset("Cp1383", null, new String[] {}),
        new Charset("Cp33722", null, new String[] {}),
        new Charset("Big5_Solaris", null, new String[] {}),
        new Charset("EUC_JP_LINUX", null, new String[] {}),
        new Charset("EUC_JP_Solaris", null, new String[] {}),
        new Charset("ISCII91", null, new String[] {"x-ISCII91", "iscii"}),
        new Charset("ISO2022_CN_CNS", null, new String[] {}),
        new Charset("ISO2022_CN_GB", null, new String[] {}),
        new Charset("x-iso-8859-11", null, new String[] {}),
        new Charset("JISAutoDetect", null, new String[] {}),
        new Charset("MacArabic", null, new String[] {}),
        new Charset("MacCentralEurope", null, new String[] {}),
        new Charset("MacCroatian", null, new String[] {}),
        new Charset("MacCyrillic", null, new String[] {}),
        new Charset("MacDingbat", null, new String[] {}),
        new Charset("MacGreek", "MacGreek", new String[] {}),
        new Charset("MacHebrew", null, new String[] {}),
        new Charset("MacIceland", null, new String[] {}),
        new Charset("MacRoman", "MacRoman", new String[] {"Macintosh", "MAC", "csMacintosh"}),
        new Charset("MacRomania", null, new String[] {}),
        new Charset("MacSymbol", null, new String[] {}),
        new Charset("MacThai", null, new String[] {}),
        new Charset("MacTurkish", null, new String[] {}),
        new Charset("MacUkraine", null, new String[] {}),
        new Charset("UnicodeBig", null, new String[] {}),
        new Charset("UnicodeLittle", null, new String[] {})
    };

    /**
     * Contains the canonical names of character sets which can be used to 
     * decode bytes into Java chars.
     */
    private static TreeSet decodingSupported = null;
    
    /**
     * Contains the canonical names of character sets which can be used to 
     * encode Java chars into bytes.
     */
    private static TreeSet encodingSupported = null;
    
    /**
     * Maps character set names to Charset objects. All possible names of
     * a charset will be mapped to the Charset.
     */
    private static HashMap charsetMap = null;
    
    static {
        decodingSupported = new TreeSet();
        encodingSupported = new TreeSet();
        byte[] dummy = new byte[] {'d', 'u', 'm', 'm', 'y'};
        for (int i = 0; i < JAVA_CHARSETS.length; i++) {
            try {
                String s = new String(dummy, JAVA_CHARSETS[i].canonical);
                decodingSupported.add(JAVA_CHARSETS[i].canonical.toLowerCase());
            } catch (UnsupportedOperationException e) {
            } catch (UnsupportedEncodingException e) {
            }
            try {
                "dummy".getBytes(JAVA_CHARSETS[i].canonical);
                encodingSupported.add(JAVA_CHARSETS[i].canonical.toLowerCase());
            } catch (UnsupportedOperationException e) {
            } catch (UnsupportedEncodingException e) {
            }
        }
        
        charsetMap = new HashMap();
        for (int i = 0; i < JAVA_CHARSETS.length; i++) {
            Charset c = JAVA_CHARSETS[i];
            charsetMap.put(c.canonical.toLowerCase(), c);
            if (c.mime != null) {
                charsetMap.put(c.mime.toLowerCase(), c);
            }
            if (c.aliases != null) {
                for (int j = 0; j < c.aliases.length; j++) {
                    charsetMap.put(c.aliases[j].toLowerCase(), c);
                }
            }
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Character sets which support decoding: " 
                        + decodingSupported);
            log.debug("Character sets which support encoding: " 
                        + encodingSupported);
        }
    }
    
    /**
     * ANDROID:  THE FOLLOWING SET OF STATIC STRINGS ARE COPIED FROM A NEWER VERSION OF MIME4J
     */
    
    /** carriage return - line feed sequence */
    public static final String CRLF = "\r\n";

    /** US-ASCII CR, carriage return (13) */
    public static final int CR = '\r';

    /** US-ASCII LF, line feed (10) */
    public static final int LF = '\n';

    /** US-ASCII SP, space (32) */
    public static final int SP = ' ';

    /** US-ASCII HT, horizontal-tab (9)*/
    public static final int HT = '\t';
    
    public static final java.nio.charset.Charset US_ASCII = java.nio.charset.Charset
            .forName("US-ASCII");

    public static final java.nio.charset.Charset ISO_8859_1 = java.nio.charset.Charset
            .forName("ISO-8859-1");

    public static final java.nio.charset.Charset UTF_8 = java.nio.charset.Charset
            .forName("UTF-8");

    /**
     * Returns <code>true</code> if the specified character is a whitespace
     * character (CR, LF, SP or HT).
     * 
     * ANDROID:  COPIED FROM A NEWER VERSION OF MIME4J
     * 
     * @param ch
     *            character to test.
     * @return <code>true</code> if the specified character is a whitespace
     *         character, <code>false</code> otherwise.
     */
    public static boolean isWhitespace(char ch) {
        return ch == SP || ch == HT || ch == CR || ch == LF;
    }

    /**
     * Returns <code>true</code> if the specified string consists entirely of
     * whitespace characters.
     * 
     * ANDROID:  COPIED FROM A NEWER VERSION OF MIME4J
     * 
     * @param s
     *            string to test.
     * @return <code>true</code> if the specified string consists entirely of
     *         whitespace characters, <code>false</code> otherwise.
     */
    public static boolean isWhitespace(final String s) {
        if (s == null) {
            throw new IllegalArgumentException("String may not be null");
        }
        final int len = s.length();
        for (int i = 0; i < len; i++) {
            if (!isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if the VM supports encoding (chars to bytes) the 
     * specified character set. NOTE: the given character set name may 
     * not be known to the VM even if this method returns <code>true</code>.
     * Use {@link #toJavaCharset(String)} to get the canonical Java character
     * set name.
     * 
     * @param charsetName the characters set name.
     * @return <code>true</code> if encoding is supported, <code>false</code>
     *         otherwise.
     */
    public static boolean isEncodingSupported(String charsetName) {
        return encodingSupported.contains(charsetName.toLowerCase());
    }
    
    /**
     * Determines if the VM supports decoding (bytes to chars) the 
     * specified character set. NOTE: the given character set name may 
     * not be known to the VM even if this method returns <code>true</code>.
     * Use {@link #toJavaCharset(String)} to get the canonical Java character
     * set name.
     * 
     * @param charsetName the characters set name.
     * @return <code>true</code> if decoding is supported, <code>false</code>
     *         otherwise.
     */
    public static boolean isDecodingSupported(String charsetName) {
        return decodingSupported.contains(charsetName.toLowerCase());
    }
    
    /**
     * Gets the preferred MIME character set name for the specified
     * character set or <code>null</code> if not known.
     * 
     * @param charsetName the character set name to look for.
     * @return the MIME preferred name or <code>null</code> if not known.
     */
    public static String toMimeCharset(String charsetName) {
        Charset c = (Charset) charsetMap.get(charsetName.toLowerCase());
        if (c != null) {
            return c.mime;
        }
        return null;
    }
    
    /**
     * Gets the canonical Java character set name for the specified
     * character set or <code>null</code> if not known. This should be
     * called before doing any conversions using the Java API. NOTE:
     * you must use {@link #isEncodingSupported(String)} or
     * {@link #isDecodingSupported(String)} to make sure the returned
     * Java character set is supported by the current VM.
     * 
     * @param charsetName the character set name to look for.
     * @return the canonical Java name or <code>null</code> if not known.
     */
    public static String toJavaCharset(String charsetName) {
        Charset c = (Charset) charsetMap.get(charsetName.toLowerCase());
        if (c != null) {
            return c.canonical;
        }
        return null;
    }

    public static java.nio.charset.Charset getCharset(String charsetName) {
        String defaultCharset = "ISO-8859-1";
        
        // Use the default chareset if given charset is null
        if(charsetName == null) charsetName = defaultCharset;
            
        try {
            return java.nio.charset.Charset.forName(charsetName);
        } catch (IllegalCharsetNameException e) {
            log.info("Illegal charset " + charsetName + ", fallback to " + defaultCharset + ": " + e);
            // Use default charset on exception 
            return java.nio.charset.Charset.forName(defaultCharset);
        } catch (UnsupportedCharsetException ex) {
            log.info("Unsupported charset " + charsetName + ", fallback to " + defaultCharset + ": " + ex);
            // Use default charset on exception
            return java.nio.charset.Charset.forName(defaultCharset);
        }
        
    }
    /*
     * Uncomment the code below and run the main method to regenerate the
     * Javadoc table above when the known charsets change. 
     */
    
    /*
    private static String dumpHtmlTable() {
        LinkedList l = new LinkedList(Arrays.asList(JAVA_CHARSETS));
        Collections.sort(l);
        StringBuffer sb = new StringBuffer();
        sb.append(" * <table>\n");
        sb.append(" *     <tr>\n");
        sb.append(" *         <td>Canonical (Java) name</td>\n");
        sb.append(" *         <td>MIME preferred</td>\n");
        sb.append(" *         <td>Aliases</td>\n");
        sb.append(" *     </tr>\n");

        for (Iterator it = l.iterator(); it.hasNext();) {
            Charset c = (Charset) it.next();
            sb.append(" *     <tr>\n");
            sb.append(" *         <td>" + c.canonical + "</td>\n");
            sb.append(" *         <td>" + (c.mime == null ? "?" : c.mime)+ "</td>\n");
            sb.append(" *         <td>");
            for (int i = 0; c.aliases != null && i < c.aliases.length; i++) {
                sb.append(c.aliases[i] + " ");
            }
            sb.append("</td>\n");
            sb.append(" *     </tr>\n");
        }
        sb.append(" * </table>\n");
        return sb.toString();
    }
    
    public static void main(String[] args) {
        System.out.println(dumpHtmlTable());
    }*/
}
