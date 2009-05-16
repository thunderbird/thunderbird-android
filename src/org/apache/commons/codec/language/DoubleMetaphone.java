/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.apache.commons.codec.language;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;

/**
 * Encodes a string into a double metaphone value.
 * This Implementation is based on the algorithm by <CITE>Lawrence Philips</CITE>.
 * <ul>
 * <li>Original Article: <a 
 * href="http://www.cuj.com/documents/s=8038/cuj0006philips/">
 * http://www.cuj.com/documents/s=8038/cuj0006philips/</a></li>
 * <li>Original Source Code: <a href="ftp://ftp.cuj.com/pub/2000/1806/philips.zip">
 * ftp://ftp.cuj.com/pub/2000/1806/philips.zip</a></li>
 * </ul>
 * 
 * @author Apache Software Foundation
 * @version $Id: DoubleMetaphone.java,v 1.24 2004/06/05 18:32:04 ggregory Exp $
 */
public class DoubleMetaphone implements StringEncoder {

    /**
     * "Vowels" to test for
     */
    private static final String VOWELS = "AEIOUY";

    /**
     * Prefixes when present which are not pronounced
     */
    private static final String[] SILENT_START = 
    { "GN", "KN", "PN", "WR", "PS" };
    private static final String[] L_R_N_M_B_H_F_V_W_SPACE = 
    { "L", "R", "N", "M", "B", "H", "F", "V", "W", " " };
    private static final String[] ES_EP_EB_EL_EY_IB_IL_IN_IE_EI_ER = 
    { "ES", "EP", "EB", "EL", "EY", "IB", "IL", "IN", "IE", "EI", "ER" };
    private static final String[] L_T_K_S_N_M_B_Z = 
    { "L", "T", "K", "S", "N", "M", "B", "Z" };

    /**
     * Maximum length of an encoding, default is 4
     */
    protected int maxCodeLen = 4;

    /**
     * Creates an instance of this DoubleMetaphone encoder
     */
    public DoubleMetaphone() {
        super();
    }
    
    /**
     * Encode a value with Double Metaphone
     *
     * @param value String to encode
     * @return an encoded string
     */
    public String doubleMetaphone(String value) {
        return doubleMetaphone(value, false);
    }
    
    /**
     * Encode a value with Double Metaphone, optionally using the alternate
     * encoding.
     *
     * @param value String to encode
     * @param alternate use alternate encode
     * @return an encoded string
     */
    public String doubleMetaphone(String value, boolean alternate) {
        value = cleanInput(value);
        if (value == null) {
            return null;
        }
        
        boolean slavoGermanic = isSlavoGermanic(value);
        int index = isSilentStart(value) ? 1 : 0;
        
        DoubleMetaphoneResult result = new DoubleMetaphoneResult(this.getMaxCodeLen());
        
        while (!result.isComplete() && index <= value.length() - 1) {
            switch (value.charAt(index)) {
            case 'A':
            case 'E':
            case 'I':
            case 'O':
            case 'U':
            case 'Y':
                index = handleAEIOUY(value, result, index);
                break;
            case 'B':
                result.append('P');
                index = charAt(value, index + 1) == 'B' ? index + 2 : index + 1;
                break;
            case '\u00C7':
                // A C with a Cedilla
                result.append('S');
                index++;
                break; 
            case 'C':
                index = handleC(value, result, index);
                break;
            case 'D':
                index = handleD(value, result, index);
                break;
            case 'F':
                result.append('F');
                index = charAt(value, index + 1) == 'F' ? index + 2 : index + 1;
                break;
            case 'G':
                index = handleG(value, result, index, slavoGermanic);
                break;
            case 'H':
                index = handleH(value, result, index);
                break;
            case 'J':
                index = handleJ(value, result, index, slavoGermanic);
                break;
            case 'K':
                result.append('K');
                index = charAt(value, index + 1) == 'K' ? index + 2 : index + 1;
                break;
            case 'L':
                index = handleL(value, result, index);
                break;
            case 'M':
                result.append('M');
                index = conditionM0(value, index) ? index + 2 : index + 1;
                break;
            case 'N':
                result.append('N');
                index = charAt(value, index + 1) == 'N' ? index + 2 : index + 1;
                break;
            case '\u00D1':
                // N with a tilde (spanish ene)
                result.append('N');
                index++;
                break;
            case 'P':
                index = handleP(value, result, index);
                break;
            case 'Q':
                result.append('K');
                index = charAt(value, index + 1) == 'Q' ? index + 2 : index + 1;
                break;
            case 'R':
                index = handleR(value, result, index, slavoGermanic);
                break;
            case 'S':
                index = handleS(value, result, index, slavoGermanic);
                break;
            case 'T':
                index = handleT(value, result, index);
                break;
            case 'V':
                result.append('F');
                index = charAt(value, index + 1) == 'V' ? index + 2 : index + 1;
                break;
            case 'W':
                index = handleW(value, result, index);
                break;
            case 'X':
                index = handleX(value, result, index);
                break;
            case 'Z':
                index = handleZ(value, result, index, slavoGermanic);
                break;
            default:
                index++;
                break;
            }
        }

        return alternate ? result.getAlternate() : result.getPrimary();
    }
    
    /**
     * Encode the value using DoubleMetaphone.  It will only work if 
     * <code>obj</code> is a <code>String</code> (like <code>Metaphone</code>).
     *
     * @param obj Object to encode (should be of type String)
     * @return An encoded Object (will be of type String)
     * @throws EncoderException encode parameter is not of type String
     */
    public Object encode(Object obj) throws EncoderException {
        if (!(obj instanceof String)) {
            throw new EncoderException("DoubleMetaphone encode parameter is not of type String"); 
        } 
        return doubleMetaphone((String) obj);
    }

    /**
     * Encode the value using DoubleMetaphone.
     *
     * @param value String to encode
     * @return An encoded String
     */
    public String encode(String value) {
        return doubleMetaphone(value);   
    }

    /**
     * Check if the Double Metaphone values of two <code>String</code> values
     * are equal.
     * 
     * @param value1 The left-hand side of the encoded {@link String#equals(Object)}.
     * @param value2 The right-hand side of the encoded {@link String#equals(Object)}.
     * @return <code>true</code> if the encoded <code>String</code>s are equal;
     *          <code>false</code> otherwise.
     * @see #isDoubleMetaphoneEqual(String,String,boolean)
     */
    public boolean isDoubleMetaphoneEqual(String value1, String value2) {
        return isDoubleMetaphoneEqual(value1, value2, false);
    }
    
    /**
     * Check if the Double Metaphone values of two <code>String</code> values
     * are equal, optionally using the alternate value.
     * 
     * @param value1 The left-hand side of the encoded {@link String#equals(Object)}.
     * @param value2 The right-hand side of the encoded {@link String#equals(Object)}.
     * @param alternate use the alternate value if <code>true</code>.
     * @return <code>true</code> if the encoded <code>String</code>s are equal;
     *          <code>false</code> otherwise.
     */
    public boolean isDoubleMetaphoneEqual(String value1, 
                                          String value2, 
                                          boolean alternate) {
        return doubleMetaphone(value1, alternate).equals(doubleMetaphone
                                                         (value2, alternate));
    }
    
    /**
     * Returns the maxCodeLen.
     * @return int
     */
    public int getMaxCodeLen() {
        return this.maxCodeLen;
    }

    /**
     * Sets the maxCodeLen.
     * @param maxCodeLen The maxCodeLen to set
     */
    public void setMaxCodeLen(int maxCodeLen) {
        this.maxCodeLen = maxCodeLen;
    }

    //-- BEGIN HANDLERS --//

    /**
     * Handles 'A', 'E', 'I', 'O', 'U', and 'Y' cases
     */
    private int handleAEIOUY(String value, DoubleMetaphoneResult result, int 
                             index) {
        if (index == 0) {
            result.append('A');
        }
        return index + 1;
    }
    
    /**
     * Handles 'C' cases
     */
    private int handleC(String value, 
                        DoubleMetaphoneResult result, 
                        int index) {
        if (conditionC0(value, index)) {  // very confusing, moved out
            result.append('K');
            index += 2;
        } else if (index == 0 && contains(value, index, 6, "CAESAR")) {
            result.append('S');
            index += 2;
        } else if (contains(value, index, 2, "CH")) {
            index = handleCH(value, result, index);
        } else if (contains(value, index, 2, "CZ") && 
                   !contains(value, index - 2, 4, "WICZ")) {
            //-- "Czerny" --//
            result.append('S', 'X');
            index += 2;
        } else if (contains(value, index + 1, 3, "CIA")) {
            //-- "focaccia" --//
            result.append('X');
            index += 3;
        } else if (contains(value, index, 2, "CC") && 
                   !(index == 1 && charAt(value, 0) == 'M')) {
            //-- double "cc" but not "McClelland" --//
            return handleCC(value, result, index);
        } else if (contains(value, index, 2, "CK", "CG", "CQ")) {
            result.append('K');
            index += 2;
        } else if (contains(value, index, 2, "CI", "CE", "CY")) {
            //-- Italian vs. English --//
            if (contains(value, index, 3, "CIO", "CIE", "CIA")) {
                result.append('S', 'X');
            } else {
                result.append('S');
            }
            index += 2;
        } else {
            result.append('K');
            if (contains(value, index + 1, 2, " C", " Q", " G")) { 
                //-- Mac Caffrey, Mac Gregor --//
                index += 3;
            } else if (contains(value, index + 1, 1, "C", "K", "Q") && 
                       !contains(value, index + 1, 2, "CE", "CI")) {
                index += 2;
            } else {
                index++;
            }
        }
        
        return index;
    }

    /**
     * Handles 'CC' cases
     */
    private int handleCC(String value, 
                         DoubleMetaphoneResult result, 
                         int index) {
        if (contains(value, index + 2, 1, "I", "E", "H") && 
            !contains(value, index + 2, 2, "HU")) {
            //-- "bellocchio" but not "bacchus" --//
            if ((index == 1 && charAt(value, index - 1) == 'A') || 
                contains(value, index - 1, 5, "UCCEE", "UCCES")) {
                //-- "accident", "accede", "succeed" --//
                result.append("KS");
            } else {
                //-- "bacci", "bertucci", other Italian --//
                result.append('X');
            }
            index += 3;
        } else {    // Pierce's rule
            result.append('K');
            index += 2;
        }
        
        return index;
    }
    
    /**
     * Handles 'CH' cases
     */
    private int handleCH(String value, 
                         DoubleMetaphoneResult result, 
                         int index) {
        if (index > 0 && contains(value, index, 4, "CHAE")) {   // Michael
            result.append('K', 'X');
            return index + 2;
        } else if (conditionCH0(value, index)) {
            //-- Greek roots ("chemistry", "chorus", etc.) --//
            result.append('K');
            return index + 2;
        } else if (conditionCH1(value, index)) {
            //-- Germanic, Greek, or otherwise 'ch' for 'kh' sound --//
            result.append('K');
            return index + 2;
        } else {
            if (index > 0) {
                if (contains(value, 0, 2, "MC")) {
                    result.append('K');
                } else {
                    result.append('X', 'K');
                }
            } else {
                result.append('X');
            }
            return index + 2;
        }
    }

    /**
     * Handles 'D' cases
     */
    private int handleD(String value, 
                        DoubleMetaphoneResult result, 
                        int index) {
        if (contains(value, index, 2, "DG")) {
            //-- "Edge" --//
            if (contains(value, index + 2, 1, "I", "E", "Y")) {
                result.append('J');
                index += 3;
                //-- "Edgar" --//
            } else {
                result.append("TK");
                index += 2;
            }
        } else if (contains(value, index, 2, "DT", "DD")) {
            result.append('T');
            index += 2;
        } else {
            result.append('T');
            index++;
        }
        return index;
    }

    /**
     * Handles 'G' cases
     */
    private int handleG(String value, 
                        DoubleMetaphoneResult result, 
                        int index, 
                        boolean slavoGermanic) {
        if (charAt(value, index + 1) == 'H') {
            index = handleGH(value, result, index);
        } else if (charAt(value, index + 1) == 'N') {
            if (index == 1 && isVowel(charAt(value, 0)) && !slavoGermanic) {
                result.append("KN", "N");
            } else if (!contains(value, index + 2, 2, "EY") && 
                       charAt(value, index + 1) != 'Y' && !slavoGermanic) {
                result.append("N", "KN");
            } else {
                result.append("KN");
            }
            index = index + 2;
        } else if (contains(value, index + 1, 2, "LI") && !slavoGermanic) {
            result.append("KL", "L");
            index += 2;
        } else if (index == 0 && (charAt(value, index + 1) == 'Y' || contains(value, index + 1, 2, ES_EP_EB_EL_EY_IB_IL_IN_IE_EI_ER))) {
            //-- -ges-, -gep-, -gel-, -gie- at beginning --//
            result.append('K', 'J');
            index += 2;
        } else if ((contains(value, index + 1, 2, "ER") || 
                    charAt(value, index + 1) == 'Y') &&
                   !contains(value, 0, 6, "DANGER", "RANGER", "MANGER") &&
                   !contains(value, index - 1, 1, "E", "I") && 
                   !contains(value, index - 1, 3, "RGY", "OGY")) {
            //-- -ger-, -gy- --//
            result.append('K', 'J');
            index += 2;
        } else if (contains(value, index + 1, 1, "E", "I", "Y") || 
                   contains(value, index - 1, 4, "AGGI", "OGGI")) {
            //-- Italian "biaggi" --//
            if ((contains(value, 0 ,4, "VAN ", "VON ") || contains(value, 0, 3, "SCH")) || contains(value, index + 1, 2, "ET")) {
                //-- obvious germanic --//
                result.append('K');
            } else if (contains(value, index + 1, 4, "IER")) {
                result.append('J');
            } else {
                result.append('J', 'K');
            }
            index += 2;
        } else if (charAt(value, index + 1) == 'G') {
            index += 2;
            result.append('K');
        } else {
            index++;
            result.append('K');
        }
        return index;
    }
    
    /**
     * Handles 'GH' cases
     */
    private int handleGH(String value, 
                         DoubleMetaphoneResult result, 
                         int index) {
        if (index > 0 && !isVowel(charAt(value, index - 1))) {
            result.append('K');
            index += 2;
        } else if (index == 0) {
            if (charAt(value, index + 2) == 'I') {
                result.append('J');
            } else {
                result.append('K');
            }
            index += 2;
        } else if ((index > 1 && contains(value, index - 2, 1, "B", "H", "D")) ||
                   (index > 2 && contains(value, index - 3, 1, "B", "H", "D")) ||
                   (index > 3 && contains(value, index - 4, 1, "B", "H"))) {
            //-- Parker's rule (with some further refinements) - "hugh"
            index += 2;
        } else {
            if (index > 2 && charAt(value, index - 1) == 'U' && 
                contains(value, index - 3, 1, "C", "G", "L", "R", "T")) {
                //-- "laugh", "McLaughlin", "cough", "gough", "rough", "tough"
                result.append('F');
            } else if (index > 0 && charAt(value, index - 1) != 'I') {
                result.append('K');
            }
            index += 2;
        }
        return index;
    }

    /**
     * Handles 'H' cases
     */
    private int handleH(String value, 
                        DoubleMetaphoneResult result, 
                        int index) {
        //-- only keep if first & before vowel or between 2 vowels --//
        if ((index == 0 || isVowel(charAt(value, index - 1))) && 
            isVowel(charAt(value, index + 1))) {
            result.append('H');
            index += 2;
            //-- also takes car of "HH" --//
        } else {
            index++;
        }
        return index;
    }
    
    /**
     * Handles 'J' cases
     */
    private int handleJ(String value, DoubleMetaphoneResult result, int index, 
                        boolean slavoGermanic) {
        if (contains(value, index, 4, "JOSE") || contains(value, 0, 4, "SAN ")) {
                //-- obvious Spanish, "Jose", "San Jacinto" --//
                if ((index == 0 && (charAt(value, index + 4) == ' ') || 
                     value.length() == 4) || contains(value, 0, 4, "SAN ")) {
                    result.append('H');
                } else {
                    result.append('J', 'H');
                }
                index++;
            } else {
                if (index == 0 && !contains(value, index, 4, "JOSE")) {
                    result.append('J', 'A');
                } else if (isVowel(charAt(value, index - 1)) && !slavoGermanic && 
                              (charAt(value, index + 1) == 'A' || charAt(value, index + 1) == 'O')) {
                    result.append('J', 'H');
                } else if (index == value.length() - 1) {
                    result.append('J', ' ');
                } else if (!contains(value, index + 1, 1, L_T_K_S_N_M_B_Z) && !contains(value, index - 1, 1, "S", "K", "L")) {
                    result.append('J');
                }

                if (charAt(value, index + 1) == 'J') {
                    index += 2;
                } else {
                    index++;
                }
            }
        return index;
    }
    
    /**
     * Handles 'L' cases
     */
    private int handleL(String value, 
                        DoubleMetaphoneResult result, 
                        int index) {
        result.append('L');
        if (charAt(value, index + 1) == 'L') {
            if (conditionL0(value, index)) {
                result.appendAlternate(' ');
            }
            index += 2;
        } else {
            index++;
        }
        return index;
    }

    /**
     * Handles 'P' cases
     */
    private int handleP(String value, 
                        DoubleMetaphoneResult result, 
                        int index) {
        if (charAt(value, index + 1) == 'H') {
            result.append('F');
            index += 2;
        } else {
            result.append('P');
            index = contains(value, index + 1, 1, "P", "B") ? index + 2 : index + 1;
        }
        return index;
    }

    /**
     * Handles 'R' cases
     */
    private int handleR(String value, 
                        DoubleMetaphoneResult result, 
                        int index, 
                        boolean slavoGermanic) {
        if (index == value.length() - 1 && !slavoGermanic && 
            contains(value, index - 2, 2, "IE") && 
            !contains(value, index - 4, 2, "ME", "MA")) {
            result.appendAlternate('R');
        } else {
            result.append('R');
        }
        return charAt(value, index + 1) == 'R' ? index + 2 : index + 1;
    }

    /**
     * Handles 'S' cases
     */
    private int handleS(String value, 
                        DoubleMetaphoneResult result, 
                        int index, 
                        boolean slavoGermanic) {
        if (contains(value, index - 1, 3, "ISL", "YSL")) {
            //-- special cases "island", "isle", "carlisle", "carlysle" --//
            index++;
        } else if (index == 0 && contains(value, index, 5, "SUGAR")) {
            //-- special case "sugar-" --//
            result.append('X', 'S');
            index++;
        } else if (contains(value, index, 2, "SH")) {
            if (contains(value, index + 1, 4, 
                         "HEIM", "HOEK", "HOLM", "HOLZ")) {
                //-- germanic --//
                result.append('S');
            } else {
                result.append('X');
            }
            index += 2;
        } else if (contains(value, index, 3, "SIO", "SIA") || contains(value, index, 4, "SIAN")) {
            //-- Italian and Armenian --//
            if (slavoGermanic) {
                result.append('S');
            } else {
                result.append('S', 'X');
            }
            index += 3;
        } else if ((index == 0 && contains(value, index + 1, 1, "M", "N", "L", "W")) || contains(value, index + 1, 1, "Z")) {
            //-- german & anglicisations, e.g. "smith" match "schmidt" //
            // "snider" match "schneider" --//
            //-- also, -sz- in slavic language altho in hungarian it //
            //   is pronounced "s" --//
            result.append('S', 'X');
            index = contains(value, index + 1, 1, "Z") ? index + 2 : index + 1;
        } else if (contains(value, index, 2, "SC")) {
            index = handleSC(value, result, index);
        } else {
            if (index == value.length() - 1 && contains(value, index - 2, 
                                                        2, "AI", "OI")){
                //-- french e.g. "resnais", "artois" --//
                result.appendAlternate('S');
            } else {
                result.append('S');
            }
            index = contains(value, index + 1, 1, "S", "Z") ? index + 2 : index + 1;
        }
        return index;
    }

    /**
     * Handles 'SC' cases
     */
    private int handleSC(String value, 
                         DoubleMetaphoneResult result, 
                         int index) {
        if (charAt(value, index + 2) == 'H') {
            //-- Schlesinger's rule --//
            if (contains(value, index + 3, 
                         2, "OO", "ER", "EN", "UY", "ED", "EM")) {
                //-- Dutch origin, e.g. "school", "schooner" --//
                if (contains(value, index + 3, 2, "ER", "EN")) {
                    //-- "schermerhorn", "schenker" --//
                    result.append("X", "SK");
                } else {
                    result.append("SK");
                }
            } else {
                if (index == 0 && !isVowel(charAt(value, 3)) && charAt(value, 3) != 'W') {
                    result.append('X', 'S');
                } else {
                    result.append('X');
                }
            }
        } else if (contains(value, index + 2, 1, "I", "E", "Y")) {
            result.append('S');
        } else {
            result.append("SK");
        }
        return index + 3;
    }

    /**
     * Handles 'T' cases
     */
    private int handleT(String value, 
                        DoubleMetaphoneResult result, 
                        int index) {
        if (contains(value, index, 4, "TION")) {
            result.append('X');
            index += 3;
        } else if (contains(value, index, 3, "TIA", "TCH")) {
            result.append('X');
            index += 3;
        } else if (contains(value, index, 2, "TH") || contains(value, index, 
                                                               3, "TTH")) {
            if (contains(value, index + 2, 2, "OM", "AM") || 
                //-- special case "thomas", "thames" or germanic --//
                contains(value, 0, 4, "VAN ", "VON ") || 
                contains(value, 0, 3, "SCH")) {
                result.append('T');
            } else {
                result.append('0', 'T');
            }
            index += 2;
        } else {
            result.append('T');
            index = contains(value, index + 1, 1, "T", "D") ? index + 2 : index + 1;
        }
        return index;
    }

    /**
     * Handles 'W' cases
     */
    private int handleW(String value, 
                        DoubleMetaphoneResult result, 
                        int index) {
        if (contains(value, index, 2, "WR")) {
            //-- can also be in middle of word --//
            result.append('R');
            index += 2;
        } else {
            if (index == 0 && (isVowel(charAt(value, index + 1)) || 
                               contains(value, index, 2, "WH"))) {
                if (isVowel(charAt(value, index + 1))) {
                    //-- Wasserman should match Vasserman --//
                    result.append('A', 'F');
                } else {
                    //-- need Uomo to match Womo --//
                    result.append('A');
                }
                index++;
            } else if ((index == value.length() - 1 && isVowel(charAt(value, index - 1))) ||
                       contains(value, index - 1, 
                                5, "EWSKI", "EWSKY", "OWSKI", "OWSKY") ||
                       contains(value, 0, 3, "SCH")) {
                //-- Arnow should match Arnoff --//
                result.appendAlternate('F');
                index++;
            } else if (contains(value, index, 4, "WICZ", "WITZ")) {
                //-- Polish e.g. "filipowicz" --//
                result.append("TS", "FX");
                index += 4;
            } else {
                index++;
            }
        }
        return index;
    }
    
    /**
     * Handles 'X' cases
     */
    private int handleX(String value, 
                        DoubleMetaphoneResult result, 
                        int index) {
        if (index == 0) {
            result.append('S');
            index++;
        } else {
            if (!((index == value.length() - 1) && 
                  (contains(value, index - 3, 3, "IAU", "EAU") || 
                   contains(value, index - 2, 2, "AU", "OU")))) {
                //-- French e.g. breaux --//
                result.append("KS");
            }
            index = contains(value, index + 1, 1, "C", "X") ? index + 2 : index + 1;
        }
        return index;
    }

    /**
     * Handles 'Z' cases
     */
    private int handleZ(String value, DoubleMetaphoneResult result, int index, 
                        boolean slavoGermanic) {
        if (charAt(value, index + 1) == 'H') {
            //-- Chinese pinyin e.g. "zhao" or Angelina "Zhang" --//
            result.append('J');
            index += 2;
        } else {
            if (contains(value, index + 1, 2, "ZO", "ZI", "ZA") || (slavoGermanic && (index > 0 && charAt(value, index - 1) != 'T'))) {
                result.append("S", "TS");
            } else {
                result.append('S');
            }
            index = charAt(value, index + 1) == 'Z' ? index + 2 : index + 1;
        }
        return index;
    }

    //-- BEGIN CONDITIONS --//

    /**
     * Complex condition 0 for 'C'
     */
    private boolean conditionC0(String value, int index) {
		if (contains(value, index, 4, "CHIA")) {
			return true;
		} else if (index <= 1) {
			return false;
		} else if (isVowel(charAt(value, index - 2))) {
			return false;
		} else if (!contains(value, index - 1, 3, "ACH")) {
			return false;
		} else {
			char c = charAt(value, index + 2);
			return (c != 'I' && c != 'E')
					|| contains(value, index - 2, 6, "BACHER", "MACHER");
		}
	}
    
    /**
	 * Complex condition 0 for 'CH'
	 */
    private boolean conditionCH0(String value, int index) {
        if (index != 0) {
            return false;
        } else if (!contains(value, index + 1, 5, "HARAC", "HARIS") && 
                   !contains(value, index + 1, 3, "HOR", "HYM", "HIA", "HEM")) {
            return false;
        } else if (contains(value, 0, 5, "CHORE")) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Complex condition 1 for 'CH'
     */
    private boolean conditionCH1(String value, int index) {
        return ((contains(value, 0, 4, "VAN ", "VON ") || contains(value, 0, 
                                                                   3, "SCH")) ||
                contains(value, index - 2, 6, "ORCHES", "ARCHIT", "ORCHID") ||
                contains(value, index + 2, 1, "T", "S") ||
                ((contains(value, index - 1, 1, "A", "O", "U", "E") || index == 0) &&
                 (contains(value, index + 2, 1, L_R_N_M_B_H_F_V_W_SPACE) || index + 1 == value.length() - 1)));
    }
    
    /**
     * Complex condition 0 for 'L'
     */
    private boolean conditionL0(String value, int index) {
        if (index == value.length() - 3 && 
            contains(value, index - 1, 4, "ILLO", "ILLA", "ALLE")) {
            return true;
        } else if ((contains(value, index - 1, 2, "AS", "OS") || 
                    contains(value, value.length() - 1, 1, "A", "O")) &&
                   contains(value, index - 1, 4, "ALLE")) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Complex condition 0 for 'M'
     */
    private boolean conditionM0(String value, int index) {
		if (charAt(value, index + 1) == 'M') {
			return true;
		}
		return contains(value, index - 1, 3, "UMB")
				&& ((index + 1) == value.length() - 1 || contains(value,
						index + 2, 2, "ER"));
	}
    
    //-- BEGIN HELPER FUNCTIONS --//

    /**
	 * Determines whether or not a value is of slavo-germanic orgin. A value is
	 * of slavo-germanic origin if it contians any of 'W', 'K', 'CZ', or 'WITZ'.
	 */
    private boolean isSlavoGermanic(String value) {
        return value.indexOf('W') > -1 || value.indexOf('K') > -1 || 
            value.indexOf("CZ") > -1 || value.indexOf("WITZ") > -1;
    }

    /**
     * Determines whether or not a character is a vowel or not
     */
    private boolean isVowel(char ch) {
        return VOWELS.indexOf(ch) != -1;
    }

    /**
     * Determines whether or not the value starts with a silent letter.  It will
     * return <code>true</code> if the value starts with any of 'GN', 'KN',
     * 'PN', 'WR' or 'PS'.
     */    
    private boolean isSilentStart(String value) {
        boolean result = false;
        for (int i = 0; i < SILENT_START.length; i++) {
            if (value.startsWith(SILENT_START[i])) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Cleans the input
     */    
    private String cleanInput(String input) {
		if (input == null) {
			return null;
		}
		input = input.trim();
		if (input.length() == 0) {
			return null;
		}
		return input.toUpperCase();
	}

    /**
	 * Gets the character at index <code>index</code> if available, otherwise
	 * it returns <code>Character.MIN_VALUE</code> so that there is some sort
	 * of a default
	 */    
    protected char charAt(String value, int index) {
        if (index < 0 || index >= value.length()) {
            return Character.MIN_VALUE;
        } 
        return value.charAt(index);
    }

    /**
     * Shortcut method with 1 criteria
     */    
    private static boolean contains(String value, int start, int length, 
                                    String criteria) {
        return contains(value, start, length, 
                        new String[] { criteria });
    }

    /**
     * Shortcut method with 2 criteria
     */    
    private static boolean contains(String value, int start, int length, 
                                    String criteria1, String criteria2) {
        return contains(value, start, length, 
                        new String[] { criteria1, criteria2 });
    }

    /**
     * Shortcut method with 3 criteria
     */    
    private static boolean contains(String value, int start, int length, 
                                    String criteria1, String criteria2, 
                                    String criteria3) {
        return contains(value, start, length, 
                        new String[] { criteria1, criteria2, criteria3 });
    }

    /**
     * Shortcut method with 4 criteria
     */    
    private static boolean contains(String value, int start, int length, 
                                    String criteria1, String criteria2, 
                                    String criteria3, String criteria4) {
        return contains(value, start, length, 
                        new String[] { criteria1, criteria2, criteria3, 
                                       criteria4 });
    }

    /**
     * Shortcut method with 5 criteria
     */    
    private static boolean contains(String value, int start, int length, 
                                    String criteria1, String criteria2, 
                                    String criteria3, String criteria4, 
                                    String criteria5) {
        return contains(value, start, length, 
                        new String[] { criteria1, criteria2, criteria3, 
                                       criteria4, criteria5 });
    }

    /**
     * Shortcut method with 6 criteria
     */    
    private static boolean contains(String value, int start, int length, 
                                    String criteria1, String criteria2, 
                                    String criteria3, String criteria4, 
                                    String criteria5, String criteria6) {
        return contains(value, start, length, 
                        new String[] { criteria1, criteria2, criteria3, 
                                       criteria4, criteria5, criteria6 });
    }
    
    /**
     * Determines whether <code>value</code> contains any of the criteria 
     starting
     * at index <code>start</code> and matching up to length <code>length</code>
     */    
    protected static boolean contains(String value, int start, int length, 
                                      String[] criteria) {
        boolean result = false;
        if (start >= 0 && start + length <= value.length()) {
            String target = value.substring(start, start + length);

            for (int i = 0; i < criteria.length; i++) {
                if (target.equals(criteria[i])) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }
    
    //-- BEGIN INNER CLASSES --//
    
    /**
     * Inner class for storing results, since there is the optional alternate
     * encoding.
     */
    public class DoubleMetaphoneResult {

        private StringBuffer primary = new StringBuffer(getMaxCodeLen());
        private StringBuffer alternate = new StringBuffer(getMaxCodeLen());
        private int maxLength;

        public DoubleMetaphoneResult(int maxLength) {
            this.maxLength = maxLength;
        }

        public void append(char value) {
            appendPrimary(value);
            appendAlternate(value);
        }

        public void append(char primary, char alternate) {
            appendPrimary(primary);
            appendAlternate(alternate);
        }

        public void appendPrimary(char value) {
            if (this.primary.length() < this.maxLength) {
                this.primary.append(value);
            }
        }

        public void appendAlternate(char value) {
            if (this.alternate.length() < this.maxLength) {
                this.alternate.append(value);
            }
        }

        public void append(String value) {
            appendPrimary(value);
            appendAlternate(value);
        }

        public void append(String primary, String alternate) {
            appendPrimary(primary);
            appendAlternate(alternate);
        }

        public void appendPrimary(String value) {
            int addChars = this.maxLength - this.primary.length();
            if (value.length() <= addChars) {
                this.primary.append(value);
            } else {
                this.primary.append(value.substring(0, addChars));
            }
        }

        public void appendAlternate(String value) {
            int addChars = this.maxLength - this.alternate.length();
            if (value.length() <= addChars) {
                this.alternate.append(value);
            } else {
                this.alternate.append(value.substring(0, addChars));
            }
        }

        public String getPrimary() {
            return this.primary.toString();
        }

        public String getAlternate() {
            return this.alternate.toString();
        }

        public boolean isComplete() {
            return this.primary.length() >= this.maxLength && 
                this.alternate.length() >= this.maxLength;
        }
    }
}
