package com.fsck.k9.mail.internet;

import android.util.Log;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Locale;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.internet.JisSupport.SHIFT_JIS;

public class CharsetSupport {
    /**
     * Table for character set fall-back.
     *
     * Table format: unsupported charset (regular expression), fall-back charset
     */
    private static final String[][] CHARSET_FALLBACK_MAP = new String[][] {
            // Some Android versions don't support KOI8-U
            {"koi8-u", "koi8-r"},
            {"iso-2022-jp-[\\d]+", "iso-2022-jp"},
            // Default fall-back is US-ASCII
            {".*", "US-ASCII"}
    };


    public static void setCharset(String charset, Part part) throws MessagingException {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                part.getMimeType() + ";\r\n charset=" + getExternalCharset(charset));
    }


    public static String getCharsetFromAddress(String address) {
        String variant = JisSupport.getJisVariantFromAddress(address);
        if (variant != null) {
            String charset = "x-" + variant + "-shift_jis-2007";
            if (Charset.isSupported(charset))
                return charset;
        }

        return "UTF-8";
    }

    static String getExternalCharset(String charset) {
        if (JisSupport.isShiftJis(charset)) {
            return SHIFT_JIS;
        } else {
            return charset;
        }
    }

    static String fixupCharset(String charset, Message message) throws MessagingException {
        if (charset == null || "0".equals(charset))
            charset = "US-ASCII";  // No encoding, so use us-ascii, which is the standard.

        charset = charset.toLowerCase(Locale.US);
        if (charset.equals("cp932"))
            charset = SHIFT_JIS;

        if (charset.equals(SHIFT_JIS) || charset.equals("iso-2022-jp")) {
            String variant = JisSupport.getJisVariantFromMessage(message);
            if (variant != null)
                charset = "x-" + variant + "-" + charset + "-2007";
        }
        return charset;
    }


    static String readToString(InputStream in, String charset) throws IOException {
        boolean isIphoneString = false;

        // iso-2022-jp variants are supported by no versions as of Dec 2010.
        if (charset.length() > 19 && charset.startsWith("x-") &&
                charset.endsWith("-iso-2022-jp-2007") && !Charset.isSupported(charset)) {
            in = new Iso2022JpToShiftJisInputStream(in);
            charset = "x-" + charset.substring(2, charset.length() - 17) + "-shift_jis-2007";
        }

        // shift_jis variants are supported by Eclair and later.
        if (JisSupport.isShiftJis(charset) && !Charset.isSupported(charset)) {
            // If the JIS variant is iPhone, map the Unicode private use area in iPhone to the one in Android after
            // converting the character set from the standard Shift JIS to Unicode.
            if (charset.substring(2, charset.length() - 15).equals("iphone"))
                isIphoneString = true;

            charset = SHIFT_JIS;
        }

        /*
         * See if there is conversion from the MIME charset to the Java one.
         * this function may also throw an exception if the charset name is not known
         */
        boolean supported;
        try {
            supported = Charset.isSupported(charset);
        } catch (IllegalCharsetNameException e) {
            supported = false;
        }

        for (String[] rule: CHARSET_FALLBACK_MAP) {
            if (supported) {
                break;
            }

            if (charset.matches(rule[0])) {
                Log.e(LOG_TAG, "I don't know how to deal with the charset " + charset +
                        ". Falling back to " + rule[1]);
                charset = rule[1];
                try {
                    supported = Charset.isSupported(charset);
                } catch (IllegalCharsetNameException e) {
                    supported = false;
                }
            }
        }

        /*
         * Convert and return as new String
         */
        String str = IOUtils.toString(in, charset);

        if (isIphoneString)
            str = importStringFromIphone(str);
        return str;
    }

    private static String importStringFromIphone(String str) {
        StringBuilder buff = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i = str.offsetByCodePoints(i, 1)) {
            int codePoint = str.codePointAt(i);
            buff.appendCodePoint(importCodePointFromIphone(codePoint));
        }
        return buff.toString();
    }

    private static int importCodePointFromIphone(int codePoint) {
        switch (codePoint) {
            case 0xE001:
                return 0xFE19B;
            case 0xE002:
                return 0xFE19C;
            case 0xE003:
                return 0xFE823;
            case 0xE004:
                return 0xFE19D;
            case 0xE005:
                return 0xFE19E;
            case 0xE006:
                return 0xFE4CF;
            case 0xE007:
                return 0xFE4CD;
            case 0xE008:
                return 0xFE4EF;
            case 0xE009:
                return 0xFE523;
            case 0xE00A:
                return 0xFE525;
            case 0xE00B:
                return 0xFE528;
            case 0xE00C:
                return 0xFE538;
            case 0xE00D:
                return 0xFEB96;
            case 0xE00E:
                return 0xFEB97;
            case 0xE00F:
                return 0xFEB98;
            case 0xE010:
                return 0xFEB93;
            case 0xE011:
                return 0xFEB94;
            case 0xE012:
                return 0xFEB95;
            case 0xE013:
                return 0xFE7D5;
            case 0xE014:
                return 0xFE7D2;
            case 0xE015:
                return 0xFE7D3;
            case 0xE016:
                return 0xFE7D1;
            case 0xE017:
                return 0xFE7DA;
            case 0xE018:
                return 0xFE7D4;
            case 0xE019:
                return 0xFE1BD;
            case 0xE01A:
                return 0xFE1BE;
            case 0xE01B:
                return 0xFE7E4;
            case 0xE01C:
                return 0xFE7EA;
            case 0xE01D:
                return 0xFE7E9;
            case 0xE01E:
                return 0xFE7DF;
            case 0xE01F:
                return 0xFE7E3;
            case 0xE020:
                return 0xFEB09;
            case 0xE021:
                return 0xFEB04;
            case 0xE022:
                return 0xFEB0C;
            case 0xE023:
                return 0xFEB0E;
            case 0xE024:
                return 0xFE01E;
            case 0xE025:
                return 0xFE01F;
            case 0xE026:
                return 0xFE020;
            case 0xE027:
                return 0xFE021;
            case 0xE028:
                return 0xFE022;
            case 0xE029:
                return 0xFE023;
            case 0xE02A:
                return 0xFE024;
            case 0xE02B:
                return 0xFE025;
            case 0xE02C:
                return 0xFE026;
            case 0xE02D:
                return 0xFE027;
            case 0xE02E:
                return 0xFE028;
            case 0xE02F:
                return 0xFE029;
            case 0xE030:
                return 0xFE040;
            case 0xE031:
                return 0xFE4D2;
            case 0xE032:
                return 0xFE041;
            case 0xE033:
                return 0xFE512;
            case 0xE034:
                return 0xFE825;
            case 0xE035:
                return 0xFE826;
            case 0xE036:
                return 0xFE4B0;
            case 0xE037:
                return 0xFE4BB;
            case 0xE038:
                return 0xFE4B2;
            case 0xE039:
                return 0xFE7EC;
            case 0xE03A:
                return 0xFE7F5;
            case 0xE03B:
                return 0xFE4C3;
            case 0xE03C:
                return 0xFE800;
            case 0xE03D:
                return 0xFE801;
            case 0xE03E:
                return 0xFE813;
            case 0xE03F:
                return 0xFEB82;
            case 0xE040:
                return 0xFE815;
            case 0xE041:
                return 0xFE816;
            case 0xE042:
                return 0xFE818;
            case 0xE043:
                return 0xFE980;
            case 0xE044:
                return 0xFE982;
            case 0xE045:
                return 0xFE981;
            case 0xE046:
                return 0xFE962;
            case 0xE047:
                return 0xFE983;
            case 0xE048:
                return 0xFE003;
            case 0xE049:
                return 0xFE001;
            case 0xE04A:
                return 0xFE000;
            case 0xE04B:
                return 0xFE002;
            case 0xE04C:
                return 0xFE014;
            case 0xE04D:
                return 0xFE009;
            case 0xE04E:
                return 0xFE1AF;
            case 0xE04F:
                return 0xFE1B8;
            case 0xE050:
                return 0xFE1C0;
            case 0xE051:
                return 0xFE1C1;
            case 0xE052:
                return 0xFE1B7;
            case 0xE053:
                return 0xFE1C2;
            case 0xE054:
                return 0xFE1C3;
            case 0xE055:
                return 0xFE1BC;
            case 0xE056:
                return 0xFE335;
            case 0xE057:
                return 0xFE330;
            case 0xE058:
                return 0xFE323;
            case 0xE059:
                return 0xFE320;
            case 0xE05A:
                return 0xFE4F4;
            case 0xE101:
                return 0xFE52D;
            case 0xE102:
                return 0xFE52E;
            case 0xE103:
                return 0xFE52B;
            case 0xE104:
                return 0xFE526;
            case 0xE105:
                return 0xFE329;
            case 0xE106:
                return 0xFE327;
            case 0xE107:
                return 0xFE341;
            case 0xE108:
                return 0xFE344;
            case 0xE109:
                return 0xFE1C4;
            case 0xE10A:
                return 0xFE1C5;
            case 0xE10B:
                return 0xFE1BF;
            case 0xE10C:
                return 0xFE1B0;
            case 0xE10D:
                return 0xFE7ED;
            case 0xE10E:
                return 0xFE4D1;
            case 0xE10F:
                return 0xFEB56;
            case 0xE110:
                return 0xFE03C;
            case 0xE111:
                return 0xFE827;
            case 0xE112:
                return 0xFE510;
            case 0xE113:
                return 0xFE4F5;
            case 0xE114:
                return 0xFEB85;
            case 0xE115:
                return 0xFE7D9;
            case 0xE116:
                return 0xFE4CA;
            case 0xE117:
                return 0xFE515;
            case 0xE118:
                return 0xFE03F;
            case 0xE119:
                return 0xFE042;
            case 0xE11A:
                return 0xFE1B2;
            case 0xE11B:
                return 0xFE1AE;
            case 0xE11C:
                return 0xFE1B3;
            case 0xE11D:
                return 0xFE4F6;
            case 0xE11E:
                return 0xFE53B;
            case 0xE11F:
                return 0xFE537;
            case 0xE120:
                return 0xFE960;
            case 0xE121:
                return 0xFE4BC;
            case 0xE122:
                return 0xFE7FB;
            case 0xE123:
                return 0xFE7FA;
            case 0xE124:
                return 0xFE7FD;
            case 0xE125:
                return 0xFE807;
            case 0xE126:
                return 0xFE81D;
            case 0xE127:
                return 0xFE81E;
            case 0xE128:
                return 0xFE81F;
            case 0xE129:
                return 0xFE820;
            case 0xE12A:
                return 0xFE81C;
            case 0xE12B:
                return 0xFE1B1;
            case 0xE12C:
                return 0xFE81B;
            case 0xE12D:
                return 0xFE80B;
            case 0xE12E:
                return 0xFEB32;
            case 0xE12F:
                return 0xFE4DD;
            case 0xE130:
                return 0xFE80C;
            case 0xE131:
                return 0xFE7DB;
            case 0xE132:
                return 0xFE7D7;
            case 0xE133:
                return 0xFE80D;
            case 0xE134:
                return 0xFE7DC;
            case 0xE135:
                return 0xFE7EE;
            case 0xE136:
                return 0xFE7EB;
            case 0xE137:
                return 0xFE7F8;
            case 0xE138:
                return 0xFEB33;
            case 0xE139:
                return 0xFEB34;
            case 0xE13A:
                return 0xFEB35;
            case 0xE13B:
                return 0xFE509;
            case 0xE13C:
                return 0xFEB59;
            case 0xE13D:
                return 0xFE004;
            case 0xE13E:
                return 0xFE4D6;
            case 0xE13F:
                return 0xFE505;
            case 0xE140:
                return 0xFE507;
            case 0xE141:
                return 0xFE821;
            case 0xE142:
                return 0xFE52F;
            case 0xE143:
                return 0xFE514;
            case 0xE144:
                return 0xFEB86;
            case 0xE145:
                return 0xFEB87;
            case 0xE146:
                return 0xFE00B;
            case 0xE147:
                return 0xFE965;
            case 0xE148:
                return 0xFE546;
            case 0xE149:
                return 0xFE4DE;
            case 0xE14A:
                return 0xFE4DF;
            case 0xE14B:
                return 0xFE531;
            case 0xE14C:
                return 0xFEB5E;
            case 0xE14D:
                return 0xFE4B5;
            case 0xE14E:
                return 0xFE7F7;
            case 0xE14F:
                return 0xFE7F6;
            case 0xE150:
                return 0xFE7E7;
            case 0xE151:
                return 0xFE506;
            case 0xE152:
                return 0xFE1A1;
            case 0xE153:
                return 0xFE4B3;
            case 0xE154:
                return 0xFE4B6;
            case 0xE155:
                return 0xFE4B4;
            case 0xE156:
                return 0xFE4B9;
            case 0xE157:
                return 0xFE4BA;
            case 0xE158:
                return 0xFE4B7;
            case 0xE159:
                return 0xFE7E6;
            case 0xE15A:
                return 0xFE7EF;
            case 0xE201:
                return 0xFE7F0;
            case 0xE202:
                return 0xFE7E8;
            case 0xE203:
                return 0xFEB24;
            case 0xE204:
                return 0xFEB19;
            case 0xE205:
                return 0xFEB61;
            case 0xE206:
                return 0xFEB62;
            case 0xE207:
                return 0xFEB25;
            case 0xE208:
                return 0xFEB1F;
            case 0xE209:
                return 0xFE044;
            case 0xE20A:
                return 0xFEB20;
            case 0xE20B:
                return 0xFE838;
            case 0xE20C:
                return 0xFEB1A;
            case 0xE20D:
                return 0xFEB1C;
            case 0xE20E:
                return 0xFEB1B;
            case 0xE20F:
                return 0xFEB1D;
            case 0xE210:
                return 0xFE82C;
            case 0xE211:
                return 0xFE82B;
            case 0xE212:
                return 0xFEB36;
            case 0xE213:
                return 0xFEB37;
            case 0xE214:
                return 0xFEB38;
            case 0xE215:
                return 0xFEB39;
            case 0xE216:
                return 0xFEB3A;
            case 0xE217:
                return 0xFEB3B;
            case 0xE218:
                return 0xFEB3C;
            case 0xE219:
                return 0xFEB63;
            case 0xE21A:
                return 0xFEB64;
            case 0xE21B:
                return 0xFEB67;
            case 0xE21C:
                return 0xFE82E;
            case 0xE21D:
                return 0xFE82F;
            case 0xE21E:
                return 0xFE830;
            case 0xE21F:
                return 0xFE831;
            case 0xE220:
                return 0xFE832;
            case 0xE221:
                return 0xFE833;
            case 0xE222:
                return 0xFE834;
            case 0xE223:
                return 0xFE835;
            case 0xE224:
                return 0xFE836;
            case 0xE225:
                return 0xFE837;
            case 0xE226:
                return 0xFEB3D;
            case 0xE227:
                return 0xFEB3E;
            case 0xE228:
                return 0xFEB3F;
            case 0xE229:
                return 0xFEB81;
            case 0xE22A:
                return 0xFEB31;
            case 0xE22B:
                return 0xFEB2F;
            case 0xE22C:
                return 0xFEB40;
            case 0xE22D:
                return 0xFEB41;
            case 0xE22E:
                return 0xFEB99;
            case 0xE22F:
                return 0xFEB9A;
            case 0xE230:
                return 0xFEB9B;
            case 0xE231:
                return 0xFEB9C;
            case 0xE232:
                return 0xFEAF8;
            case 0xE233:
                return 0xFEAF9;
            case 0xE234:
                return 0xFEAFA;
            case 0xE235:
                return 0xFEAFB;
            case 0xE236:
                return 0xFEAF0;
            case 0xE237:
                return 0xFEAF2;
            case 0xE238:
                return 0xFEAF1;
            case 0xE239:
                return 0xFEAF3;
            case 0xE23A:
                return 0xFEAFC;
            case 0xE23B:
                return 0xFEAFD;
            case 0xE23C:
                return 0xFEAFE;
            case 0xE23D:
                return 0xFEAFF;
            case 0xE23E:
                return 0xFE4F8;
            case 0xE23F:
                return 0xFE02B;
            case 0xE240:
                return 0xFE02C;
            case 0xE241:
                return 0xFE02D;
            case 0xE242:
                return 0xFE02E;
            case 0xE243:
                return 0xFE02F;
            case 0xE244:
                return 0xFE030;
            case 0xE245:
                return 0xFE031;
            case 0xE246:
                return 0xFE032;
            case 0xE247:
                return 0xFE033;
            case 0xE248:
                return 0xFE034;
            case 0xE249:
                return 0xFE035;
            case 0xE24A:
                return 0xFE036;
            case 0xE24B:
                return 0xFE037;
            case 0xE24C:
                return 0xFEB42;
            case 0xE24D:
                return 0xFEB27;
            case 0xE24E:
                return 0xFEB29;
            case 0xE24F:
                return 0xFEB2D;
            case 0xE250:
                return 0xFE839;
            case 0xE251:
                return 0xFE83A;
            case 0xE252:
                return 0xFEB23;
            case 0xE253:
                return 0xFE1B4;
            case 0xE254:
                return 0xFEE77;
            case 0xE255:
                return 0xFEE78;
            case 0xE256:
                return 0xFEE79;
            case 0xE257:
                return 0xFEE7A;
            case 0xE258:
                return 0xFEE7B;
            case 0xE259:
                return 0xFEE7C;
            case 0xE25A:
                return 0xFEE7D;
            case 0xE301:
                return 0xFE527;
            case 0xE302:
                return 0xFE4D3;
            case 0xE303:
                return 0xFE045;
            case 0xE304:
                return 0xFE03D;
            case 0xE305:
                return 0xFE046;
            case 0xE306:
                return 0xFE828;
            case 0xE307:
                return 0xFE047;
            case 0xE308:
                return 0xFE048;
            case 0xE309:
                return 0xFE508;
            case 0xE30A:
                return 0xFE803;
            case 0xE30B:
                return 0xFE985;
            case 0xE30C:
                return 0xFE987;
            case 0xE30D:
                return 0xFEB43;
            case 0xE30E:
                return 0xFEB1E;
            case 0xE30F:
                return 0xFE50A;
            case 0xE310:
                return 0xFE516;
            case 0xE311:
                return 0xFEB58;
            case 0xE312:
                return 0xFE517;
            case 0xE313:
                return 0xFE53E;
            case 0xE314:
                return 0xFE50F;
            case 0xE315:
                return 0xFEB2B;
            case 0xE316:
                return 0xFE53C;
            case 0xE317:
                return 0xFE530;
            case 0xE318:
                return 0xFE4D4;
            case 0xE319:
                return 0xFE4D5;
            case 0xE31A:
                return 0xFE4D7;
            case 0xE31B:
                return 0xFE4D8;
            case 0xE31C:
                return 0xFE195;
            case 0xE31D:
                return 0xFE196;
            case 0xE31E:
                return 0xFE197;
            case 0xE31F:
                return 0xFE198;
            case 0xE320:
                return 0xFE199;
            case 0xE321:
                return 0xFE4D9;
            case 0xE322:
                return 0xFE4DA;
            case 0xE323:
                return 0xFE4F0;
            case 0xE324:
                return 0xFE808;
            case 0xE325:
                return 0xFE4F2;
            case 0xE326:
                return 0xFE814;
            case 0xE327:
                return 0xFEB0D;
            case 0xE328:
                return 0xFEB11;
            case 0xE329:
                return 0xFEB12;
            case 0xE32A:
                return 0xFEB13;
            case 0xE32B:
                return 0xFEB14;
            case 0xE32C:
                return 0xFEB15;
            case 0xE32D:
                return 0xFEB16;
            case 0xE32E:
                return 0xFEB60;
            case 0xE32F:
                return 0xFEB68;
            case 0xE330:
                return 0xFEB5D;
            case 0xE331:
                return 0xFEB5B;
            case 0xE332:
                return 0xFEB44;
            case 0xE333:
                return 0xFEB45;
            case 0xE334:
                return 0xFEB57;
            case 0xE335:
                return 0xFEB69;
            case 0xE336:
                return 0xFEB0A;
            case 0xE337:
                return 0xFEB0B;
            case 0xE338:
                return 0xFE984;
            case 0xE339:
                return 0xFE964;
            case 0xE33A:
                return 0xFE966;
            case 0xE33B:
                return 0xFE967;
            case 0xE33C:
                return 0xFE968;
            case 0xE33D:
                return 0xFE969;
            case 0xE33E:
                return 0xFE96A;
            case 0xE33F:
                return 0xFE96B;
            case 0xE340:
                return 0xFE963;
            case 0xE341:
                return 0xFE96C;
            case 0xE342:
                return 0xFE961;
            case 0xE343:
                return 0xFE96D;
            case 0xE344:
                return 0xFE96E;
            case 0xE345:
                return 0xFE051;
            case 0xE346:
                return 0xFE052;
            case 0xE347:
                return 0xFE053;
            case 0xE348:
                return 0xFE054;
            case 0xE349:
                return 0xFE055;
            case 0xE34A:
                return 0xFE056;
            case 0xE34B:
                return 0xFE511;
            case 0xE34C:
                return 0xFE96F;
            case 0xE34D:
                return 0xFE970;
            case 0xE401:
                return 0xFE345;
            case 0xE402:
                return 0xFE343;
            case 0xE403:
                return 0xFE340;
            case 0xE404:
                return 0xFE333;
            case 0xE405:
                return 0xFE347;
            case 0xE406:
                return 0xFE33C;
            case 0xE407:
                return 0xFE33F;
            case 0xE408:
                return 0xFE342;
            case 0xE409:
                return 0xFE32A;
            case 0xE40A:
                return 0xFE33E;
            case 0xE40B:
                return 0xFE33B;
            case 0xE40C:
                return 0xFE32E;
            case 0xE40D:
                return 0xFE32F;
            case 0xE40E:
                return 0xFE326;
            case 0xE40F:
                return 0xFE325;
            case 0xE410:
                return 0xFE322;
            case 0xE411:
                return 0xFE33A;
            case 0xE412:
                return 0xFE334;
            case 0xE413:
                return 0xFE339;
            case 0xE414:
                return 0xFE336;
            case 0xE415:
                return 0xFE338;
            case 0xE416:
                return 0xFE33D;
            case 0xE417:
                return 0xFE32D;
            case 0xE418:
                return 0xFE32C;
            case 0xE419:
                return 0xFE190;
            case 0xE41A:
                return 0xFE192;
            case 0xE41B:
                return 0xFE191;
            case 0xE41C:
                return 0xFE193;
            case 0xE41D:
                return 0xFE35B;
            case 0xE41E:
                return 0xFEB9D;
            case 0xE41F:
                return 0xFEB9E;
            case 0xE420:
                return 0xFEB9F;
            case 0xE421:
                return 0xFEBA0;
            case 0xE422:
                return 0xFEBA1;
            case 0xE423:
                return 0xFE351;
            case 0xE424:
                return 0xFE352;
            case 0xE425:
                return 0xFE829;
            case 0xE426:
                return 0xFE353;
            case 0xE427:
                return 0xFE358;
            case 0xE428:
                return 0xFE1A0;
            case 0xE429:
                return 0xFE1A2;
            case 0xE42A:
                return 0xFE7D6;
            case 0xE42B:
                return 0xFE7DD;
            case 0xE42C:
                return 0xFE80E;
            case 0xE42D:
                return 0xFE7DE;
            case 0xE42E:
                return 0xFE7E5;
            case 0xE42F:
                return 0xFE7F1;
            case 0xE430:
                return 0xFE7F2;
            case 0xE431:
                return 0xFE7F3;
            case 0xE432:
                return 0xFE7F4;
            case 0xE433:
                return 0xFE7FE;
            case 0xE434:
                return 0xFE7E0;
            case 0xE435:
                return 0xFE7E2;
            case 0xE436:
                return 0xFE518;
            case 0xE437:
                return 0xFEB17;
            case 0xE438:
                return 0xFE519;
            case 0xE439:
                return 0xFE51A;
            case 0xE43A:
                return 0xFE51B;
            case 0xE43B:
                return 0xFE51C;
            case 0xE43C:
                return 0xFE007;
            case 0xE43D:
                return 0xFE82A;
            case 0xE43E:
                return 0xFE038;
            case 0xE43F:
                return 0xFE971;
            case 0xE440:
                return 0xFE51D;
            case 0xE441:
                return 0xFE1C6;
            case 0xE442:
                return 0xFE51E;
            case 0xE443:
                return 0xFE005;
            case 0xE444:
                return 0xFE049;
            case 0xE445:
                return 0xFE51F;
            case 0xE446:
                return 0xFE017;
            case 0xE447:
                return 0xFE043;
            case 0xE448:
                return 0xFE513;
            case 0xE449:
                return 0xFE00A;
            case 0xE44A:
                return 0xFE00C;
            case 0xE44B:
                return 0xFE008;
            case 0xE44C:
                return 0xFE00D;
            case 0xE501:
                return 0xFE4B8;
            case 0xE502:
                return 0xFE804;
            case 0xE503:
                return 0xFE805;
            case 0xE504:
                return 0xFE4BD;
            case 0xE505:
                return 0xFE4BE;
            case 0xE506:
                return 0xFE4BF;
            case 0xE507:
                return 0xFE802;
            case 0xE508:
                return 0xFE4C0;
            case 0xE509:
                return 0xFE4C4;
            case 0xE50A:
                return 0xFE4C5;
            case 0xE50B:
                return 0xFE4E5;
            case 0xE50C:
                return 0xFE4E6;
            case 0xE50D:
                return 0xFE4E7;
            case 0xE50E:
                return 0xFE4E8;
            case 0xE50F:
                return 0xFE4E9;
            case 0xE510:
                return 0xFE4EA;
            case 0xE511:
                return 0xFE4EB;
            case 0xE512:
                return 0xFE4EC;
            case 0xE513:
                return 0xFE4ED;
            case 0xE514:
                return 0xFE4EE;
            case 0xE515:
                return 0xFE1A4;
            case 0xE516:
                return 0xFE1A5;
            case 0xE517:
                return 0xFE1A6;
            case 0xE518:
                return 0xFE1A7;
            case 0xE519:
                return 0xFE1A8;
            case 0xE51A:
                return 0xFE1A9;
            case 0xE51B:
                return 0xFE1AA;
            case 0xE51C:
                return 0xFE1AB;
            case 0xE51D:
                return 0xFE4C6;
            case 0xE51E:
                return 0xFE1B5;
            case 0xE51F:
                return 0xFE1B6;
            case 0xE520:
                return 0xFE1C7;
            case 0xE521:
                return 0xFE1C8;
            case 0xE522:
                return 0xFE1C9;
            case 0xE523:
                return 0xFE1BA;
            case 0xE524:
                return 0xFE1CA;
            case 0xE525:
                return 0xFE1CB;
            case 0xE526:
                return 0xFE1CC;
            case 0xE527:
                return 0xFE1CD;
            case 0xE528:
                return 0xFE1CE;
            case 0xE529:
                return 0xFE1CF;
            case 0xE52A:
                return 0xFE1D0;
            case 0xE52B:
                return 0xFE1D1;
            case 0xE52C:
                return 0xFE1D2;
            case 0xE52D:
                return 0xFE1D3;
            case 0xE52E:
                return 0xFE1D4;
            case 0xE52F:
                return 0xFE1D5;
            case 0xE530:
                return 0xFE1D6;
            case 0xE531:
                return 0xFE1D7;
            case 0xE532:
                return 0xFE50B;
            case 0xE533:
                return 0xFE50C;
            case 0xE534:
                return 0xFE50D;
            case 0xE535:
                return 0xFE50E;
            case 0xE536:
                return 0xFE553;
            case 0xE537:
                return 0xFEB2A;
            case 0xE538:
                return 0xFEE70;
            case 0xE539:
                return 0xFEE71;
            case 0xE53A:
                return 0xFEE72;
            case 0xE53B:
                return 0xFEE73;
            case 0xE53C:
                return 0xFEE74;
            case 0xE53D:
                return 0xFEE75;
            case 0xE53E:
                return 0xFEE76;
            default:
                return codePoint;
        }
    }

}
