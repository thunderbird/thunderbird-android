
package com.fsck.k9.mail.internet;

import android.content.Context;
import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.internet.BinaryTempFileBody.BinaryTempFileBodyInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;


public class MimeUtility {
    public static final String DEFAULT_ATTACHMENT_MIME_TYPE = "application/octet-stream";

    public static final String K9_SETTINGS_MIME_TYPE = "application/x-k9settings";

    private static final String TEXT_DIVIDER =
            "------------------------------------------------------------------------";

    /*
     * http://www.w3schools.com/media/media_mimeref.asp
     * +
     * http://www.stdicon.com/mimetypes
     */
    public static final String[][] MIME_TYPE_BY_EXTENSION_MAP = new String[][] {
        //* Do not delete the next two lines
    { "", DEFAULT_ATTACHMENT_MIME_TYPE },
    { "k9s", K9_SETTINGS_MIME_TYPE},
    //* Do not delete the previous two lines
    { "123", "application/vnd.lotus-1-2-3"},
    { "323", "text/h323"},
    { "3dml", "text/vnd.in3d.3dml"},
    { "3g2", "video/3gpp2"},
    { "3gp", "video/3gpp"},
    { "aab", "application/x-authorware-bin"},
    { "aac", "audio/x-aac"},
    { "aam", "application/x-authorware-map"},
    { "a", "application/octet-stream"},
    { "aas", "application/x-authorware-seg"},
    { "abw", "application/x-abiword"},
    { "acc", "application/vnd.americandynamics.acc"},
    { "ace", "application/x-ace-compressed"},
    { "acu", "application/vnd.acucobol"},
    { "acutc", "application/vnd.acucorp"},
    { "acx", "application/internet-property-stream"},
    { "adp", "audio/adpcm"},
    { "aep", "application/vnd.audiograph"},
    { "afm", "application/x-font-type1"},
    { "afp", "application/vnd.ibm.modcap"},
    { "ai", "application/postscript"},
    { "aif", "audio/x-aiff"},
    { "aifc", "audio/x-aiff"},
    { "aiff", "audio/x-aiff"},
    { "air", "application/vnd.adobe.air-application-installer-package+zip"},
    { "ami", "application/vnd.amiga.ami"},
    { "apk", "application/vnd.android.package-archive"},
    { "application", "application/x-ms-application"},
    { "apr", "application/vnd.lotus-approach"},
    { "asc", "application/pgp-signature"},
    { "asf", "video/x-ms-asf"},
    { "asm", "text/x-asm"},
    { "aso", "application/vnd.accpac.simply.aso"},
    { "asr", "video/x-ms-asf"},
    { "asx", "video/x-ms-asf"},
    { "atc", "application/vnd.acucorp"},
    { "atom", "application/atom+xml"},
    { "atomcat", "application/atomcat+xml"},
    { "atomsvc", "application/atomsvc+xml"},
    { "atx", "application/vnd.antix.game-component"},
    { "au", "audio/basic"},
    { "avi", "video/x-msvideo"},
    { "aw", "application/applixware"},
    { "axs", "application/olescript"},
    { "azf", "application/vnd.airzip.filesecure.azf"},
    { "azs", "application/vnd.airzip.filesecure.azs"},
    { "azw", "application/vnd.amazon.ebook"},
    { "bas", "text/plain"},
    { "bat", "application/x-msdownload"},
    { "bcpio", "application/x-bcpio"},
    { "bdf", "application/x-font-bdf"},
    { "bdm", "application/vnd.syncml.dm+wbxml"},
    { "bh2", "application/vnd.fujitsu.oasysprs"},
    { "bin", "application/octet-stream"},
    { "bmi", "application/vnd.bmi"},
    { "bmp", "image/bmp"},
    { "book", "application/vnd.framemaker"},
    { "box", "application/vnd.previewsystems.box"},
    { "boz", "application/x-bzip2"},
    { "bpk", "application/octet-stream"},
    { "btif", "image/prs.btif"},
    { "bz2", "application/x-bzip2"},
    { "bz", "application/x-bzip"},
    { "c4d", "application/vnd.clonk.c4group"},
    { "c4f", "application/vnd.clonk.c4group"},
    { "c4g", "application/vnd.clonk.c4group"},
    { "c4p", "application/vnd.clonk.c4group"},
    { "c4u", "application/vnd.clonk.c4group"},
    { "cab", "application/vnd.ms-cab-compressed"},
    { "car", "application/vnd.curl.car"},
    { "cat", "application/vnd.ms-pki.seccat"},
    { "cct", "application/x-director"},
    { "cc", "text/x-c"},
    { "ccxml", "application/ccxml+xml"},
    { "cdbcmsg", "application/vnd.contact.cmsg"},
    { "cdf", "application/x-cdf"},
    { "cdkey", "application/vnd.mediastation.cdkey"},
    { "cdx", "chemical/x-cdx"},
    { "cdxml", "application/vnd.chemdraw+xml"},
    { "cdy", "application/vnd.cinderella"},
    { "cer", "application/x-x509-ca-cert"},
    { "cgm", "image/cgm"},
    { "chat", "application/x-chat"},
    { "chm", "application/vnd.ms-htmlhelp"},
    { "chrt", "application/vnd.kde.kchart"},
    { "cif", "chemical/x-cif"},
    { "cii", "application/vnd.anser-web-certificate-issue-initiation"},
    { "cla", "application/vnd.claymore"},
    { "class", "application/java-vm"},
    { "clkk", "application/vnd.crick.clicker.keyboard"},
    { "clkp", "application/vnd.crick.clicker.palette"},
    { "clkt", "application/vnd.crick.clicker.template"},
    { "clkw", "application/vnd.crick.clicker.wordbank"},
    { "clkx", "application/vnd.crick.clicker"},
    { "clp", "application/x-msclip"},
    { "cmc", "application/vnd.cosmocaller"},
    { "cmdf", "chemical/x-cmdf"},
    { "cml", "chemical/x-cml"},
    { "cmp", "application/vnd.yellowriver-custom-menu"},
    { "cmx", "image/x-cmx"},
    { "cod", "application/vnd.rim.cod"},
    { "com", "application/x-msdownload"},
    { "conf", "text/plain"},
    { "cpio", "application/x-cpio"},
    { "cpp", "text/x-c"},
    { "cpt", "application/mac-compactpro"},
    { "crd", "application/x-mscardfile"},
    { "crl", "application/pkix-crl"},
    { "crt", "application/x-x509-ca-cert"},
    { "csh", "application/x-csh"},
    { "csml", "chemical/x-csml"},
    { "csp", "application/vnd.commonspace"},
    { "css", "text/css"},
    { "cst", "application/x-director"},
    { "csv", "text/csv"},
    { "c", "text/plain"},
    { "cu", "application/cu-seeme"},
    { "curl", "text/vnd.curl"},
    { "cww", "application/prs.cww"},
    { "cxt", "application/x-director"},
    { "cxx", "text/x-c"},
    { "daf", "application/vnd.mobius.daf"},
    { "dataless", "application/vnd.fdsn.seed"},
    { "davmount", "application/davmount+xml"},
    { "dcr", "application/x-director"},
    { "dcurl", "text/vnd.curl.dcurl"},
    { "dd2", "application/vnd.oma.dd2+xml"},
    { "ddd", "application/vnd.fujixerox.ddd"},
    { "deb", "application/x-debian-package"},
    { "def", "text/plain"},
    { "deploy", "application/octet-stream"},
    { "der", "application/x-x509-ca-cert"},
    { "dfac", "application/vnd.dreamfactory"},
    { "dic", "text/x-c"},
    { "diff", "text/plain"},
    { "dir", "application/x-director"},
    { "dis", "application/vnd.mobius.dis"},
    { "dist", "application/octet-stream"},
    { "distz", "application/octet-stream"},
    { "djv", "image/vnd.djvu"},
    { "djvu", "image/vnd.djvu"},
    { "dll", "application/x-msdownload"},
    { "dmg", "application/octet-stream"},
    { "dms", "application/octet-stream"},
    { "dna", "application/vnd.dna"},
    { "doc", "application/msword"},
    { "docm", "application/vnd.ms-word.document.macroenabled.12"},
    { "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
    { "dot", "application/msword"},
    { "dotm", "application/vnd.ms-word.template.macroenabled.12"},
    { "dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template"},
    { "dp", "application/vnd.osgi.dp"},
    { "dpg", "application/vnd.dpgraph"},
    { "dsc", "text/prs.lines.tag"},
    { "dtb", "application/x-dtbook+xml"},
    { "dtd", "application/xml-dtd"},
    { "dts", "audio/vnd.dts"},
    { "dtshd", "audio/vnd.dts.hd"},
    { "dump", "application/octet-stream"},
    { "dvi", "application/x-dvi"},
    { "dwf", "model/vnd.dwf"},
    { "dwg", "image/vnd.dwg"},
    { "dxf", "image/vnd.dxf"},
    { "dxp", "application/vnd.spotfire.dxp"},
    { "dxr", "application/x-director"},
    { "ecelp4800", "audio/vnd.nuera.ecelp4800"},
    { "ecelp7470", "audio/vnd.nuera.ecelp7470"},
    { "ecelp9600", "audio/vnd.nuera.ecelp9600"},
    { "ecma", "application/ecmascript"},
    { "edm", "application/vnd.novadigm.edm"},
    { "edx", "application/vnd.novadigm.edx"},
    { "efif", "application/vnd.picsel"},
    { "ei6", "application/vnd.pg.osasli"},
    { "elc", "application/octet-stream"},
    { "eml", "message/rfc822"},
    { "emma", "application/emma+xml"},
    { "eol", "audio/vnd.digital-winds"},
    { "eot", "application/vnd.ms-fontobject"},
    { "eps", "application/postscript"},
    { "epub", "application/epub+zip"},
    { "es3", "application/vnd.eszigno3+xml"},
    { "esf", "application/vnd.epson.esf"},
    { "et3", "application/vnd.eszigno3+xml"},
    { "etx", "text/x-setext"},
    { "evy", "application/envoy"},
    { "exe", "application/octet-stream"},
    { "ext", "application/vnd.novadigm.ext"},
    { "ez2", "application/vnd.ezpix-album"},
    { "ez3", "application/vnd.ezpix-package"},
    { "ez", "application/andrew-inset"},
    { "f4v", "video/x-f4v"},
    { "f77", "text/x-fortran"},
    { "f90", "text/x-fortran"},
    { "fbs", "image/vnd.fastbidsheet"},
    { "fdf", "application/vnd.fdf"},
    { "fe_launch", "application/vnd.denovo.fcselayout-link"},
    { "fg5", "application/vnd.fujitsu.oasysgp"},
    { "fgd", "application/x-director"},
    { "fh4", "image/x-freehand"},
    { "fh5", "image/x-freehand"},
    { "fh7", "image/x-freehand"},
    { "fhc", "image/x-freehand"},
    { "fh", "image/x-freehand"},
    { "fif", "application/fractals"},
    { "fig", "application/x-xfig"},
    { "fli", "video/x-fli"},
    { "flo", "application/vnd.micrografx.flo"},
    { "flr", "x-world/x-vrml"},
    { "flv", "video/x-flv"},
    { "flw", "application/vnd.kde.kivio"},
    { "flx", "text/vnd.fmi.flexstor"},
    { "fly", "text/vnd.fly"},
    { "fm", "application/vnd.framemaker"},
    { "fnc", "application/vnd.frogans.fnc"},
    { "for", "text/x-fortran"},
    { "fpx", "image/vnd.fpx"},
    { "frame", "application/vnd.framemaker"},
    { "fsc", "application/vnd.fsc.weblaunch"},
    { "fst", "image/vnd.fst"},
    { "ftc", "application/vnd.fluxtime.clip"},
    { "f", "text/x-fortran"},
    { "fti", "application/vnd.anser-web-funds-transfer-initiation"},
    { "fvt", "video/vnd.fvt"},
    { "fzs", "application/vnd.fuzzysheet"},
    { "g3", "image/g3fax"},
    { "gac", "application/vnd.groove-account"},
    { "gdl", "model/vnd.gdl"},
    { "geo", "application/vnd.dynageo"},
    { "gex", "application/vnd.geometry-explorer"},
    { "ggb", "application/vnd.geogebra.file"},
    { "ggt", "application/vnd.geogebra.tool"},
    { "ghf", "application/vnd.groove-help"},
    { "gif", "image/gif"},
    { "gim", "application/vnd.groove-identity-message"},
    { "gmx", "application/vnd.gmx"},
    { "gnumeric", "application/x-gnumeric"},
    { "gph", "application/vnd.flographit"},
    { "gqf", "application/vnd.grafeq"},
    { "gqs", "application/vnd.grafeq"},
    { "gram", "application/srgs"},
    { "gre", "application/vnd.geometry-explorer"},
    { "grv", "application/vnd.groove-injector"},
    { "grxml", "application/srgs+xml"},
    { "gsf", "application/x-font-ghostscript"},
    { "gtar", "application/x-gtar"},
    { "gtm", "application/vnd.groove-tool-message"},
    { "gtw", "model/vnd.gtw"},
    { "gv", "text/vnd.graphviz"},
    { "gz", "application/x-gzip"},
    { "h261", "video/h261"},
    { "h263", "video/h263"},
    { "h264", "video/h264"},
    { "hbci", "application/vnd.hbci"},
    { "hdf", "application/x-hdf"},
    { "hh", "text/x-c"},
    { "hlp", "application/winhlp"},
    { "hpgl", "application/vnd.hp-hpgl"},
    { "hpid", "application/vnd.hp-hpid"},
    { "hps", "application/vnd.hp-hps"},
    { "hqx", "application/mac-binhex40"},
    { "hta", "application/hta"},
    { "htc", "text/x-component"},
    { "h", "text/plain"},
    { "htke", "application/vnd.kenameaapp"},
    { "html", "text/html"},
    { "htm", "text/html"},
    { "htt", "text/webviewhtml"},
    { "hvd", "application/vnd.yamaha.hv-dic"},
    { "hvp", "application/vnd.yamaha.hv-voice"},
    { "hvs", "application/vnd.yamaha.hv-script"},
    { "icc", "application/vnd.iccprofile"},
    { "ice", "x-conference/x-cooltalk"},
    { "icm", "application/vnd.iccprofile"},
    { "ico", "image/x-icon"},
    { "ics", "text/calendar"},
    { "ief", "image/ief"},
    { "ifb", "text/calendar"},
    { "ifm", "application/vnd.shana.informed.formdata"},
    { "iges", "model/iges"},
    { "igl", "application/vnd.igloader"},
    { "igs", "model/iges"},
    { "igx", "application/vnd.micrografx.igx"},
    { "iif", "application/vnd.shana.informed.interchange"},
    { "iii", "application/x-iphone"},
    { "imp", "application/vnd.accpac.simply.imp"},
    { "ims", "application/vnd.ms-ims"},
    { "ins", "application/x-internet-signup"},
    { "in", "text/plain"},
    { "ipk", "application/vnd.shana.informed.package"},
    { "irm", "application/vnd.ibm.rights-management"},
    { "irp", "application/vnd.irepository.package+xml"},
    { "iso", "application/octet-stream"},
    { "isp", "application/x-internet-signup"},
    { "itp", "application/vnd.shana.informed.formtemplate"},
    { "ivp", "application/vnd.immervision-ivp"},
    { "ivu", "application/vnd.immervision-ivu"},
    { "jad", "text/vnd.sun.j2me.app-descriptor"},
    { "jam", "application/vnd.jam"},
    { "jar", "application/java-archive"},
    { "java", "text/x-java-source"},
    { "jfif", "image/pipeg"},
    { "jisp", "application/vnd.jisp"},
    { "jlt", "application/vnd.hp-jlyt"},
    { "jnlp", "application/x-java-jnlp-file"},
    { "joda", "application/vnd.joost.joda-archive"},
    { "jpeg", "image/jpeg"},
    { "jpe", "image/jpeg"},
    { "jpg", "image/jpeg"},
    { "jpgm", "video/jpm"},
    { "jpgv", "video/jpeg"},
    { "jpm", "video/jpm"},
    { "js", "application/x-javascript"},
    { "json", "application/json"},
    { "kar", "audio/midi"},
    { "karbon", "application/vnd.kde.karbon"},
    { "kfo", "application/vnd.kde.kformula"},
    { "kia", "application/vnd.kidspiration"},
    { "kil", "application/x-killustrator"},
    { "kml", "application/vnd.google-earth.kml+xml"},
    { "kmz", "application/vnd.google-earth.kmz"},
    { "kne", "application/vnd.kinar"},
    { "knp", "application/vnd.kinar"},
    { "kon", "application/vnd.kde.kontour"},
    { "kpr", "application/vnd.kde.kpresenter"},
    { "kpt", "application/vnd.kde.kpresenter"},
    { "ksh", "text/plain"},
    { "ksp", "application/vnd.kde.kspread"},
    { "ktr", "application/vnd.kahootz"},
    { "ktz", "application/vnd.kahootz"},
    { "kwd", "application/vnd.kde.kword"},
    { "kwt", "application/vnd.kde.kword"},
    { "latex", "application/x-latex"},
    { "lbd", "application/vnd.llamagraphics.life-balance.desktop"},
    { "lbe", "application/vnd.llamagraphics.life-balance.exchange+xml"},
    { "les", "application/vnd.hhe.lesson-player"},
    { "lha", "application/octet-stream"},
    { "link66", "application/vnd.route66.link66+xml"},
    { "list3820", "application/vnd.ibm.modcap"},
    { "listafp", "application/vnd.ibm.modcap"},
    { "list", "text/plain"},
    { "log", "text/plain"},
    { "lostxml", "application/lost+xml"},
    { "lrf", "application/octet-stream"},
    { "lrm", "application/vnd.ms-lrm"},
    { "lsf", "video/x-la-asf"},
    { "lsx", "video/x-la-asf"},
    { "ltf", "application/vnd.frogans.ltf"},
    { "lvp", "audio/vnd.lucent.voice"},
    { "lwp", "application/vnd.lotus-wordpro"},
    { "lzh", "application/octet-stream"},
    { "m13", "application/x-msmediaview"},
    { "m14", "application/x-msmediaview"},
    { "m1v", "video/mpeg"},
    { "m2a", "audio/mpeg"},
    { "m2v", "video/mpeg"},
    { "m3a", "audio/mpeg"},
    { "m3u", "audio/x-mpegurl"},
    { "m4u", "video/vnd.mpegurl"},
    { "m4v", "video/x-m4v"},
    { "ma", "application/mathematica"},
    { "mag", "application/vnd.ecowin.chart"},
    { "maker", "application/vnd.framemaker"},
    { "man", "text/troff"},
    { "mathml", "application/mathml+xml"},
    { "mb", "application/mathematica"},
    { "mbk", "application/vnd.mobius.mbk"},
    { "mbox", "application/mbox"},
    { "mc1", "application/vnd.medcalcdata"},
    { "mcd", "application/vnd.mcd"},
    { "mcurl", "text/vnd.curl.mcurl"},
    { "mdb", "application/x-msaccess"},
    { "mdi", "image/vnd.ms-modi"},
    { "mesh", "model/mesh"},
    { "me", "text/troff"},
    { "mfm", "application/vnd.mfmp"},
    { "mgz", "application/vnd.proteus.magazine"},
    { "mht", "message/rfc822"},
    { "mhtml", "message/rfc822"},
    { "mid", "audio/midi"},
    { "midi", "audio/midi"},
    { "mif", "application/vnd.mif"},
    { "mime", "message/rfc822"},
    { "mj2", "video/mj2"},
    { "mjp2", "video/mj2"},
    { "mlp", "application/vnd.dolby.mlp"},
    { "mmd", "application/vnd.chipnuts.karaoke-mmd"},
    { "mmf", "application/vnd.smaf"},
    { "mmr", "image/vnd.fujixerox.edmics-mmr"},
    { "mny", "application/x-msmoney"},
    { "mobi", "application/x-mobipocket-ebook"},
    { "movie", "video/x-sgi-movie"},
    { "mov", "video/quicktime"},
    { "mp2a", "audio/mpeg"},
    { "mp2", "video/mpeg"},
    { "mp3", "audio/mpeg"},
    { "mp4a", "audio/mp4"},
    { "mp4s", "application/mp4"},
    { "mp4", "video/mp4"},
    { "mp4v", "video/mp4"},
    { "mpa", "video/mpeg"},
    { "mpc", "application/vnd.mophun.certificate"},
    { "mpeg", "video/mpeg"},
    { "mpe", "video/mpeg"},
    { "mpg4", "video/mp4"},
    { "mpga", "audio/mpeg"},
    { "mpg", "video/mpeg"},
    { "mpkg", "application/vnd.apple.installer+xml"},
    { "mpm", "application/vnd.blueice.multipass"},
    { "mpn", "application/vnd.mophun.application"},
    { "mpp", "application/vnd.ms-project"},
    { "mpt", "application/vnd.ms-project"},
    { "mpv2", "video/mpeg"},
    { "mpy", "application/vnd.ibm.minipay"},
    { "mqy", "application/vnd.mobius.mqy"},
    { "mrc", "application/marc"},
    { "mscml", "application/mediaservercontrol+xml"},
    { "mseed", "application/vnd.fdsn.mseed"},
    { "mseq", "application/vnd.mseq"},
    { "msf", "application/vnd.epson.msf"},
    { "msh", "model/mesh"},
    { "msi", "application/x-msdownload"},
    { "ms", "text/troff"},
    { "msty", "application/vnd.muvee.style"},
    { "mts", "model/vnd.mts"},
    { "mus", "application/vnd.musician"},
    { "musicxml", "application/vnd.recordare.musicxml+xml"},
    { "mvb", "application/x-msmediaview"},
    { "mxf", "application/mxf"},
    { "mxl", "application/vnd.recordare.musicxml"},
    { "mxml", "application/xv+xml"},
    { "mxs", "application/vnd.triscape.mxs"},
    { "mxu", "video/vnd.mpegurl"},
    { "nb", "application/mathematica"},
    { "nc", "application/x-netcdf"},
    { "ncx", "application/x-dtbncx+xml"},
    { "n-gage", "application/vnd.nokia.n-gage.symbian.install"},
    { "ngdat", "application/vnd.nokia.n-gage.data"},
    { "nlu", "application/vnd.neurolanguage.nlu"},
    { "nml", "application/vnd.enliven"},
    { "nnd", "application/vnd.noblenet-directory"},
    { "nns", "application/vnd.noblenet-sealer"},
    { "nnw", "application/vnd.noblenet-web"},
    { "npx", "image/vnd.net-fpx"},
    { "nsf", "application/vnd.lotus-notes"},
    { "nws", "message/rfc822"},
    { "oa2", "application/vnd.fujitsu.oasys2"},
    { "oa3", "application/vnd.fujitsu.oasys3"},
    { "o", "application/octet-stream"},
    { "oas", "application/vnd.fujitsu.oasys"},
    { "obd", "application/x-msbinder"},
    { "obj", "application/octet-stream"},
    { "oda", "application/oda"},
    { "odb", "application/vnd.oasis.opendocument.database"},
    { "odc", "application/vnd.oasis.opendocument.chart"},
    { "odf", "application/vnd.oasis.opendocument.formula"},
    { "odft", "application/vnd.oasis.opendocument.formula-template"},
    { "odg", "application/vnd.oasis.opendocument.graphics"},
    { "odi", "application/vnd.oasis.opendocument.image"},
    { "odp", "application/vnd.oasis.opendocument.presentation"},
    { "ods", "application/vnd.oasis.opendocument.spreadsheet"},
    { "odt", "application/vnd.oasis.opendocument.text"},
    { "oga", "audio/ogg"},
    { "ogg", "audio/ogg"},
    { "ogv", "video/ogg"},
    { "ogx", "application/ogg"},
    { "onepkg", "application/onenote"},
    { "onetmp", "application/onenote"},
    { "onetoc2", "application/onenote"},
    { "onetoc", "application/onenote"},
    { "opf", "application/oebps-package+xml"},
    { "oprc", "application/vnd.palm"},
    { "org", "application/vnd.lotus-organizer"},
    { "osf", "application/vnd.yamaha.openscoreformat"},
    { "osfpvg", "application/vnd.yamaha.openscoreformat.osfpvg+xml"},
    { "otc", "application/vnd.oasis.opendocument.chart-template"},
    { "otf", "application/x-font-otf"},
    { "otg", "application/vnd.oasis.opendocument.graphics-template"},
    { "oth", "application/vnd.oasis.opendocument.text-web"},
    { "oti", "application/vnd.oasis.opendocument.image-template"},
    { "otm", "application/vnd.oasis.opendocument.text-master"},
    { "otp", "application/vnd.oasis.opendocument.presentation-template"},
    { "ots", "application/vnd.oasis.opendocument.spreadsheet-template"},
    { "ott", "application/vnd.oasis.opendocument.text-template"},
    { "oxt", "application/vnd.openofficeorg.extension"},
    { "p10", "application/pkcs10"},
    { "p12", "application/x-pkcs12"},
    { "p7b", "application/x-pkcs7-certificates"},
    { "p7c", "application/x-pkcs7-mime"},
    { "p7m", "application/x-pkcs7-mime"},
    { "p7r", "application/x-pkcs7-certreqresp"},
    { "p7s", "application/x-pkcs7-signature"},
    { "pas", "text/x-pascal"},
    { "pbd", "application/vnd.powerbuilder6"},
    { "pbm", "image/x-portable-bitmap"},
    { "pcf", "application/x-font-pcf"},
    { "pcl", "application/vnd.hp-pcl"},
    { "pclxl", "application/vnd.hp-pclxl"},
    { "pct", "image/x-pict"},
    { "pcurl", "application/vnd.curl.pcurl"},
    { "pcx", "image/x-pcx"},
    { "pdb", "application/vnd.palm"},
    { "pdf", "application/pdf"},
    { "pfa", "application/x-font-type1"},
    { "pfb", "application/x-font-type1"},
    { "pfm", "application/x-font-type1"},
    { "pfr", "application/font-tdpfr"},
    { "pfx", "application/x-pkcs12"},
    { "pgm", "image/x-portable-graymap"},
    { "pgn", "application/x-chess-pgn"},
    { "pgp", "application/pgp-encrypted"},
    { "pic", "image/x-pict"},
    { "pkg", "application/octet-stream"},
    { "pki", "application/pkixcmp"},
    { "pkipath", "application/pkix-pkipath"},
    { "pko", "application/ynd.ms-pkipko"},
    { "plb", "application/vnd.3gpp.pic-bw-large"},
    { "plc", "application/vnd.mobius.plc"},
    { "plf", "application/vnd.pocketlearn"},
    { "pls", "application/pls+xml"},
    { "pl", "text/plain"},
    { "pma", "application/x-perfmon"},
    { "pmc", "application/x-perfmon"},
    { "pml", "application/x-perfmon"},
    { "pmr", "application/x-perfmon"},
    { "pmw", "application/x-perfmon"},
    { "png", "image/png"},
    { "pnm", "image/x-portable-anymap"},
    { "portpkg", "application/vnd.macports.portpkg"},
    { "pot,", "application/vnd.ms-powerpoint"},
    { "pot", "application/vnd.ms-powerpoint"},
    { "potm", "application/vnd.ms-powerpoint.template.macroenabled.12"},
    { "potx", "application/vnd.openxmlformats-officedocument.presentationml.template"},
    { "ppa", "application/vnd.ms-powerpoint"},
    { "ppam", "application/vnd.ms-powerpoint.addin.macroenabled.12"},
    { "ppd", "application/vnd.cups-ppd"},
    { "ppm", "image/x-portable-pixmap"},
    { "pps", "application/vnd.ms-powerpoint"},
    { "ppsm", "application/vnd.ms-powerpoint.slideshow.macroenabled.12"},
    { "ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow"},
    { "ppt", "application/vnd.ms-powerpoint"},
    { "pptm", "application/vnd.ms-powerpoint.presentation.macroenabled.12"},
    { "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
    { "pqa", "application/vnd.palm"},
    { "prc", "application/x-mobipocket-ebook"},
    { "pre", "application/vnd.lotus-freelance"},
    { "prf", "application/pics-rules"},
    { "ps", "application/postscript"},
    { "psb", "application/vnd.3gpp.pic-bw-small"},
    { "psd", "image/vnd.adobe.photoshop"},
    { "psf", "application/x-font-linux-psf"},
    { "p", "text/x-pascal"},
    { "ptid", "application/vnd.pvi.ptid1"},
    { "pub", "application/x-mspublisher"},
    { "pvb", "application/vnd.3gpp.pic-bw-var"},
    { "pwn", "application/vnd.3m.post-it-notes"},
    { "pwz", "application/vnd.ms-powerpoint"},
    { "pya", "audio/vnd.ms-playready.media.pya"},
    { "pyc", "application/x-python-code"},
    { "pyo", "application/x-python-code"},
    { "py", "text/x-python"},
    { "pyv", "video/vnd.ms-playready.media.pyv"},
    { "qam", "application/vnd.epson.quickanime"},
    { "qbo", "application/vnd.intu.qbo"},
    { "qfx", "application/vnd.intu.qfx"},
    { "qps", "application/vnd.publishare-delta-tree"},
    { "qt", "video/quicktime"},
    { "qwd", "application/vnd.quark.quarkxpress"},
    { "qwt", "application/vnd.quark.quarkxpress"},
    { "qxb", "application/vnd.quark.quarkxpress"},
    { "qxd", "application/vnd.quark.quarkxpress"},
    { "qxl", "application/vnd.quark.quarkxpress"},
    { "qxt", "application/vnd.quark.quarkxpress"},
    { "ra", "audio/x-pn-realaudio"},
    { "ram", "audio/x-pn-realaudio"},
    { "rar", "application/x-rar-compressed"},
    { "ras", "image/x-cmu-raster"},
    { "rcprofile", "application/vnd.ipunplugged.rcprofile"},
    { "rdf", "application/rdf+xml"},
    { "rdz", "application/vnd.data-vision.rdz"},
    { "rep", "application/vnd.businessobjects"},
    { "res", "application/x-dtbresource+xml"},
    { "rgb", "image/x-rgb"},
    { "rif", "application/reginfo+xml"},
    { "rl", "application/resource-lists+xml"},
    { "rlc", "image/vnd.fujixerox.edmics-rlc"},
    { "rld", "application/resource-lists-diff+xml"},
    { "rm", "application/vnd.rn-realmedia"},
    { "rmi", "audio/midi"},
    { "rmp", "audio/x-pn-realaudio-plugin"},
    { "rms", "application/vnd.jcp.javame.midlet-rms"},
    { "rnc", "application/relax-ng-compact-syntax"},
    { "roff", "text/troff"},
    { "rpm", "application/x-rpm"},
    { "rpss", "application/vnd.nokia.radio-presets"},
    { "rpst", "application/vnd.nokia.radio-preset"},
    { "rq", "application/sparql-query"},
    { "rs", "application/rls-services+xml"},
    { "rsd", "application/rsd+xml"},
    { "rss", "application/rss+xml"},
    { "rtf", "application/rtf"},
    { "rtx", "text/richtext"},
    { "saf", "application/vnd.yamaha.smaf-audio"},
    { "sbml", "application/sbml+xml"},
    { "sc", "application/vnd.ibm.secure-container"},
    { "scd", "application/x-msschedule"},
    { "scm", "application/vnd.lotus-screencam"},
    { "scq", "application/scvp-cv-request"},
    { "scs", "application/scvp-cv-response"},
    { "sct", "text/scriptlet"},
    { "scurl", "text/vnd.curl.scurl"},
    { "sda", "application/vnd.stardivision.draw"},
    { "sdc", "application/vnd.stardivision.calc"},
    { "sdd", "application/vnd.stardivision.impress"},
    { "sdkd", "application/vnd.solent.sdkm+xml"},
    { "sdkm", "application/vnd.solent.sdkm+xml"},
    { "sdp", "application/sdp"},
    { "sdw", "application/vnd.stardivision.writer"},
    { "see", "application/vnd.seemail"},
    { "seed", "application/vnd.fdsn.seed"},
    { "sema", "application/vnd.sema"},
    { "semd", "application/vnd.semd"},
    { "semf", "application/vnd.semf"},
    { "ser", "application/java-serialized-object"},
    { "setpay", "application/set-payment-initiation"},
    { "setreg", "application/set-registration-initiation"},
    { "sfd-hdstx", "application/vnd.hydrostatix.sof-data"},
    { "sfs", "application/vnd.spotfire.sfs"},
    { "sgl", "application/vnd.stardivision.writer-global"},
    { "sgml", "text/sgml"},
    { "sgm", "text/sgml"},
    { "sh", "application/x-sh"},
    { "shar", "application/x-shar"},
    { "shf", "application/shf+xml"},
    { "sic", "application/vnd.wap.sic"},
    { "sig", "application/pgp-signature"},
    { "silo", "model/mesh"},
    { "sis", "application/vnd.symbian.install"},
    { "sisx", "application/vnd.symbian.install"},
    { "sit", "application/x-stuffit"},
    { "si", "text/vnd.wap.si"},
    { "sitx", "application/x-stuffitx"},
    { "skd", "application/vnd.koan"},
    { "skm", "application/vnd.koan"},
    { "skp", "application/vnd.koan"},
    { "skt", "application/vnd.koan"},
    { "slc", "application/vnd.wap.slc"},
    { "sldm", "application/vnd.ms-powerpoint.slide.macroenabled.12"},
    { "sldx", "application/vnd.openxmlformats-officedocument.presentationml.slide"},
    { "slt", "application/vnd.epson.salt"},
    { "sl", "text/vnd.wap.sl"},
    { "smf", "application/vnd.stardivision.math"},
    { "smi", "application/smil+xml"},
    { "smil", "application/smil+xml"},
    { "snd", "audio/basic"},
    { "snf", "application/x-font-snf"},
    { "so", "application/octet-stream"},
    { "spc", "application/x-pkcs7-certificates"},
    { "spf", "application/vnd.yamaha.smaf-phrase"},
    { "spl", "application/x-futuresplash"},
    { "spot", "text/vnd.in3d.spot"},
    { "spp", "application/scvp-vp-response"},
    { "spq", "application/scvp-vp-request"},
    { "spx", "audio/ogg"},
    { "src", "application/x-wais-source"},
    { "srx", "application/sparql-results+xml"},
    { "sse", "application/vnd.kodak-descriptor"},
    { "ssf", "application/vnd.epson.ssf"},
    { "ssml", "application/ssml+xml"},
    { "sst", "application/vnd.ms-pkicertstore"},
    { "stc", "application/vnd.sun.xml.calc.template"},
    { "std", "application/vnd.sun.xml.draw.template"},
    { "s", "text/x-asm"},
    { "stf", "application/vnd.wt.stf"},
    { "sti", "application/vnd.sun.xml.impress.template"},
    { "stk", "application/hyperstudio"},
    { "stl", "application/vnd.ms-pki.stl"},
    { "stm", "text/html"},
    { "str", "application/vnd.pg.format"},
    { "stw", "application/vnd.sun.xml.writer.template"},
    { "sus", "application/vnd.sus-calendar"},
    { "susp", "application/vnd.sus-calendar"},
    { "sv4cpio", "application/x-sv4cpio"},
    { "sv4crc", "application/x-sv4crc"},
    { "svd", "application/vnd.svd"},
    { "svg", "image/svg+xml"},
    { "svgz", "image/svg+xml"},
    { "swa", "application/x-director"},
    { "swf", "application/x-shockwave-flash"},
    { "swi", "application/vnd.arastra.swi"},
    { "sxc", "application/vnd.sun.xml.calc"},
    { "sxd", "application/vnd.sun.xml.draw"},
    { "sxg", "application/vnd.sun.xml.writer.global"},
    { "sxi", "application/vnd.sun.xml.impress"},
    { "sxm", "application/vnd.sun.xml.math"},
    { "sxw", "application/vnd.sun.xml.writer"},
    { "tao", "application/vnd.tao.intent-module-archive"},
    { "t", "application/x-troff"},
    { "tar", "application/x-tar"},
    { "tcap", "application/vnd.3gpp2.tcap"},
    { "tcl", "application/x-tcl"},
    { "teacher", "application/vnd.smart.teacher"},
    { "tex", "application/x-tex"},
    { "texi", "application/x-texinfo"},
    { "texinfo", "application/x-texinfo"},
    { "text", "text/plain"},
    { "tfm", "application/x-tex-tfm"},
    { "tgz", "application/x-gzip"},
    { "tiff", "image/tiff"},
    { "tif", "image/tiff"},
    { "tmo", "application/vnd.tmobile-livetv"},
    { "torrent", "application/x-bittorrent"},
    { "tpl", "application/vnd.groove-tool-template"},
    { "tpt", "application/vnd.trid.tpt"},
    { "tra", "application/vnd.trueapp"},
    { "trm", "application/x-msterminal"},
    { "tr", "text/troff"},
    { "tsv", "text/tab-separated-values"},
    { "ttc", "application/x-font-ttf"},
    { "ttf", "application/x-font-ttf"},
    { "twd", "application/vnd.simtech-mindmapper"},
    { "twds", "application/vnd.simtech-mindmapper"},
    { "txd", "application/vnd.genomatix.tuxedo"},
    { "txf", "application/vnd.mobius.txf"},
    { "txt", "text/plain"},
    { "u32", "application/x-authorware-bin"},
    { "udeb", "application/x-debian-package"},
    { "ufd", "application/vnd.ufdl"},
    { "ufdl", "application/vnd.ufdl"},
    { "uls", "text/iuls"},
    { "umj", "application/vnd.umajin"},
    { "unityweb", "application/vnd.unity"},
    { "uoml", "application/vnd.uoml+xml"},
    { "uris", "text/uri-list"},
    { "uri", "text/uri-list"},
    { "urls", "text/uri-list"},
    { "ustar", "application/x-ustar"},
    { "utz", "application/vnd.uiq.theme"},
    { "uu", "text/x-uuencode"},
    { "vcd", "application/x-cdlink"},
    { "vcf", "text/x-vcard"},
    { "vcg", "application/vnd.groove-vcard"},
    { "vcs", "text/x-vcalendar"},
    { "vcx", "application/vnd.vcx"},
    { "vis", "application/vnd.visionary"},
    { "viv", "video/vnd.vivo"},
    { "vor", "application/vnd.stardivision.writer"},
    { "vox", "application/x-authorware-bin"},
    { "vrml", "x-world/x-vrml"},
    { "vsd", "application/vnd.visio"},
    { "vsf", "application/vnd.vsf"},
    { "vss", "application/vnd.visio"},
    { "vst", "application/vnd.visio"},
    { "vsw", "application/vnd.visio"},
    { "vtu", "model/vnd.vtu"},
    { "vxml", "application/voicexml+xml"},
    { "w3d", "application/x-director"},
    { "wad", "application/x-doom"},
    { "wav", "audio/x-wav"},
    { "wax", "audio/x-ms-wax"},
    { "wbmp", "image/vnd.wap.wbmp"},
    { "wbs", "application/vnd.criticaltools.wbs+xml"},
    { "wbxml", "application/vnd.wap.wbxml"},
    { "wcm", "application/vnd.ms-works"},
    { "wdb", "application/vnd.ms-works"},
    { "wiz", "application/msword"},
    { "wks", "application/vnd.ms-works"},
    { "wma", "audio/x-ms-wma"},
    { "wmd", "application/x-ms-wmd"},
    { "wmf", "application/x-msmetafile"},
    { "wmlc", "application/vnd.wap.wmlc"},
    { "wmlsc", "application/vnd.wap.wmlscriptc"},
    { "wmls", "text/vnd.wap.wmlscript"},
    { "wml", "text/vnd.wap.wml"},
    { "wm", "video/x-ms-wm"},
    { "wmv", "video/x-ms-wmv"},
    { "wmx", "video/x-ms-wmx"},
    { "wmz", "application/x-ms-wmz"},
    { "wpd", "application/vnd.wordperfect"},
    { "wpl", "application/vnd.ms-wpl"},
    { "wps", "application/vnd.ms-works"},
    { "wqd", "application/vnd.wqd"},
    { "wri", "application/x-mswrite"},
    { "wrl", "x-world/x-vrml"},
    { "wrz", "x-world/x-vrml"},
    { "wsdl", "application/wsdl+xml"},
    { "wspolicy", "application/wspolicy+xml"},
    { "wtb", "application/vnd.webturbo"},
    { "wvx", "video/x-ms-wvx"},
    { "x32", "application/x-authorware-bin"},
    { "x3d", "application/vnd.hzn-3d-crossword"},
    { "xaf", "x-world/x-vrml"},
    { "xap", "application/x-silverlight-app"},
    { "xar", "application/vnd.xara"},
    { "xbap", "application/x-ms-xbap"},
    { "xbd", "application/vnd.fujixerox.docuworks.binder"},
    { "xbm", "image/x-xbitmap"},
    { "xdm", "application/vnd.syncml.dm+xml"},
    { "xdp", "application/vnd.adobe.xdp+xml"},
    { "xdw", "application/vnd.fujixerox.docuworks"},
    { "xenc", "application/xenc+xml"},
    { "xer", "application/patch-ops-error+xml"},
    { "xfdf", "application/vnd.adobe.xfdf"},
    { "xfdl", "application/vnd.xfdl"},
    { "xht", "application/xhtml+xml"},
    { "xhtml", "application/xhtml+xml"},
    { "xhvml", "application/xv+xml"},
    { "xif", "image/vnd.xiff"},
    { "xla", "application/vnd.ms-excel"},
    { "xlam", "application/vnd.ms-excel.addin.macroenabled.12"},
    { "xlb", "application/vnd.ms-excel"},
    { "xlc", "application/vnd.ms-excel"},
    { "xlm", "application/vnd.ms-excel"},
    { "xls", "application/vnd.ms-excel"},
    { "xlsb", "application/vnd.ms-excel.sheet.binary.macroenabled.12"},
    { "xlsm", "application/vnd.ms-excel.sheet.macroenabled.12"},
    { "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
    { "xlt", "application/vnd.ms-excel"},
    { "xltm", "application/vnd.ms-excel.template.macroenabled.12"},
    { "xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template"},
    { "xlw", "application/vnd.ms-excel"},
    { "xml", "application/xml"},
    { "xo", "application/vnd.olpc-sugar"},
    { "xof", "x-world/x-vrml"},
    { "xop", "application/xop+xml"},
    { "xpdl", "application/xml"},
    { "xpi", "application/x-xpinstall"},
    { "xpm", "image/x-xpixmap"},
    { "xpr", "application/vnd.is-xpr"},
    { "xps", "application/vnd.ms-xpsdocument"},
    { "xpw", "application/vnd.intercon.formnet"},
    { "xpx", "application/vnd.intercon.formnet"},
    { "xsl", "application/xml"},
    { "xslt", "application/xslt+xml"},
    { "xsm", "application/vnd.syncml+xml"},
    { "xspf", "application/xspf+xml"},
    { "xul", "application/vnd.mozilla.xul+xml"},
    { "xvm", "application/xv+xml"},
    { "xvml", "application/xv+xml"},
    { "xwd", "image/x-xwindowdump"},
    { "xyz", "chemical/x-xyz"},
    { "z", "application/x-compress"},
    { "zaz", "application/vnd.zzazz.deck+xml"},
    { "zip", "application/zip"},
    { "zir", "application/vnd.zul"},
    { "zirz", "application/vnd.zul"},
    { "zmm", "application/vnd.handheld-entertainment+xml"}
    };

    /**
     * Table for MIME type replacements.
     *
     * Table format: wrong type, correct type
     */
    private static final String[][] MIME_TYPE_REPLACEMENT_MAP = new String[][] {
        {"image/jpg", "image/jpeg"},
        {"image/pjpeg", "image/jpeg"},   // see issue 1712
        {"application/x-zip-compressed", "application/zip"} // see issue 3791
    };

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

    public static String unfold(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("\r|\n", "");
    }

    public static String decode(String s) {
        return decode(s, null);
    }

    public static String decode(String s, Message message) {
        if (s == null) {
            return null;
        }

        return DecoderUtil.decodeEncodedWords(s, message);
    }

    public static String unfoldAndDecode(String s) {
        return unfoldAndDecode(s, null);
    }

    public static String unfoldAndDecode(String s, Message message) {
        return decode(unfold(s), message);
    }

    // TODO implement proper foldAndEncode
    public static String foldAndEncode(String s) {
        return s;
    }

    /**
     * Returns the named parameter of a header field. If name is null the first
     * parameter is returned, or if there are no additional parameters in the
     * field the entire field is returned. Otherwise the named parameter is
     * searched for in a case insensitive fashion and returned. If the parameter
     * cannot be found the method returns null.
     *
     * @param header
     * @param name
     * @return
     */
    public static String getHeaderParameter(String header, String name) {
        if (header == null) {
            return null;
        }
        header = header.replaceAll("\r|\n", "");
        String[] parts = header.split(";");
        if (name == null && parts.length > 0) {
            return parts[0].trim();
        }
        for (String part : parts) {
            if (part.trim().toLowerCase(Locale.US).startsWith(name.toLowerCase(Locale.US))) {
                String[] partParts = part.split("=", 2);
                if (partParts.length == 2) {
                    String parameter = partParts[1].trim();
                    int len = parameter.length();
                    if (len >= 2 && parameter.startsWith("\"") && parameter.endsWith("\"")) {
                        return parameter.substring(1, len - 1);
                    } else {
                        return parameter;
                    }
                }
            }
        }
        return null;
    }

    public static Part findFirstPartByMimeType(Part part, String mimeType)
    throws MessagingException {
        if (part.getBody() instanceof Multipart) {
            Multipart multipart = (Multipart)part.getBody();
            for (int i = 0, count = multipart.getCount(); i < count; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                Part ret = findFirstPartByMimeType(bodyPart, mimeType);
                if (ret != null) {
                    return ret;
                }
            }
        } else if (part.getMimeType().equalsIgnoreCase(mimeType)) {
            return part;
        }
        return null;
    }

    public static Part findPartByContentId(Part part, String contentId) throws Exception {
        if (part.getBody() instanceof Multipart) {
            Multipart multipart = (Multipart)part.getBody();
            for (int i = 0, count = multipart.getCount(); i < count; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                Part ret = findPartByContentId(bodyPart, contentId);
                if (ret != null) {
                    return ret;
                }
            }
        }
        String[] header = part.getHeader(MimeHeader.HEADER_CONTENT_ID);
        if (header != null) {
            for (String s : header) {
                if (s.equals(contentId)) {
                    return part;
                }
            }
        }
        return null;
    }

    /**
      * Reads the Part's body and returns a String based on any charset conversion that needed
      * to be done.  Note, this <b>does not</b> return a text representation of HTML.
      * @param part The part containing a body
      * @return a String containing the converted text in the body, or null if there was no text
      * or an error during conversion.
      */
    public static String getTextFromPart(Part part) {
        try {
            if ((part != null) && (part.getBody() != null)) {
                final Body body = part.getBody();
                if (body instanceof TextBody) {
                    return ((TextBody)body).getText();
                }

                final String mimeType = part.getMimeType();
                if ((mimeType != null) && MimeUtility.mimeTypeMatches(mimeType, "text/*")) {
                    /*
                     * We've got a text part, so let's see if it needs to be processed further.
                     */
                    String charset = getHeaderParameter(part.getContentType(), "charset");
                    /*
                     * determine the charset from HTML message.
                     */
                    if (mimeType.equalsIgnoreCase("text/html") && charset == null) {
                        InputStream in = part.getBody().getInputStream();
                        try {
                            byte[] buf = new byte[256];
                            in.read(buf, 0, buf.length);
                            String str = new String(buf, "US-ASCII");

                            if (str.length() == 0) {
                                return "";
                            }
                            Pattern p = Pattern.compile("<meta http-equiv=\"?Content-Type\"? content=\"text/html; charset=(.+?)\">", Pattern.CASE_INSENSITIVE);
                            Matcher m = p.matcher(str);
                            if (m.find()) {
                                charset = m.group(1);
                            }
                        } finally {
                            try {
                                if (in instanceof BinaryTempFileBodyInputStream) {
                                    /*
                                     * If this is a BinaryTempFileBodyInputStream, calling close()
                                     * will delete the file. But we can't let that happen because
                                     * the file needs to be opened again by the code a few lines
                                     * down.
                                     */
                                    ((BinaryTempFileBodyInputStream) in).closeWithoutDeleting();
                                } else {
                                    in.close();
                                }
                            } catch (Exception e) { /* ignore */ }
                        }
                    }
                    charset = fixupCharset(charset, getMessageFromPart(part));

                    /*
                     * Now we read the part into a buffer for further processing. Because
                     * the stream is now wrapped we'll remove any transfer encoding at this point.
                     */
                    InputStream in = part.getBody().getInputStream();
                    try {
                        String text = readToString(in, charset);

                        // Replace the body with a TextBody that already contains the decoded text
                        part.setBody(new TextBody(text));

                        return text;
                    } finally {
                        try {
                            /*
                             * This time we don't care if it's a BinaryTempFileBodyInputStream. We
                             * replaced the body with a TextBody instance and hence don't need the
                             * file anymore.
                             */
                            in.close();
                        } catch (IOException e) { /* Ignore */ }
                    }
                }
            }

        } catch (OutOfMemoryError oom) {
            /*
             * If we are not able to process the body there's nothing we can do about it. Return
             * null and let the upper layers handle the missing content.
             */
            Log.e(K9.LOG_TAG, "Unable to getTextFromPart " + oom.toString());
        } catch (Exception e) {
            /*
             * If we are not able to process the body there's nothing we can do about it. Return
             * null and let the upper layers handle the missing content.
             */
            Log.e(K9.LOG_TAG, "Unable to getTextFromPart", e);
        }
        return null;
    }

    /**
     * Returns true if the given mimeType matches the matchAgainst specification.
     * @param mimeType A MIME type to check.
     * @param matchAgainst A MIME type to check against. May include wildcards such as image/* or
     * * /*.
     * @return
     */
    public static boolean mimeTypeMatches(String mimeType, String matchAgainst) {
        Pattern p = Pattern.compile(matchAgainst.replaceAll("\\*", "\\.\\*"), Pattern.CASE_INSENSITIVE);
        return p.matcher(mimeType).matches();
    }

    /**
     * Returns true if the given mimeType matches any of the matchAgainst specifications.
     * @param mimeType A MIME type to check.
     * @param matchAgainst An array of MIME types to check against. May include wildcards such
     * as image/* or * /*.
     * @return
     */
    public static boolean mimeTypeMatches(String mimeType, String[] matchAgainst) {
        for (String matchType : matchAgainst) {
            if (mimeTypeMatches(mimeType, matchType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes any content transfer encoding from the stream and returns a Body.
     */
    public static Body decodeBody(InputStream in, String contentTransferEncoding)
    throws IOException {
        /*
         * We'll remove any transfer encoding by wrapping the stream.
         */
        if (contentTransferEncoding != null) {
            contentTransferEncoding =
                MimeUtility.getHeaderParameter(contentTransferEncoding, null);
            if ("quoted-printable".equalsIgnoreCase(contentTransferEncoding)) {
                in = new QuotedPrintableInputStream(in);
            } else if ("base64".equalsIgnoreCase(contentTransferEncoding)) {
                in = new Base64InputStream(in);
            }
        }

        BinaryTempFileBody tempBody = new BinaryTempFileBody();
        OutputStream out = tempBody.getOutputStream();
        try {
            IOUtils.copy(in, out);
        } finally {
            out.close();
        }
        return tempBody;
    }


    /**
     * Empty base class for the class hierarchy used by
     * {@link MimeUtility#extractTextAndAttachments(Context, Message)}.
     *
     * @see Text
     * @see Html
     * @see MessageHeader
     * @see Alternative
     */
    static abstract class Viewable { /* empty */ }

    /**
     * Class representing textual parts of a message that aren't marked as attachments.
     *
     * @see MimeUtility#isPartTextualBody(Part)
     */
    static abstract class Textual extends Viewable {
        private Part mPart;

        public Textual(Part part) {
            mPart = part;
        }

        public Part getPart() {
            return mPart;
        }
    }

    /**
     * Class representing a {@code text/plain} part of a message.
     */
    static class Text extends Textual {
        public Text(Part part) {
            super(part);
        }
    }

    /**
     * Class representing a {@code text/html} part of a message.
     */
    static class Html extends Textual {
        public Html(Part part) {
            super(part);
        }
    }

    /**
     * Class representing a {@code message/rfc822} part of a message.
     *
     * <p>
     * This is used to extract basic header information when the message contents are displayed
     * inline.
     * </p>
     */
    static class MessageHeader extends Viewable {
        private Part mContainerPart;
        private Message mMessage;

        public MessageHeader(Part containerPart, Message message) {
            mContainerPart = containerPart;
            mMessage = message;
        }

        public Part getContainerPart() {
            return mContainerPart;
        }

        public Message getMessage() {
            return mMessage;
        }
    }

    /**
     * Class representing a {@code multipart/alternative} part of a message.
     *
     * <p>
     * Only relevant {@code text/plain} and {@code text/html} children are stored in this container
     * class.
     * </p>
     */
    static class Alternative extends Viewable {
        private List<Viewable> mText;
        private List<Viewable> mHtml;

        public Alternative(List<Viewable> text, List<Viewable> html) {
            mText = text;
            mHtml = html;
        }

        public List<Viewable> getText() {
            return mText;
        }

        public List<Viewable> getHtml() {
            return mHtml;
        }
    }

    /**
     * Store viewable text of a message as plain text and HTML, and the parts considered
     * attachments.
     *
     * @see MimeUtility#extractTextAndAttachments(Context, Message)
     */
    public static class ViewableContainer {
        /**
         * The viewable text of the message in plain text.
         */
        public final String text;

        /**
         * The viewable text of the message in HTML.
         */
        public final String html;

        /**
         * The parts of the message considered attachments (everything not viewable).
         */
        public final List<Part> attachments;

        ViewableContainer(String text, String html, List<Part> attachments) {
            this.text = text;
            this.html = html;
            this.attachments = attachments;
        }
    }

    /**
     * Collect attachment parts of a message.
     *
     * @param message
     *         The message to collect the attachment parts from.
     *
     * @return A list of parts regarded as attachments.
     *
     * @throws MessagingException
     *          In case of an error.
     */
    public static List<Part> collectAttachments(Message message)
            throws MessagingException {
        try {
            List<Part> attachments = new ArrayList<Part>();
            getViewables(message, attachments);

            return attachments;
        } catch (Exception e) {
            throw new MessagingException("Couldn't collect attachment parts", e);
        }
    }

    /**
     * Collect the viewable textual parts of a message.
     *
     * @param message
     *         The message to extract the viewable parts from.
     *
     * @return A set of viewable parts of the message.
     *
     * @throws MessagingException
     *          In case of an error.
     */
    public static Set<Part> collectTextParts(Message message)
            throws MessagingException {
        try {
            List<Part> attachments = new ArrayList<Part>();

            // Collect all viewable parts
            List<Viewable> viewables = getViewables(message, attachments);

            // Extract the Part references
            return getParts(viewables);
        } catch (Exception e) {
            throw new MessagingException("Couldn't extract viewable parts", e);
        }
    }

    /**
     * Extract the viewable textual parts of a message and return the rest as attachments.
     *
     * @param context
     *         A {@link Context} instance that will be used to get localized strings.
     * @param message
     *         The message to extract the text and attachments from.
     *
     * @return A {@link ViewableContainer} instance containing the textual parts of the message as
     *         plain text and HTML, and a list of message parts considered attachments.
     *
     * @throws MessagingException
     *          In case of an error.
     */
    public static ViewableContainer extractTextAndAttachments(Context context, Message message)
            throws MessagingException {
        try {
            List<Part> attachments = new ArrayList<Part>();

            // Collect all viewable parts
            List<Viewable> viewables = getViewables(message, attachments);

            /*
             * Convert the tree of viewable parts into text and HTML
             */

            // Used to suppress the divider for the first viewable part
            boolean hideDivider = true;

            StringBuilder text = new StringBuilder();
            StringBuilder html = new StringBuilder();

            for (Viewable viewable : viewables) {
                if (viewable instanceof Textual) {
                    // This is either a text/plain or text/html part. Fill the variables 'text' and
                    // 'html', converting between plain text and HTML as necessary.
                    text.append(buildText(viewable, !hideDivider));
                    html.append(buildHtml(viewable, !hideDivider));
                    hideDivider = false;
                } else if (viewable instanceof MessageHeader) {
                    MessageHeader header = (MessageHeader) viewable;
                    Part containerPart = header.getContainerPart();
                    Message innerMessage =  header.getMessage();

                    addTextDivider(text, containerPart, !hideDivider);
                    addMessageHeaderText(context, text, innerMessage);

                    addHtmlDivider(html, containerPart, !hideDivider);
                    addMessageHeaderHtml(context, html, innerMessage);

                    hideDivider = true;
                } else if (viewable instanceof Alternative) {
                    // Handle multipart/alternative contents
                    Alternative alternative = (Alternative) viewable;

                    /*
                     * We made sure at least one of text/plain or text/html is present when
                     * creating the Alternative object. If one part is not present we convert the
                     * other one to make sure 'text' and 'html' always contain the same text.
                     */
                    List<Viewable> textAlternative = alternative.getText().isEmpty() ?
                            alternative.getHtml() : alternative.getText();
                    List<Viewable> htmlAlternative = alternative.getHtml().isEmpty() ?
                            alternative.getText() : alternative.getHtml();

                    // Fill the 'text' variable
                    boolean divider = !hideDivider;
                    for (Viewable textViewable : textAlternative) {
                        text.append(buildText(textViewable, divider));
                        divider = true;
                    }

                    // Fill the 'html' variable
                    divider = !hideDivider;
                    for (Viewable htmlViewable : htmlAlternative) {
                        html.append(buildHtml(htmlViewable, divider));
                        divider = true;
                    }
                    hideDivider = false;
                }
            }

            return new ViewableContainer(text.toString(), html.toString(), attachments);
        } catch (Exception e) {
            throw new MessagingException("Couldn't extract viewable parts", e);
        }
    }

    /**
     * Traverse the MIME tree of a message an extract viewable parts.
     *
     * @param part
     *         The message part to start from.
     * @param attachments
     *         A list that will receive the parts that are considered attachments.
     *
     * @return A list of {@link Viewable}s.
     *
     * @throws MessagingException
     *          In case of an error.
     */
    public static List<Viewable> getViewables(Part part, List<Part> attachments) throws MessagingException {
        List<Viewable> viewables = new ArrayList<Viewable>();

        Body body = part.getBody();
        if (body instanceof Multipart) {
            Multipart multipart = (Multipart) body;
            if (part.getMimeType().equalsIgnoreCase("multipart/alternative")) {
                /*
                 * For multipart/alternative parts we try to find a text/plain and a text/html
                 * child. Everything else we find is put into 'attachments'.
                 */
                List<Viewable> text = findTextPart(multipart, true);

                Set<Part> knownTextParts = getParts(text);
                List<Viewable> html = findHtmlPart(multipart, knownTextParts, attachments, true);

                if (!text.isEmpty() || !html.isEmpty()) {
                    Alternative alternative = new Alternative(text, html);
                    viewables.add(alternative);
                }
            } else {
                // For all other multipart parts we recurse to grab all viewable children.
                int childCount = multipart.getCount();
                for (int i = 0; i < childCount; i++) {
                    Part bodyPart = multipart.getBodyPart(i);
                    viewables.addAll(getViewables(bodyPart, attachments));
                }
            }
        } else if (body instanceof Message &&
                !("attachment".equalsIgnoreCase(getContentDisposition(part)))) {
            /*
             * We only care about message/rfc822 parts whose Content-Disposition header has a value
             * other than "attachment".
             */
            Message message = (Message) body;

            // We add the Message object so we can extract the filename later.
            viewables.add(new MessageHeader(part, message));

            // Recurse to grab all viewable parts and attachments from that message.
            viewables.addAll(getViewables(message, attachments));
        } else if (isPartTextualBody(part)) {
            /*
             * Save text/plain and text/html
             */
            String mimeType = part.getMimeType();
            if (mimeType.equalsIgnoreCase("text/plain")) {
                Text text = new Text(part);
                viewables.add(text);
            } else {
                Html html = new Html(part);
                viewables.add(html);
            }
        } else {
            // Everything else is treated as attachment.
            attachments.add(part);
        }

        return viewables;
    }

    /**
     * Search the children of a {@link Multipart} for {@code text/plain} parts.
     *
     * @param multipart
     *         The {@code Multipart} to search through.
     * @param directChild
     *         If {@code true}, this method will return after the first {@code text/plain} was
     *         found.
     *
     * @return A list of {@link Text} viewables.
     *
     * @throws MessagingException
     *          In case of an error.
     */
    private static List<Viewable> findTextPart(Multipart multipart, boolean directChild)
            throws MessagingException {
        List<Viewable> viewables = new ArrayList<Viewable>();

        int childCount = multipart.getCount();
        for (int i = 0; i < childCount; i++) {
            Part part = multipart.getBodyPart(i);
            Body body = part.getBody();
            if (body instanceof Multipart) {
                Multipart innerMultipart = (Multipart) body;

                /*
                 * Recurse to find text parts. Since this is a multipart that is a child of a
                 * multipart/alternative we don't want to stop after the first text/plain part
                 * we find. This will allow to get all text parts for constructions like this:
                 *
                 * 1. multipart/alternative
                 * 1.1. multipart/mixed
                 * 1.1.1. text/plain
                 * 1.1.2. text/plain
                 * 1.2. text/html
                 */
                List<Viewable> textViewables = findTextPart(innerMultipart, false);

                if (!textViewables.isEmpty()) {
                    viewables.addAll(textViewables);
                    if (directChild) {
                        break;
                    }
                }
            } else if (isPartTextualBody(part) && part.getMimeType().equalsIgnoreCase("text/plain")) {
                Text text = new Text(part);
                viewables.add(text);
                if (directChild) {
                    break;
                }
            }
        }

        return viewables;
    }

    /**
     * Search the children of a {@link Multipart} for {@code text/html} parts.
     *
     * <p>
     * Every part that is not a {@code text/html} we want to display, we add to 'attachments'.
     * </p>
     *
     * @param multipart
     *         The {@code Multipart} to search through.
     * @param knownTextParts
     *         A set of {@code text/plain} parts that shouldn't be added to 'attachments'.
     * @param attachments
     *         A list that will receive the parts that are considered attachments.
     * @param directChild
     *         If {@code true}, this method will add all {@code text/html} parts except the first
     *         found to 'attachments'.
     *
     * @return A list of {@link Text} viewables.
     *
     * @throws MessagingException
     *          In case of an error.
     */
    private static List<Viewable> findHtmlPart(Multipart multipart, Set<Part> knownTextParts,
            List<Part> attachments, boolean directChild) throws MessagingException {
        List<Viewable> viewables = new ArrayList<Viewable>();

        boolean partFound = false;
        int childCount = multipart.getCount();
        for (int i = 0; i < childCount; i++) {
            Part part = multipart.getBodyPart(i);
            Body body = part.getBody();
            if (body instanceof Multipart) {
                Multipart innerMultipart = (Multipart) body;

                if (directChild && partFound) {
                    // We already found our text/html part. Now we're only looking for attachments.
                    findAttachments(innerMultipart, knownTextParts, attachments);
                } else {
                    /*
                     * Recurse to find HTML parts. Since this is a multipart that is a child of a
                     * multipart/alternative we don't want to stop after the first text/html part
                     * we find. This will allow to get all text parts for constructions like this:
                     *
                     * 1. multipart/alternative
                     * 1.1. text/plain
                     * 1.2. multipart/mixed
                     * 1.2.1. text/html
                     * 1.2.2. text/html
                     * 1.3. image/jpeg
                     */
                    List<Viewable> htmlViewables = findHtmlPart(innerMultipart, knownTextParts,
                            attachments, false);

                    if (!htmlViewables.isEmpty()) {
                        partFound = true;
                        viewables.addAll(htmlViewables);
                    }
                }
            } else if (!(directChild && partFound) && isPartTextualBody(part) &&
                    part.getMimeType().equalsIgnoreCase("text/html")) {
                Html html = new Html(part);
                viewables.add(html);
                partFound = true;
            } else if (!knownTextParts.contains(part)) {
                // Only add this part as attachment if it's not a viewable text/plain part found
                // earlier.
                attachments.add(part);
            }
        }

        return viewables;
    }

    /**
     * Build a set of message parts for fast lookups.
     *
     * @param viewables
     *         A list of {@link Viewable}s containing references to the message parts to include in
     *         the set.
     *
     * @return The set of viewable {@code Part}s.
     *
     * @see MimeUtility#findHtmlPart(Multipart, Set, List, boolean)
     * @see MimeUtility#findAttachments(Multipart, Set, List)
     */
    private static Set<Part> getParts(List<Viewable> viewables) {
        Set<Part> parts = new HashSet<Part>();

        for (Viewable viewable : viewables) {
            if (viewable instanceof Textual) {
                parts.add(((Textual) viewable).getPart());
            } else if (viewable instanceof Alternative) {
                Alternative alternative = (Alternative) viewable;
                parts.addAll(getParts(alternative.getText()));
                parts.addAll(getParts(alternative.getHtml()));
            }
        }

        return parts;
    }

    /**
     * Traverse the MIME tree and add everything that's not a known text part to 'attachments'.
     *
     * @param multipart
     *         The {@link Multipart} to start from.
     * @param knownTextParts
     *         A set of known text parts we don't want to end up in 'attachments'.
     * @param attachments
     *         A list that will receive the parts that are considered attachments.
     */
    private static void findAttachments(Multipart multipart, Set<Part> knownTextParts,
            List<Part> attachments) {
        int childCount = multipart.getCount();
        for (int i = 0; i < childCount; i++) {
            Part part = multipart.getBodyPart(i);
            Body body = part.getBody();
            if (body instanceof Multipart) {
                Multipart innerMultipart = (Multipart) body;
                findAttachments(innerMultipart, knownTextParts, attachments);
            } else if (!knownTextParts.contains(part)) {
                attachments.add(part);
            }
        }
    }

    /**
     * Extract important header values from a message to display inline (plain text version).
     *
     * @param context
     *         A {@link Context} instance that will be used to get localized strings.
     * @param text
     *         The {@link StringBuilder} that will receive the (plain text) output.
     * @param message
     *         The message to extract the header values from.
     *
     * @throws MessagingException
     *          In case of an error.
     */
    private static void addMessageHeaderText(Context context, StringBuilder text, Message message)
            throws MessagingException {
        // From: <sender>
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            text.append(context.getString(R.string.message_compose_quote_header_from));
            text.append(' ');
            text.append(Address.toString(from));
            text.append("\n");
        }

        // To: <recipients>
        Address[] to = message.getRecipients(RecipientType.TO);
        if (to != null && to.length > 0) {
            text.append(context.getString(R.string.message_compose_quote_header_to));
            text.append(' ');
            text.append(Address.toString(to));
            text.append("\n");
        }

        // Cc: <recipients>
        Address[] cc = message.getRecipients(RecipientType.CC);
        if (cc != null && cc.length > 0) {
            text.append(context.getString(R.string.message_compose_quote_header_cc));
            text.append(' ');
            text.append(Address.toString(cc));
            text.append("\n");
        }

        // Date: <date>
        Date date = message.getSentDate();
        if (date != null) {
            text.append(context.getString(R.string.message_compose_quote_header_send_date));
            text.append(' ');
            text.append(date.toString());
            text.append("\n");
        }

        // Subject: <subject>
        String subject = message.getSubject();
        text.append(context.getString(R.string.message_compose_quote_header_subject));
        text.append(' ');
        if (subject == null) {
            text.append(context.getString(R.string.general_no_subject));
        } else {
            text.append(subject);
        }
        text.append("\n\n");
    }

    /**
     * Extract important header values from a message to display inline (HTML version).
     *
     * @param context
     *         A {@link Context} instance that will be used to get localized strings.
     * @param html
     *         The {@link StringBuilder} that will receive the (HTML) output.
     * @param message
     *         The message to extract the header values from.
     *
     * @throws MessagingException
     *          In case of an error.
     */
    private static void addMessageHeaderHtml(Context context, StringBuilder html, Message message)
            throws MessagingException {

        html.append("<table style=\"border: 0\">");

        // From: <sender>
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            addTableRow(html, context.getString(R.string.message_compose_quote_header_from),
                    Address.toString(from));
        }

        // To: <recipients>
        Address[] to = message.getRecipients(RecipientType.TO);
        if (to != null && to.length > 0) {
            addTableRow(html, context.getString(R.string.message_compose_quote_header_to),
                    Address.toString(to));
        }

        // Cc: <recipients>
        Address[] cc = message.getRecipients(RecipientType.CC);
        if (cc != null && cc.length > 0) {
            addTableRow(html, context.getString(R.string.message_compose_quote_header_cc),
                    Address.toString(cc));
        }

        // Date: <date>
        Date date = message.getSentDate();
        if (date != null) {
            addTableRow(html, context.getString(R.string.message_compose_quote_header_send_date),
                    date.toString());
        }

        // Subject: <subject>
        String subject = message.getSubject();
        addTableRow(html, context.getString(R.string.message_compose_quote_header_subject),
                (subject == null) ? context.getString(R.string.general_no_subject) : subject);

        html.append("</table>");
    }

    /**
     * Output an HTML table two column row with some hardcoded style.
     *
     * @param html
     *         The {@link StringBuilder} that will receive the output.
     * @param header
     *         The string to be put in the {@code TH} element.
     * @param value
     *         The string to be put in the {@code TD} element.
     */
    private static void addTableRow(StringBuilder html, String header, String value) {
        html.append("<tr><th style=\"text-align: left; vertical-align: top;\">");
        html.append(header);
        html.append("</th>");
        html.append("<td>");
        html.append(value);
        html.append("</td></tr>");
    }

    /**
     * Use the contents of a {@link Viewable} to create the plain text to be displayed.
     *
     * <p>
     * This will use {@link HtmlConverter#htmlToText(String)} to convert HTML parts to plain text
     * if necessary.
     * </p>
     *
     * @param viewable
     *         The viewable part to build the text from.
     * @param prependDivider
     *         {@code true}, if the text divider should be inserted as first element.
     *         {@code false}, otherwise.
     *
     * @return The contents of the supplied viewable instance as plain text.
     */
    private static StringBuilder buildText(Viewable viewable, boolean prependDivider)
    {
        StringBuilder text = new StringBuilder();
        if (viewable instanceof Textual) {
            Part part = ((Textual)viewable).getPart();
            addTextDivider(text, part, prependDivider);

            String t = getTextFromPart(part);
            if (t == null) {
                t = "";
            } else if (viewable instanceof Html) {
                t = HtmlConverter.htmlToText(t);
            }
            text.append(t);
        } else if (viewable instanceof Alternative) {
            // That's odd - an Alternative as child of an Alternative; go ahead and try to use the
            // text/plain child; fall-back to the text/html part.
            Alternative alternative = (Alternative) viewable;

            List<Viewable> textAlternative = alternative.getText().isEmpty() ?
                    alternative.getHtml() : alternative.getText();

            boolean divider = prependDivider;
            for (Viewable textViewable : textAlternative) {
                text.append(buildText(textViewable, divider));
                divider = true;
            }
        }

        return text;
    }

    /*
     * Some constants that are used by addTextDivider() below.
     */
    private static final int TEXT_DIVIDER_LENGTH = TEXT_DIVIDER.length();
    private static final String FILENAME_PREFIX = "----- ";
    private static final int FILENAME_PREFIX_LENGTH = FILENAME_PREFIX.length();
    private static final String FILENAME_SUFFIX = " ";
    private static final int FILENAME_SUFFIX_LENGTH = FILENAME_SUFFIX.length();

    /**
     * Add a plain text divider between two plain text message parts.
     *
     * @param text
     *         The {@link StringBuilder} to append the divider to.
     * @param part
     *         The message part that will follow after the divider. This is used to extract the
     *         part's name.
     * @param prependDivider
     *         {@code true}, if the divider should be appended. {@code false}, otherwise.
     */
    private static void addTextDivider(StringBuilder text, Part part, boolean prependDivider) {
        if (prependDivider) {
            String filename = getPartName(part);

            text.append("\n\n");
            int len = filename.length();
            if (len > 0) {
                if (len > TEXT_DIVIDER_LENGTH - FILENAME_PREFIX_LENGTH - FILENAME_SUFFIX_LENGTH) {
                    filename = filename.substring(0, TEXT_DIVIDER_LENGTH - FILENAME_PREFIX_LENGTH -
                            FILENAME_SUFFIX_LENGTH - 3) + "...";
                }
                text.append(FILENAME_PREFIX);
                text.append(filename);
                text.append(FILENAME_SUFFIX);
                text.append(TEXT_DIVIDER.substring(0, TEXT_DIVIDER_LENGTH -
                        FILENAME_PREFIX_LENGTH - filename.length() - FILENAME_SUFFIX_LENGTH));
            } else {
                text.append(TEXT_DIVIDER);
            }
            text.append("\n\n");
        }
    }

    /**
     * Use the contents of a {@link Viewable} to create the HTML to be displayed.
     *
     * <p>
     * This will use {@link HtmlConverter#textToHtml(String)} to convert plain text parts
     * to HTML if necessary.
     * </p>
     *
     * @param viewable
     *         The viewable part to build the HTML from.
     * @param prependDivider
     *         {@code true}, if the HTML divider should be inserted as first element.
     *         {@code false}, otherwise.
     *
     * @return The contents of the supplied viewable instance as HTML.
     */
    private static StringBuilder buildHtml(Viewable viewable, boolean prependDivider)
    {
        StringBuilder html = new StringBuilder();
        if (viewable instanceof Textual) {
            Part part = ((Textual)viewable).getPart();
            addHtmlDivider(html, part, prependDivider);

            String t = getTextFromPart(part);
            if (t == null) {
                t = "";
            } else if (viewable instanceof Text) {
                t = HtmlConverter.textToHtml(t);
            }
            html.append(t);
        } else if (viewable instanceof Alternative) {
            // That's odd - an Alternative as child of an Alternative; go ahead and try to use the
            // text/html child; fall-back to the text/plain part.
            Alternative alternative = (Alternative) viewable;

            List<Viewable> htmlAlternative = alternative.getHtml().isEmpty() ?
                    alternative.getText() : alternative.getHtml();

            boolean divider = prependDivider;
            for (Viewable htmlViewable : htmlAlternative) {
                html.append(buildHtml(htmlViewable, divider));
                divider = true;
            }
        }

        return html;
    }

    /**
     * Add an HTML divider between two HTML message parts.
     *
     * @param html
     *         The {@link StringBuilder} to append the divider to.
     * @param part
     *         The message part that will follow after the divider. This is used to extract the
     *         part's name.
     * @param prependDivider
     *         {@code true}, if the divider should be appended. {@code false}, otherwise.
     */
    private static void addHtmlDivider(StringBuilder html, Part part, boolean prependDivider) {
        if (prependDivider) {
            String filename = getPartName(part);

            html.append("<p style=\"margin-top: 2.5em; margin-bottom: 1em; border-bottom: 1px solid #000\">");
            html.append(filename);
            html.append("</p>");
        }
    }

    /**
     * Get the name of the message part.
     *
     * @param part
     *         The part to get the name for.
     *
     * @return The (file)name of the part if available. An empty string, otherwise.
     */
    private static String getPartName(Part part) {
        try {
            String disposition = part.getDisposition();
            if (disposition != null) {
                String name = MimeUtility.getHeaderParameter(disposition, "filename");
                return (name == null) ? "" : name;
            }
        }
        catch (MessagingException e) { /* ignore */ }

        return "";
    }

    /**
     * Get the value of the {@code Content-Disposition} header.
     *
     * @param part
     *         The message part to read the header from.
     *
     * @return The value of the {@code Content-Disposition} header if available. {@code null},
     *         otherwise.
     */
    private static String getContentDisposition(Part part) {
        try {
            String disposition = part.getDisposition();
            if (disposition != null) {
                return MimeUtility.getHeaderParameter(disposition, null);
            }
        }
        catch (MessagingException e) { /* ignore */ }

        return null;
    }

    public static Boolean isPartTextualBody(Part part) throws MessagingException {
        String disposition = part.getDisposition();
        String dispositionType = null;
        String dispositionFilename = null;
        if (disposition != null) {
            dispositionType = MimeUtility.getHeaderParameter(disposition, null);
            dispositionFilename = MimeUtility.getHeaderParameter(disposition, "filename");
        }

        /*
         * A best guess that this part is intended to be an attachment and not inline.
         */
        boolean attachment = ("attachment".equalsIgnoreCase(dispositionType) || (dispositionFilename != null));

        if ((!attachment) && (part.getMimeType().equalsIgnoreCase("text/html"))) {
            return true;
        }
        /*
         * If the part is plain text and it got this far it's part of a
         * mixed (et al) and should be rendered inline.
         */
        else if ((!attachment) && (part.getMimeType().equalsIgnoreCase("text/plain"))) {
            return true;
        }
        /*
         * Finally, if it's nothing else we will include it as an attachment.
         */
        else {
            return false;
        }
    }

    public static String getCharsetFromAddress(String address) {
        String variant = getJisVariantFromAddress(address);
        if (variant != null) {
            String charset = "x-" + variant + "-shift_jis-2007";
            if (Charset.isSupported(charset))
                return charset;
        }

        return "UTF-8";
    }

    public static String getMimeTypeByExtension(String filename) {
        String returnedType = null;
        String extension = null;

        if (filename != null && filename.lastIndexOf('.') != -1) {
            extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.US);
            returnedType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        // If the MIME type set by the user's mailer is application/octet-stream, try to figure
        // out whether there's a sane file type extension.
        if (returnedType != null && !DEFAULT_ATTACHMENT_MIME_TYPE.equalsIgnoreCase(returnedType)) {
            return returnedType;
        } else if (extension != null) {
            for (String[] contentTypeMapEntry : MIME_TYPE_BY_EXTENSION_MAP) {
                if (contentTypeMapEntry[0].equals(extension)) {
                    return contentTypeMapEntry[1];
                }
            }
        }

        return DEFAULT_ATTACHMENT_MIME_TYPE;
    }

    public static String getExtensionByMimeType(String mimeType) {
        String lowerCaseMimeType = mimeType.toLowerCase(Locale.US);
        for (String[] contentTypeMapEntry : MIME_TYPE_BY_EXTENSION_MAP) {
            if (contentTypeMapEntry[1].equals(lowerCaseMimeType)) {
                return contentTypeMapEntry[0];
            }
        }

        return null;
    }

    /**
     * Convert some wrong MIME types encountered in the wild to canonical MIME types.
     *
     * @param mimeType
     *         The original MIME type
     *
     * @return If {@code mimeType} is known to be wrong the correct MIME type is returned.
     *         Otherwise the lower case version of {@code mimeType} is returned.
     *
     * @see #MIME_TYPE_REPLACEMENT_MAP
     */
    public static String canonicalizeMimeType(String mimeType) {
        String lowerCaseMimeType = mimeType.toLowerCase(Locale.US);
        for (String[] mimeTypeMapEntry : MIME_TYPE_REPLACEMENT_MAP) {
            if (mimeTypeMapEntry[0].equals(lowerCaseMimeType)) {
                return mimeTypeMapEntry[1];
            }
        }
        return lowerCaseMimeType;
    }

    /**
     * When viewing the attachment we want the MIME type to be as sensible as possible. So we fix
     * it up if necessary.
     *
     * @param mimeType
     *         The original MIME type of the attachment.
     * @param name
     *         The (file)name of the attachment.
     *
     * @return The best MIME type we can come up with.
     */
    public static String getMimeTypeForViewing(String mimeType, String name) {
        if (DEFAULT_ATTACHMENT_MIME_TYPE.equalsIgnoreCase(mimeType)) {
            // If the MIME type is the generic "application/octet-stream"
            // we try to find a better one by looking at the file extension.
            return getMimeTypeByExtension(name);
        }

        // Some messages contain wrong MIME types. See if we know better.
        return canonicalizeMimeType(mimeType);
    }

    private static Message getMessageFromPart(Part part) {
        while (part != null) {
            if (part instanceof Message)
                return (Message)part;

            if (!(part instanceof BodyPart))
                return null;

            Multipart multipart = ((BodyPart)part).getParent();
            if (multipart == null)
                return null;

            part = multipart.getParent();
        }
        return null;
    }

    public static String fixupCharset(String charset, Message message) throws MessagingException {
        if (charset == null || "0".equals(charset))
            charset = "US-ASCII";  // No encoding, so use us-ascii, which is the standard.

        charset = charset.toLowerCase(Locale.US);
        if (charset.equals("cp932"))
            charset = "shift_jis";

        if (charset.equals("shift_jis") || charset.equals("iso-2022-jp")) {
            String variant = getJisVariantFromMessage(message);
            if (variant != null)
                charset = "x-" + variant + "-" + charset + "-2007";
        }
        return charset;
    }

    private static String getJisVariantFromMessage(Message message) throws MessagingException {
        if (message == null)
            return null;

        // If a receiver is known to use a JIS variant, the sender transfers the message after converting the
        // charset as a convention.
        String variant = getJisVariantFromReceivedHeaders(message);
        if (variant != null)
            return variant;

        // If a receiver is not known to use any JIS variants, the sender transfers the message without converting
        // the charset.
        variant = getJisVariantFromFromHeaders(message);
        if (variant != null)
            return variant;

        return getJisVariantFromMailerHeaders(message);
    }

    private static String getJisVariantFromReceivedHeaders(Message message) throws MessagingException {
        String receivedHeaders[] = message.getHeader("Received");
        if (receivedHeaders == null)
            return null;

        for (String receivedHeader : receivedHeaders) {
            String address = getAddressFromReceivedHeader(receivedHeader);
            if (address == null)
                continue;
            String variant = getJisVariantFromAddress(address);
            if (variant != null)
                return variant;
        }
        return null;
    }

    private static String getAddressFromReceivedHeader(String receivedHeader) {
        // Not implemented yet!  Extract an address from the FOR clause of the given Received header.
        return null;
    }

    private static String getJisVariantFromFromHeaders(Message message) throws MessagingException {
        Address addresses[] = message.getFrom();
        if (addresses == null || addresses.length == 0)
            return null;

        return getJisVariantFromAddress(addresses[0].getAddress());
    }

    private static String getJisVariantFromAddress(String address) {
        if (address == null)
            return null;
        if (isInDomain(address, "docomo.ne.jp") || isInDomain(address, "dwmail.jp") ||
            isInDomain(address, "pdx.ne.jp") || isInDomain(address, "willcom.com") ||
            isInDomain(address, "emnet.ne.jp") || isInDomain(address, "emobile.ne.jp"))
            return "docomo";
        else if (isInDomain(address, "softbank.ne.jp") || isInDomain(address, "vodafone.ne.jp") ||
                 isInDomain(address, "disney.ne.jp") || isInDomain(address, "vertuclub.ne.jp"))
            return "softbank";
        else if (isInDomain(address, "ezweb.ne.jp") || isInDomain(address, "ido.ne.jp"))
            return "kddi";
        return null;
    }

    private static boolean isInDomain(String address, String domain) {
        int index = address.length() - domain.length() - 1;
        if (index < 0)
            return false;

        char c = address.charAt(index);
        if (c != '@' && c != '.')
            return false;

        return address.endsWith(domain);
    }

    private static String getJisVariantFromMailerHeaders(Message message) throws MessagingException {
        String mailerHeaders[] = message.getHeader("X-Mailer");
        if (mailerHeaders == null || mailerHeaders.length == 0)
            return null;

        if (mailerHeaders[0].startsWith("iPhone Mail ") || mailerHeaders[0].startsWith("iPad Mail "))
            return "iphone";

        return null;
    }

    public static String readToString(InputStream in, String charset) throws IOException {
        boolean isIphoneString = false;

        // iso-2022-jp variants are supported by no versions as of Dec 2010.
        if (charset.length() > 19 && charset.startsWith("x-") &&
                charset.endsWith("-iso-2022-jp-2007") && !Charset.isSupported(charset)) {
            in = new Iso2022JpToShiftJisInputStream(in);
            charset = "x-" + charset.substring(2, charset.length() - 17) + "-shift_jis-2007";
        }

        // shift_jis variants are supported by Eclair and later.
        if (charset.length() > 17 && charset.startsWith("x-") &&
                charset.endsWith("-shift_jis-2007") && !Charset.isSupported(charset)) {
            // If the JIS variant is iPhone, map the Unicode private use area in iPhone to the one in Android after
            // converting the character set from the standard Shift JIS to Unicode.
            if (charset.substring(2, charset.length() - 15).equals("iphone"))
                isIphoneString = true;

            charset = "shift_jis";
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
                Log.e(K9.LOG_TAG, "I don't know how to deal with the charset " + charset +
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

    static private String importStringFromIphone(String str) {
        StringBuilder buff = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i = str.offsetByCodePoints(i, 1)) {
            int codePoint = str.codePointAt(i);
            buff.appendCodePoint(importCodePointFromIphone(codePoint));
        }
        return buff.toString();
    }

    static private int importCodePointFromIphone(int codePoint) {
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

    public static void setCharset(String charset, Part part) throws MessagingException {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                       part.getMimeType() + ";\n charset=" + getExternalCharset(charset));
    }

    public static String getExternalCharset(String charset) {
        if (charset.length() > 17 && charset.startsWith("x-") &&
                charset.endsWith("-shift_jis-2007"))
            return "shift_jis";

        return charset;
    }

    public static ViewableContainer extractPartsFromDraft(Message message)
            throws MessagingException {

        Body body = message.getBody();
        if (message.isMimeType("multipart/mixed") && body instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) body;

            ViewableContainer container;
            int count = multipart.getCount();
            if (count >= 1) {
                // The first part is either a text/plain or a multipart/alternative
                BodyPart firstPart = multipart.getBodyPart(0);
                container = extractTextual(firstPart);

                // The rest should be attachments
                for (int i = 1; i < count; i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    container.attachments.add(bodyPart);
                }
            } else {
                container = new ViewableContainer("", "", new ArrayList<Part>());
            }

            return container;
        }

        return extractTextual(message);
    }

    private static ViewableContainer extractTextual(Part part) throws MessagingException {
        String text = "";
        String html = "";
        List<Part> attachments = new ArrayList<Part>();

        Body firstBody = part.getBody();
        if (part.isMimeType("text/plain")) {
            String bodyText = getTextFromPart(part);
            if (bodyText != null) {
                text = fixDraftTextBody(bodyText);
                html = HtmlConverter.textToHtml(text);
            }
        } else if (part.isMimeType("multipart/alternative") &&
                firstBody instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) firstBody;
            for (int i = 0, count = multipart.getCount(); i < count; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String bodyText = getTextFromPart(bodyPart);
                if (bodyText != null) {
                    if (text.length() == 0 && bodyPart.isMimeType("text/plain")) {
                        text = fixDraftTextBody(bodyText);
                    } else if (html.length() == 0 && bodyPart.isMimeType("text/html")) {
                        html = fixDraftTextBody(bodyText);
                    }
                }
            }
        }

        return new ViewableContainer(text, html, attachments);
    }

    /**
     * Fix line endings of text bodies in draft messages.
     *
     * <p>
     * We create drafts with LF line endings. The values in the identity header are based on that.
     * So we replace CRLF with LF when loading messages (from the server).
     * </p>
     *
     * @param text
     *         The body text with CRLF line endings
     *
     * @return The text with LF line endings
     */
    private static String fixDraftTextBody(String text) {
        return text.replace("\r\n", "\n");
    }
}
