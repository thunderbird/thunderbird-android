package com.fsck.k9.mail.filter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FixedLengthInputStreamTest {
    private FixedLengthInputStream fixedLengthInputStream;
    private final int LENGTH_LIMIT1 = 5000;
    private final int LENGTH_LIMIT2 = 20000;
    private int STR_LENGTH;
    private String string;

    @Before
    public void setUp() {
        // the length of this is 10000
        string = "MEnASSKHcqghDICuZtxZPtVHuIFHNtNFBFBfrDWJhnzVoPdZuNXvuXgPAhOJLKEoGkzWiMlCZKKMJfbWcwzgSWEzHlIpSxFoMALb" +
                "bYEmStiGGKBhiIPfQikVsOnBkdfXuMJVOmMYeIBrpMMhExSndzYQcbczqCnhFJanfnTbsyFrIYLdpEcyYQBzirKRYrWvBqzjJJXN" +
                "mgEWthlQjdUvaxrhmKcsyQyMxTUNOgGBhWjyGQwtsxsLzcSHvtJbXXYHYDsnHEFPDRVpTtHdbahaoKFgZPLYiiNOmYqxzNcXmJTQ" +
                "AbPjqeTumnrStJcWmnexWhouoyaVwVnGmiGpIvAyuHNomOaPUTxxfYeoGfGWCxjGiEorNQpCESRxzrGrFlsWQzSIIVBFPSLHZwhz" +
                "nGLzFPszsoPWHAfMUpsqcqqCFeWyOlfkFElGXiKUQeaAWibIFczowqJThbqEmOZdAugggzlJwnbRzEVRtkKCSoMyppiMsTGYqbZV" +
                "YClrRYAHlNBSMeoSIrYZEQRSXEDiLVkcHiZGRuVfoReelRhIlyvHnVymjweJlawyZtWMICAWkuJYwcawePzBwcMzvEwBJBqFSdPw" +
                "DmHBseRrCgOksxqUycbuhKivOsMcSyNjuuoBjrSImAzlPYDxhEgdAUmzWmHDoYqdAiZOGyPYhnmvHxrierMSEpfurztHLvzANzXV" +
                "fjjjuRCcJqXKLzhOUlHFPGzSyuBfoyzhbmJnAZQegdOdleIpTgSXnMWBWeYtGoAzYrEODFZyaMUQnNowDJmvACKxKTlSaBZQHeVY" +
                "HlQRbPaFnvjojRQvHkKbQkMOZRyolgjpXsFxPjcCdIgJLfeksfjPZRnZdKaFgKwUWfAueJzoifKFPOwfkZASFdizVBqkpwUBxGPy" +
                "QesNflOdjsIGaUIBuQpmhlPRceSqonXVgXYQRHgmBitxUxxiURWYkyEyXYvvhjvHnJSatxSxkXIruANXWdeUgolZgCpojffSUFFM" +
                "dIkbQCVtIfVngnWVlKdOTinrlUTxKbDleupGPZxVDiGzzyrVvTiLqCReRfKFWBxAZfhChFJfZIAMxsKoSiSmBJKlcvwhahbyQaIp" +
                "sIjlPIgtzNopKGBpKQWryLSWEZaUJvBNtllFGVkgcEJAbueLbTzodPehMLdUrkYPEvhElsPfuAgnrlDfcVwZTfCZIfgzrdPyCUGa" +
                "hkItuezhSjbrqCFtaNFwSZwNEOnLgWCcipcVmVgfMyyuMihorOPSlLEBnxDAFlRAOtqhKSOWZmZPxVbLLekDKRVtUorNWEhYypsf" +
                "lRlVwyHpHCetlnnsHwbAJFMDBTefCOyPycOmppTjamRIAjbCMAGunjfCYQAjECinPwuDICyOJZRCfIXTcFiFmStMSgrGDcYnwiTL" +
                "RiTHGVwXbZpqbGTCEQsSvOwlraLXbUwDbJaFMDHczpxcVwpdxWGKgkNnTpmIWnOGyeksbQRAfTjNpiEjFCMCvuKOTkYYacmFTbKo" +
                "ERENhuJRggCsFhpVetmjtsahBgniNTdmxYTLueVAExBDpPhgByuuYcMHEzDfZpLwDNeIOYQDNDNdPDzncmcmMcHEhBzCjURitfgW" +
                "YmBsDQjJBkqYidbDPooOgNJQmIjrrFGKvRkUKyVoIuiBqBuJNsqRRsWlYxRQNmJPHsAMguXEbVsOtYZaQecxcZSTVLzKiBwCkJDL" +
                "VkFUvgxbBokTxBgjEdfWkXkBXHMkNnRnzzOCXGtIVQLHtmEUwKMWXitJQNVHjFJCHUfGURHleMIwzagiHvKypgSMjlfqUjSTfQmo" +
                "SerrwofvbLJadMlbnVztXwyuDZspaHXXWzrChlbTCtAHkitdthqcrvpvgrpZVXfTUAbRtxuioyRSvAIrsIGeExbwUjbnVPWSRcTk" +
                "AXejsXDFtCdIncQzVqteeiWuzyDCwCvNNvYJKtRLtmmKjUHXDSEqHhmcSVOcvLwHLpuLLzbwHohGyYgWuYcIKlePRWgsjYKZezbW" +
                "hDiwfQsjTNWQwlmqzYPvlDMAgynZwbUHHwDMHCSTIzBhgGrpHQCsveSaVBJbRGsRaMjDjBLOQjliftemKkrIjIQbZXZLZvnxsDYO" +
                "rYWprbmUZgPXjgpzZdqBjglWOGmqMzkinwcjiHyRqIOcuXYVMtomYKRsQgohmxqvGVySGMFQiqbcsbUVKrFBoecpHYkuQqKyWaXB" +
                "fQYKLLGNwPetStfyXfJPebQNwILaLozOfYLJOgnwDjjGbAswTbOwvzZypLlRFgEevkMBrMvPIiauIojiXBUfVfyWZLwCSDPDCCdL" +
                "ROpGaSyjURqmZaaBAKhbScEtpIFRIlPcaDUlLQTpHZTZnwsPomWvaYNmkPVzgGBsPxAMEAyjaSZZgpPRbjEpyKGNWlruXFGyaFlE" +
                "KWkVEfMYtuRZHHIbOiSgkCOQUhXqaZYbhQIGUeoityHcysGGOISZwdUSLqDrlcLTpLHUKBnueOpahtRjwAFKQFDQWMhfRePSJMUT" +
                "IvHILbbqdFsWqimlZTjIkoqLuUeRHMouAXpnmvBqdBwLRbHcEyqHtzqplYJMhCaPfdufjzeavpNYiwpPEVcSQMFeOFuDhCzklbRy" +
                "zMPksdjlKJPZGEaSCPvNFYnpmGZDVuaTwNOrcHeQpDlcCprdBQzdRjSfFcFebacFQAIctgzgevkygxdYoHwCFBysTPATTLBUCvdb" +
                "oekFPjTWxHxrdyLpnrwOcXLoYfmODfnHsfFKRSQgIhMDNjFJJTGBKYKtipsGqfLtmEXBvyUHYVcEMAAsyjFPJbKngNfndUBhpjke" +
                "STRSPrREYqXTtbdjWLgxkZBVqWVJZyRxSWXurobRUnmZYumanQtDFxPPqdVBODenqUjXuFKFLrTvxGfoKMnXJpdBwdlMwOXmzIsa" +
                "rxpjoOzWsiZzlyODfOZazqucbzXawmZtCMZYjgyUlYNXlLNQzVlDSVMCntkERTJgzZpyjEHSLvmlUvmRwbmCSilEEbWCcbDZHlyM" +
                "qrLDInIJdjZfgKvZRoqcRgpaAUkfCpuBGcPuOGizsTaMyzVtYvSkmXEsnSzndWfQLJQGFVKimmMzrrMjwemgZJXxyDwoANwBeuzU" +
                "QWiuRkMWVhmtHYRuQpnuZJcGnHsdZlwiGZJKPTMUQwttInsIAqGcGjNGExwhUSjUELhsiuhHiIAixqAKDXVjnpizvSBuLjaFoGEG" +
                "yIYWcRmAxeQhjuTYQBUIJmPrinVkSpaLUSDeFXsXOweKxuMJyiUxGAXZvJcKMnVCfaLUTYMiRVnxsmKdnaLOdOgyxfZGKBWhSPcB" +
                "cKTBFvLbgigzwaxqIbqefiboMLNxKTkQbEeLfecxVeeDjvZkDyZRtQnyKdvTEeLNNWjbDVuMGjDrRDWmDhKOCaMfePYYtQXAKDYn" +
                "RhbfQgOxtqsizhaTBgPpbwpUGaODnERDUAxxoeUdKqWQOmtiqxcwFmkoEoxCMoJiCNnJWNcXBuNTEBRYNyiPpiSuilHtafArZvJI" +
                "qAyDCMbntwqLOTWrVAOBSeGCsRYJNQsmTYUTdMKOyhYwrwcepDDOGmKTwljUjuvJYAPjgMaMovGNCfKLvLtOMBCmtcxEeNaHwyVJ" +
                "KKREyJsvJCtJFSDsWiRgdcTFNJBiQKHPuquiVvZpxsTiyXzaGbZRwaLSrVpArJfQOnoZcJnbfYokGBHjxEgQrzdVnNFTIQXqdlkO" +
                "TQcedkDBICqshQebZcCgPpcxymbeHpbvPcpZbmxsyavwhdUbVmDgKQcfvzXtVDLQLFpmIlkbHEgqodlstRRcdnSkIFmlPLFeDNWP" +
                "qZAifWfYPFeDpvHvmXtpMTPuHdsVQvRarrKRiuNgereFLTZqWQspOBRzssosjOSaVrCJtDXgmiLqfpOiaiabkFcTwoRdYiITVeeM" +
                "nzQOyMiGFBdJaRLyyWDWnBYaZdroHFyfXaNKICBssZWrcwMnCVJmgZqZlgMyTAhNpFcJOJfzDwpHvFpSoHjIHwsgjLtAmoqSWfZO" +
                "DKLlZxQcKZdLgbKmKREGKAVWqpxVRrROpPuStNKNeUCftKkWMaIOvYTbjTuJNULXfAENRwhWllaFDeBrpfkSFwWNqJmQIUFoskFI" +
                "RGrkQlPzTWJBVnAIOriDIGIpwMAILqCDEKWplOMGYyEnXsdXqfwQXlknvztgdfUYvObbuGKgvrLlBgEGMoSMdsxGSjpcViPRaTJf" +
                "TqJvxrQToJGYNWDfMfefpQSTWxMLwVZUWzpNUKLffAympuVbpWGZLFqJhRlwZBMmaUEyuVjuguBHmrUUxDIaxjQQKlTgvHvShzHB" +
                "pfdGjsTGhUjeBLxocaKoQtmIoPMqpWEXYVdvrkkTgsSBsbRZZSccDnCSwgtyqGPdqMnlwvzYlBTZMSsGxORBjldFYfrYdjGsTKXf" +
                "jiLZrwRxyARdOumsImJbgNgBIMXuRyIkKbTNUEfItxjBGpwjCvQMZsNdQIdSKAjpUlPNgJLrWstNKQnqlKNvULiQvcjUwAuOCzvF" +
                "WZcMuPlpYQNXpjMJkkhRvVVmOUtLNlUnOoXKfsNvPJkGpkjOihYmctLGtNCmeSfOCWPAHPZaUqPXFddJeyXDpkXYgxAJjaDHbJph" +
                "StttbevAbcXhSnJLdELkNnBiCkQrpEoCiLVagxQqvjQfRMxQXnnGECwfaVuvszxqMFzKsDbQnUYnTeELEvSHcSmbXNcHtIVRbZnj" +
                "WQwklyWYTZnWhnNslDnjkCFIAKAtVwLZRGbGGQmmBsXaTMCUcRSxSbpMFOLUhUnzFBoAmMbHGKBNBbYxsEdAUWjLCKxkTFaJAlBK" +
                "HzhSocGGExHzMotlcVRQpHcpYhQuvFnLFfvYRpYbruFlemYVjzBUXlHdgawtZPxOrRDowTztLxUNWbHUbdxwWOFlYkNabmORHoxk" +
                "TTKVaTojoLIHzvtEvozlkEaDPUIvMbwjfdDeLsUKPoupCZhtKMfcgtTghvMArOchYSZqpilxwYokZpLqqhUNBoihkklOqPrbUvYL" +
                "VRobifzyuRZdbnSQMkDOrutEeEMBhezKXfSRKMxDcCfiCTmPefTeEFuRXJoJYSMaplnrizLVbQpngsNxypFPifMnAHYOVJHtpxZB" +
                "gbfdgDyjgDuNoYLqiFnmzCJbRJheQaiEHsGKgwnELTQUEQdKvtcmxnnLHwpKsawIhNYQXHpOPIpErffLtYBtPUclPzObvILoZHal" +
                "eMCKrmDheDRBsYZdHfMANrtMbhYKiwhQJsHUKEMxYMwZHxhzrnZyShLuHSRaNgdOtIZMiHClsSOTleWQxGLWQqFNiKeZeBcZeQhN" +
                "rIiPQGuPSckzzdyljxtLuhjEwwaTplMwLzOnctrPfWPYRcwTJHCSMAjOdZPRJsdCaMXDrIVpJQuzuKmSkmDUgihoirxVeYsofcQh" +
                "uuRSmYxsFbDWsxMASiDqtqLiyGaPKesqllKoYfBWmTjIsgaxzFRLXZZkFAzerhMGvLQhsQoxlbHJqaORYjbtVUSgBlfdYZTQSlKV" +
                "ztwnQpgEybjhYHtaYAlSPuHtIBbaPgHyLUmzyzLAwWBpLVYOLAYWOiIYFJcaiJekroOoXBHcdfAChYutOXYSWYzZoKbtRplyYlox" +
                "VVoRoJDNQfNoVOXmRtVOgErrvMgSsYoLuXnSrOCgQNqaBLEwkynXfxMHxGewGMZtIVtRssYgfpPGeQzcIiSUxlykVqbDSgHjGVQi" +
                "KmlNedCfmOCFtcyVreqckXoqzzvKHeLaKFDwJhHkpFLdbNXFOLDCBYIURdTVoPqOingIcaYltldOZDhSgbiCCUKByOiZqdfgJOkH" +
                "QtReGsSzjDBeeBBnJoSqnrAQVrnGgYgXYMrksizAAblCYefrlEtnPXisxvTeKUQbGNjkNZibGqIYJlufYrfvpDePYaiyyGovawrK" +
                "oYlrwRPjlSHciHaqBGOIPiyCstxqpJUkOUWIJznOeQcCPzIEugfxJbDoyokuveyxpXErFviWtwRlZsbniUylfaLMElQtMHiDbxVA" +
                "arSAXgOUyGkoDtRRGNyNLdbePsIlApQYRpNLAbijePGtwVLmYhtJcQvRRKFEXfoUzLeQwHgjUvAnFlOTaQVVXnXPZidSPmGBQNMt" +
                "ZKLlNfaViScSIxozhaDfGZnYFSypFJwLpyMyQchrDroxkrYqENHnwzzjaKcnVUIRWOuBJOiMAscWBeIFSyDjlJOLYelGRdXFREiT" +
                "DegZQxUwIDZyXmlwcqaEpNNFyglwbYkICOhdyBsHBYXVJaeCGKfOveltqpYJcAElKcknJoFPDESseLlHgQWYJqCqRPLPgINVDIaF" +
                "dDJSPhKeuLVTvbUgSyACIIeNrxQryWZEdqLBPIWHuvFXrNFKeLaLbYASbvYtRQEzdPMNOkCxgxbrgERVfcEHSiBpzKhQtqiVbTSO" +
                "hGtksTjPTOAELACjTkdAuOUibPSQrHDhaVZMCbQYyXLoThAvETgIvILTyYhNtQnmdsdLrCZZVeHNZHslwsObIFugSkFIAylExwKI" +
                "exskGIUBlxgHIHLeqZqNEIEFGpSgslfMKDYhZLoLfIIrgviaPBGdZKSOMduRNPWqMvSYlpiYgzyZdNhHoaurBWkTYwoIazIEjQuA" +
                "boetzxGzVYGnqvOJRsieqStvUjzRSitgWKBRFMwJsedRbQIjJirhFNVnBnIEqseCCYwqNrBxbttEvcqmhbIWaiOwBZpDPDQSeCtO" +
                "oMMDBgtUPvkHDfYepGfsgvbHLThtGJlnNthWiOMKtMsfAPZMmgiElZAInVTWVQpANhhsbMByHPXvDRxHPiFeuaKNhFeHrCsLKNjA" +
                "rqJeGGRzFEjrknOzazIJhSzAgTSZeSVRrTLcBqEaNmpqAhdJIanxUAxclGDYohrMrfBgnUEmkwsdJWzyzzqMDwbUzCUSsoEFehxO" +
                "jLFcciTIbsLOfVwJIFitjvqnxijsjvNjFjyNnEkLwewGhUBBITCQiOwKPEGTqdwJfqqKNTSPCzQRufpbGkuJTRdLzNmLRxaonqLk" +
                "GowVbroezSkpegEAskXhbMJTCCYydMavlqvmFdMLQqUSxVURjLMDIDEXWFMstyTIxFeOJLsYYIzAUJXflPhtGNDlCkhAgqQSXGGf" +
                "uIgVYqKJiKIoGjgefXBuPNSxQCHMuippkwQxXjqjtMEXkGPYYhQLETPrEzywBxVTxvDtQvkJkOpeStceOYUtdBWuymCakvvCFYPo" +
                "MbBISEHSbuFkfzNSZdppEytfUcMhUZpENOfIuUpHAMFYMfBVWsjMxkNAgBpvHCmnTAGTXVueKUFeKXyKrtqVztKjCjyGoarZHTwR" +
                "FSwiLLvMkHulmrAGXSPVdCoiaJoxmsppYfwbwhDAoLGnzfUnqtNyCbyUHpyUItEjfqrLizMTcFeEeioiLbdObCiWXBSfqRzZidBZ" +
                "XvhsuvdNyBNherItFJQnPfyzCpNEQraKccegNMWWmPXEvUphaKDDsEsbuZYmLUFWhBupUolODQDYJoxaleAJOcfoBQAMQfqqINYp" +
                "FbZIoTLAuYySuCrXdFzvWFMiobUXyCPIaIzzFIxnIovovFQRzMHxEFlkPIoLwnuqnFQnRRgmlMHUssBdBDapvnUuhifmoCmuaBsP" +
                "IaaTTnajqsIsXpDyGQNQISxfSsczuDtZGeXEJQTRUVeeCviweEghQyKZmpmHzBYTKtUlSaxNooMprukQSFqMeUgwyUgTsMqSmkzW" +
                "DQzbFjzUJDVlfKNYWISOxPiAIrRvweUFbMRYHJvjskZwemcdoKBiCBwIqOpYrVwrEthvdXgijCtONcTlicKGtYORRhZEJiUIPTSY" +
                "MEzQrUETaHqkknZFwBKQJJwgZaceaXFmEBLKBoCzcTXayTIEOlGngzfzvkSrDmoOcZroyQSyukxxbJdlaEzDEDJOaSIszGkHeomY" +
                "KipWsMrKHIYpXaqvstsBlaFRhHyvuwtCdYsjWBtVPDNwTtOuPYklKIHAkvoDceSqonrwWtORQrcZKpiNKzFLqbWgfmnhCcKCeXng" +
                "uexijkkVShZXIvcYgZdljYyIOQCrlLtHcGMZNJFhCcWJqcriZLRZctrrESxxKZWkGeVcKLFMTHcMwidbiAchSpjQBvCwMXpAlpSt" +
                "seWUSbdfGQqdadWczKVrEYaiDrhNeepPFDllynREhXUBspdOsRPkVMVaNbcmJEwQkoMeWlsfvRpdAuBZYTzfKVDKQGDoDVDhJyeA" +
                "BnzjGEEoiGrOiLGoViHpiLCBOLBiLCtfxSWKYuKuixEAJOBiMZFbHGBVDJmUNoeXfxXKNabolkDTCwcPohDrsvvinNCHDSOYnfXp" +
                "olFRYTClufMizjJMzgBYApgrhOFZpAmmJaTadOgIVVafWvEFKOefUILvQNLaxdWuDVmzOxsSIZiWvYWbpfpIqEMgmfaUpHVdxHOu" +
                "xbYPqezLbQPjXwULNznEyHYpyCpAFOawJpMmHfluqsSNMzNVlyafSPLDywjvEMIqcYWSmDdcfUQMHbzkECgkrYJuinAWYUPjoJRZ" +
                "QkxcfkTlhhoFnmbZyvLSrIVcNYdLIDUsqCLQAXEJIJVegUJPfzssOmWHDAPZyLtylUUgFyxvwxLFgAnehydWWXnXIVmnqZEeXeAh" +
                "cANnXpHqMcAtNXgmBkELyqrYemyPsnlvWMNFJsjJcYNoaAwFiAKVxebtwLLvodQFtuJxasxTcQvsiwSRjWanjWnMDoJKoQtwOcCK" +
                "mKxHxyUuggufOjgJGvUoUooMBpMXDGrNohNzJbQmnCuFVHxulXEircCYtezkVeEkJKFRDXUlwvOSrfhTmnoytjeQbBcIaRZYsRAn" +
                "isNlMSHqEXQIDirVGicrcOUxQzYhZlnHqwbKslxGkAjIPesZeQKSgbDMnFaAmVLOOrONYuszgMCNCvNGYJFOWGpZUmNvWvTWFoEX" +
                "QrPSvEmhxyYmFuIfJppvJSDlggOwcXYiXdtgoFfRZyTQbQmdnhoVHvQffkcMSqHDGRpwSLRbqDWwHJOkVJSMFzHdttJwyVpWuTDa" +
                "uFXOuHfpoXkHStuKkreyWLsasXTRVchqRaEYjUmcTuNZYlcVPFezKqIaDiRcRxuuYFtMzJqOIwMNmITofhdQiXTIfUHGfsYnPYgM" +
                "wpVdzGoyIrSSKJjWMnGdhmJHwHoFCqNtXOnbltOMnlCuzBsNUIlrKgfHRTAzAoFAlMvxgIrGffqCLtpAdbCbxzxMuivOctNDhuXy" +
                "iTuSYUvHnxZxklJDuMbMeGqQddtmAMVAhGNPVfdBaNQiDNxrRqLqFuYMjkDwekzDSgkFbCEiHPwmAqAuoRDrtnwXQfkhyKLNlhRE" +
                "ZxDybKsCrrQPcrNFKskrvceHPGjvyBtVgaHTSmtzdMSVwNyMIlmsMIgZqpVCNVBmwPVYkEfuenVdGeQcSdGSEMuJGOdWXyMbVQiN" +
                "hKKwnoffMoKJEbfgSLOMsSVUwEBRGFBPFhhsKLZrmPIdSMKwxhPOvzGywVCamswfIspSUHXyvtQrWGNUkyVyYWwjwumRSVfDFepT" +
                "JnNHLfbAdkCfHsEbUQCHBlSlgpYExOjYyuYbLjCjBYdNXcfdShRRGhzcdsxmsbAnHunqzIeVsOrlJRfdhaDYtmeIOUBCqFZBpLXT" +
                "jkvAhXYTjLCmcKebgCUFWCLgYnfxYIrTnbLuEyNPeIlyNIWefQrpYKHtEaUVESWrHYFWGHObaKnpspDilNhkfrWdFXUyZEgkbBPC" +
                "DSUAsUKPpFNyrWPZCPHzVARrspCJbXNCkotJGtgrDdLMMGIPLEPirRZzRxYgDiefAsUvCTnQXGHTOXTCRgjZCegPBxXTblJVFepV" +
                "YzPbwggNJDRgrHbtLvEQilSMnHSmeJahMUjJXXHFwZXQlMRVrPolpogKooCqIYErEiNnaWrbOaQhLRVxGCGHVnSaHyqaFQwdaSUF" +
                "JRjJWyDvRjwIAVYTEghKdtdHnATaBxrExrpBmIdbZciYLtZszlxmziScLwdJWaXVycSAdubKkZGlozHnjBVRqllIzFufoSLgxExE";

        STR_LENGTH = string.length();
    }

    // Test the cases that fixed length is smaller than the length of bytes array

    @Test
    public void testLongInput() throws IOException {
        fixedLengthInputStream = new FixedLengthInputStream(new ByteArrayInputStream(string.getBytes()), LENGTH_LIMIT1);

        final int LENGTH = 10000;   // LENGTH > LENGTH_LIMIT1
        byte[] bytes = new byte[LENGTH_LIMIT1];

        Assert.assertEquals(fixedLengthInputStream.read(bytes, 0, LENGTH), LENGTH_LIMIT1);
        Assert.assertArrayEquals(bytes, string.substring(0, LENGTH_LIMIT1).getBytes());
    }

    @Test
    public void testLengthLimit() throws IOException {
        fixedLengthInputStream = new FixedLengthInputStream(new ByteArrayInputStream(string.getBytes()), LENGTH_LIMIT1);

        fixedLengthInputStream.read(new byte[LENGTH_LIMIT1], 0, LENGTH_LIMIT1);

        int b = fixedLengthInputStream.read();
        Assert.assertEquals(b, -1);
    }

    @Test
    public void testRead() throws IOException {
        fixedLengthInputStream = new FixedLengthInputStream(new ByteArrayInputStream(string.getBytes()), LENGTH_LIMIT1);

        final int INDEX = 888;
        final int ARRAY_LEN = 10000;

        fixedLengthInputStream.read(new byte[ARRAY_LEN], 0, INDEX);

        int b = fixedLengthInputStream.read();
        Assert.assertEquals(b, string.charAt(INDEX));
    }

    @Test
    public void testMultiRead() throws IOException {
        fixedLengthInputStream = new FixedLengthInputStream(new ByteArrayInputStream(string.getBytes()), LENGTH_LIMIT1);

        final int LENGTH1 = 2999, LENGTH2 = 3001;   // LENGTH1 + LENGTH2 > LENGTH_LIMIT1

        byte[] bytes = new byte[LENGTH_LIMIT1];
        fixedLengthInputStream.read(bytes, 0, LENGTH1);
        fixedLengthInputStream.read(bytes, LENGTH1, LENGTH2);
        final byte[] bytes1 = string.substring(0, LENGTH_LIMIT1).getBytes();
        Assert.assertArrayEquals(bytes, bytes1);
    }


    
    // Test the cases that fixed length is larger than the length of bytes array

    @Test
    public void testLongInput2() throws IOException {
        fixedLengthInputStream = new FixedLengthInputStream(new ByteArrayInputStream(string.getBytes()), LENGTH_LIMIT2);

        final int LENGTH = 15000;   // LENGTH_LIMIT2 > LENGTH > STR_LENGTH
        byte[] bytes = new byte[LENGTH_LIMIT2];

        Assert.assertEquals(fixedLengthInputStream.read(bytes, 0, LENGTH), STR_LENGTH);
        Assert.assertArrayEquals(bytes, concatByteArrays(string.getBytes(), new byte[LENGTH_LIMIT2 - STR_LENGTH]));
    }

    @Test
    public void testLengthLimit2() throws IOException {
        fixedLengthInputStream = new FixedLengthInputStream(new ByteArrayInputStream(string.getBytes()), LENGTH_LIMIT2);

        fixedLengthInputStream.read(new byte[LENGTH_LIMIT2], 0, LENGTH_LIMIT2);

        int b = fixedLengthInputStream.read();
        Assert.assertEquals(b, -1);
    }

    @Test
    public void testMultiRead2() throws IOException {
        fixedLengthInputStream = new FixedLengthInputStream(new ByteArrayInputStream(string.getBytes()), LENGTH_LIMIT2);

        final int LENGTH1 = 8999, LENGTH2 = 3001;       // LENGTH1 + LENGTH2 > STR_LENGTH > LENGTH1

        byte[] bytes = new byte[LENGTH_LIMIT2];
        fixedLengthInputStream.read(bytes, 0, LENGTH1);
        fixedLengthInputStream.read(bytes, LENGTH1, LENGTH2);
        final byte[] bytes1 = concatByteArrays(string.getBytes(), new byte[LENGTH_LIMIT2 - STR_LENGTH]);
        Assert.assertArrayEquals(bytes, bytes1);
    }

    private byte[] concatByteArrays(byte[]... byteArrays) {
        int totalLen = 0;
        for (byte[] bytes : byteArrays) {
            totalLen += bytes.length;
        }

        byte[] res = new byte[totalLen];

        int start = 0;
        for (byte[] bytes : byteArrays) {

            System.arraycopy(bytes, 0, res, start, bytes.length);

            start += bytes.length;
        }

        return res;
    }
}
