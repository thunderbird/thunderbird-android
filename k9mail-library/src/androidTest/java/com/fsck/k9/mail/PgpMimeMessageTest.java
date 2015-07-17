package com.fsck.k9.mail;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MimeMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.bc.BcPGPObjectFactory;
import org.spongycastle.openpgp.bc.BcPGPPublicKeyRingCollection;
import org.spongycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;

import static junit.framework.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class PgpMimeMessageTest  {
    private static final String PUBLIC_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
            "Version: GnuPG v1\n" +
            "\n" +
            "mQINBE49+OsBEADIu2zVIYllkqLYaCZq2d8r80titzegJiXTaW8fRS0FKGE7KmNt\n" +
            "tWvWdiyLqvWlP4Py9OZPmEBdz8AaPxqCFmVZfJimf28CW0wz2sRCYmmbQqaHFfpD\n" +
            "rK+EJofckOu2j81coaFVLbvkvUNhWU7/DKyv4+EBFt9fjxptbfpNKttwI0aeUVCa\n" +
            "+Z/m18+OLpeE33BXd5POrBb4edAlMCwKk8m4nDXJ3B+KmR0qfCLB79gqEjsDLl+y\n" +
            "65NcRk5uxIk53NRXHkmQujX1bsf5VFLha4KbUaB7BCtcSi1rY99WXfO/PWzTelOh\n" +
            "pKDIRq+v3Kl21TipY0t4kco4AUlIx5b1F0EHPpmIDr0gEheZBali5c9wUR8czc/H\n" +
            "aNkRP81hTPeBtUqp1S7GtJfcuWv6dyfBBVlnev98PCKOJo05meVwf3hkOLrciTfo\n" +
            "1yuy/9hF18u3GhL8HLrxMQksLhD6sPzDto4jJQDxKAa7v9aLoR7oIdeWkn1TU61E\n" +
            "ODR/254BRMoq619hqJwSNt6yOjGT2BBvlwbKdS8Xfw7SsBGGW8WnVJrqFCusfjSm\n" +
            "DBdV/KWstRnOMqw4nhAwNFfXmAL2L8a+rLHxalFggfGcvVpzDhJyTg+/R1y3JMCo\n" +
            "FfdFuhOTfkMqjGx8FgTmINOt54Wf9Xg6W0hQh3i98Wza3n8NuSPQJtAdqQARAQAB\n" +
            "tBVja2V0dGkgPGNrQGNrZXR0aS5kZT6JAhwEEAECAAYFAk+6naAACgkQctTBoSHq\n" +
            "3aHS+g/+MNxxfoEK+zopjWgvJmigOvejIpBWsLYJOJrpOgQuA61dQnQg0eLXPMDc\n" +
            "xQTrPtIlkn7idtLbaG2FScheOS0RdApL8UJTiU18dzjHUWsLLhEFhOAgw/kqcdG0\n" +
            "A95apNucybWU9jxynN9arxU6U+HZ67/JKxRjfdPxm+CmjiQwFPU9d6kGU/D08y58\n" +
            "1VIn7IopHlbqOYRuQcX0p6Q642oRBp4b6+ggov21mgqscKe/eBQ8yUxf61eywLbb\n" +
            "On63fkF1vl/RvsVcOnxcPLxUH4vmhuGPJ546RN7CCNjVF0QvuH9R8dnxS7/+rLe7\n" +
            "BVtZ/8sAy9r8LvnehZWVww4Wo9haVQxB69+ns63lEb+dzbBmsKbGvQ98S/Hs62Wj\n" +
            "nkMy7k+xzoRMa7tbKEtwwppxJVVSW//CVvEsS7DyaZna0udLh16MBCbMDzfAa3T4\n" +
            "PmgQPmV1BeysHcFOn3p6p2ZRcQGEdvMBYUjqxxExstwZEY8nGagvG7j5YCJKzBNY\n" +
            "xdBwkHXU3R3iM9o4aCKBsG2DMGHyhkHJXuGv9jFM32tAAf36qUJZ9eTKtoUt4xGt\n" +
            "LuxgnkS830c7nZBfJARro75SDG9eew91u2aIDGO3aNXeOODrYl2KOWbpXg/NJDwS\n" +
            "mlUZdwInb0PL6EDij1NtDiap2sIBKxtDjAeilS6vwS8s2P9HZdqJAkEEEwECACsC\n" +
            "GyMFCRLMAwAGCwkIBwMCBhUIAgkKCwQWAgMBAh4BAheABQJOPftbAhkBAAoJEO4v\n" +
            "7zp9qOKJG+oP/RBN5ahJCpwrk7U05J8x7UOPuP4UElMYoPYZCSp55mH6Xmr2C626\n" +
            "DvTxhElz1WY7oIOJ7Mgp1RtGqZYV52d6fER10jowGbSkiFTvKb4PhQl4+AcGODMY\n" +
            "LRVBw90rRhDSXzBQMeiyUf7Wse1jPsBfuOe6V1TsAtqjaAzrUDQOcsjQqW5ezvIj\n" +
            "GNTFunX6wMUHzSBX6Lh0fLAp5ICp+l3agJ8S41Y4tSuFVil2IRX3o4vqxvU4f0C+\n" +
            "KDIeJriLAMHajUp0V6VdisRHujjoTkZAGogJhNmNg0YH191a7AAKvVePgMQ/fsoW\n" +
            "1hm9afwth/HOKvMx8fgKMwkn004V/to7qHByWDND33rgwlv1LYuvumEFd/paIABh\n" +
            "dLhC6o6moVzwlOqhGfoD8DZAIzNCS4q2uCg8ik4temetPbCc5wMFtd+FO+FOb1tO\n" +
            "/RahWeBfULreEijnv/zUZPetkJV9jTZXgXqCI9GCf6MTJrOLZ+G3hVxFyyHTKlWt\n" +
            "iIzJHlX9rd3oQc7YJbdDFMZA+SdlGqiGdsjBmq0kcRqhhEa5QsnoNm9tuPuFnL5o\n" +
            "GG7OFPztj9tr9ViRvsFBlx9jvmjRbRNF3287j1r+4lbGigsA1o8bRkLLXVSK1gCw\n" +
            "bOLAPNJYH5bde6O+Qb8bepg9TByiohsFssxYXHwbgu/pcCMU1hCf15t4iQI+BBMB\n" +
            "AgAoBQJOPfr+AhsjBQkSzAMABgsJCAcDAgYVCAIJCgsEFgIDAQIeAQIXgAAKCRDu\n" +
            "L+86fajiic/5EACHIaprlic0VKeV1H564KionZd7y3i3mX+e7Mdkd9QBFkb14UBw\n" +
            "3RFnQhvq1MtaAC1lIYACYdIMF6/8LB1WQjB7kyt0IHbjEyodBVHq3U9n+mt+ZFy3\n" +
            "6loA2r1odFJIaUWA2jBlBhtd3AQriANv0yciv4dPqPQfeAR5GxDiRbzGP1FZ47To\n" +
            "PXZDHY9EKwaXo4q5D7XHzQy2aFe0IVUzXnofSE2KP9bu/wUU2DjZJ4cVXFdGFv5D\n" +
            "xQ48UgXfhmPXSx1eeElDWdZHhH8BI7DOL66+FKm9PLiDYHUuVTvPxFSppu/+Gw5p\n" +
            "gqDBwWEeKtJ1Yf3a5Vvbt+EK8BgC1/KaqY7A++dD2vM7w8PIKcf57WXF4O6KkIiW\n" +
            "0M36eoAqAyuwqeTh3+mCWewegQBS2wORBYipbDf9OPTj/fsyCkaaXM2/wee79m+W\n" +
            "+/67HVYlpIJPIKJIGs1N0PTl8WYZdaMLSL7nU/y3j51ytdidiKvRWl5X3MaCpp07\n" +
            "T8MSogntMxXLU2zEnUqJjykXVpavFfXi1piw98qd+5wKMwiGLRq52z73N+q5nWk+\n" +
            "5B2gqA3soXvloxXmoVuoTZDSnTjuQZk1kVl2XA+enE5rjVzpGte56QRYOGrjI9II\n" +
            "SjH/PYLKSwjw8YzTeYFrv5UHegjU1G7auq5nJLsCupxADoRBw2y99Oiyg7QeY2tl\n" +
            "dHRpIDxja2V0dGlAZ29vZ2xlbWFpbC5jb20+iQIcBBABAgAGBQJPup3IAAoJEHLU\n" +
            "waEh6t2h1EoP/1Uw+cWK2lJU2BTwWuSTgL/SPoFoR+UKWQ7fES4eTZ330hHmWb4V\n" +
            "Xpg+ZR6QYhXnJxMOMZ2tnya95GgdMJ+Hd4vlq6qb8746wmzIOt5XjhdMr3yiUsY9\n" +
            "NC6P6ymuYEwuNMQBU/Z53rpuoFaF4Wc9nycK+3Gj6t3aPU0JX+qiFJl63+8GNw/Q\n" +
            "CL+JQ4URQB3Vw/RADZfTBbT3VmrdSLGX2/I+nm64ysXvn6nt3q1JTHWXapPGrJXi\n" +
            "HTlvjg+Niw38iBeHOkZ9Td5BIPBlj/8SXy9weG55ruTJFw0SXhV3VXIGbN0ZuJ3g\n" +
            "nsusNCo4pJrFvJ0j3hzYrgOf/8jRUeesu7HlUPnYdBiJTNgKdCh5LlrKXlaisobl\n" +
            "H33aufjO6i5HrX+/b1U9wE/G7MIzopcgiaeSYSJpO9huBJ0+Jri/4tdxvgT6aeNz\n" +
            "9uL4rQKH2gUr9E89Np4aZ3zpp1QxfoJTVaR5AyJNaiiDOvZbvELYXK6QjAwgXIVr\n" +
            "ScopPOXL1E+fdV9tsvYJfTbTJLZ9qeMRIOBPyhSbiDrB4r/i5zYyfydeEFVxackY\n" +
            "vgSp++5HZt5lG0LFVjNnaPZETVCgVb5wmCxNsDqYV1fuxlAmPlTuXfMAvr+bxU/z\n" +
            "3dmBDc7X1VfJVLzb0M5Z0KqvQlWTZkAkIPdQarJchvOBnFa7Rb6qFpcAiQI+BBMB\n" +
            "AgAoAhsjBQkSzAMABgsJCAcDAgYVCAIJCgsEFgIDAQIeAQIXgAUCTj37WgAKCRDu\n" +
            "L+86fajiiXgTD/9tA3FTGjiCE4Z0Nzi/Q+jmNJXGr/MQvgSlbKTGKJKkNk64kLTu\n" +
            "HhYdbNhj/8419fINhxOzbetdWi+RUIRqk/FstBNGCbFYwNBbhp7jSToHLw1oESoN\n" +
            "zPhxkuptvjyaEjrn50ydykVdTeMjytmZ3w7iu5eOt+tNS0x0thGfM3a4kdYoKW0v\n" +
            "mp2BmrtUAXXsOJ475EK6IXeoGLMbgA+JtiDnWH12t/Dfl7L/6Nxjk1fGlihcJl6P\n" +
            "Z1ZytDuRjnvlt77nqMaka7N+GadqmPUWonhKg/aGPMEgQUD4IWM/2Y2EpJIqVfB5\n" +
            "Dv7llScCRB8mte/T8/dvpgr5B0KqGJDudb7Cgp+8zDGCU+M3uHU5ZQRlBO3bbCML\n" +
            "nwT6BxmLT/6ufW7nT1eXscDi+DFKsLa6FQmDY38tzB6tyYlHxQU3RTkm4cLfDzI8\n" +
            "/0JPRfx/RlKLW39QEmFJySMB3IVRtp5R0KNoKaAtYb5hRvD2JJJnx5q0u3h+me6j\n" +
            "RzCMPJWxRKQjx0MdKEJedAH02XEqgeTunm7Kitb3aYuSykHUt2D/fgA4/CQoThF5\n" +
            "SYUVbviYToEu/1hQAeHe9S1F92jCrjuTUmqejoVotk5O3uHBr7A3ASOoBrdaXxuS\n" +
            "x9WpcRprfdtoD36TDWsSuarNxFVzcGFDaV2yN6mIf2LXTNgw2UAOHJzUqokCPgQT\n" +
            "AQIAKAUCTj346wIbIwUJEswDAAYLCQgHAwIGFQgCCQoLBBYCAwECHgECF4AACgkQ\n" +
            "7i/vOn2o4onPNxAAq6jqkpbx0g/UIdNR2Q7mGQ0QJNbkt2P8Vq7jqwUu4td6GJdC\n" +
            "vy4+RUWo5aRNQ3NgzRFkjLxIrTeSfK+yjruk01r3naGh6h0rk/EY1RCw1sA6GHVV\n" +
            "gFcf83JtfgxH4NE8br+eiNnMODhOXG/UJsBMNo8bfyZu3FnJdUebCODMACJimKWb\n" +
            "gBXa5EOnDZzXjYQrNRt95/yHse76V8JLdHqSnYPvVwcIT6MubF2NPspSFjfnFsj9\n" +
            "J1Fb6aiI+3ob6HJNt2kyN0CdnnR/ZEZun8KQ37jJy7f5LXI6FDDT52oPBfddRRwy\n" +
            "qZsmprbQjxUdIPKAYyjIELy+iAoFTrsJYvGNrgGMHI2ecyC2TE3uJ3qFALLhkFAS\n" +
            "xYR+sSjAI3nJHPcfsfg10clrCfhh1KDWJjlVGgFjNd0MKIhLKA4kfwQvU4BSr5Al\n" +
            "3fzflkRQuLDTNEeM9fwVW6ew+7IHpBNmYtnkSbmURcZoA4y8VuHH7qHID756kf4W\n" +
            "u+wfNLf0SUZ1061y+PI77wUPUEVI2uJzo0xuHMG+L0TitRUv0zvaIGFt9ClX03FU\n" +
            "6r1PPLGG1JNWuBORNgTJVIQzhLM3du7OnCdc4NhfOqZUfdWrIbgPEc870DnQSdmn\n" +
            "J9OTF082SXEfEbjYzLuS5/aImXENypp6A7zeHBJ+TBJUNQj0c7S1qBeQGey0IUNo\n" +
            "cmlzdGlhbiBLZXR0ZXJlciA8Y2tAY2tldHRpLmRlPokCPgQTAQIAKAUCU/eh2wIb\n" +
            "IwUJEswDAAYLCQgHAwIGFQgCCQoLBBYCAwECHgECF4AACgkQ7i/vOn2o4omSGg/9\n" +
            "EIj+Zz5rqC9BOC3sxbvyvZaPz0G6gT36i0ZW9Qe1drqxs7rcUelYPii8TPB/4v+H\n" +
            "cx82qQpSnD6X7e8hNuRgsulkgZIhT/jnBFEJJoyMtt25UZIolj4JFpw1g+PRkufu\n" +
            "KlVZCisJup+fFN2O6IcsfFqXxnaITWalFYMvOXwJ9rbT+2kczsH+MnxqeRvFQw7u\n" +
            "Gy7Q3bu+A9+rntBhz/LPdzBOJBJh672Y9f9UVsmEFB66d84l7yUs1vlzi9DAqJK4\n" +
            "y49hCe9QMv+NwL9rB9QxLdrbX724IRvGVwRzufd5jHOgAsYbizO+QltBJTsmRHvi\n" +
            "yKClxuiUE4ygQyd5TT3ATC8wQKGfAGWRWaZoi3X6wWvKvW8cPg8ilMoTtPTlZuPL\n" +
            "G32n0NaD7dacpmKfaLeopPAJgrnTl9LwPEDg4dwcSK+ETCY1BcoVGtOVxH1ghMd2\n" +
            "IYOX+BSJiG39ApiHHBwPtc/PIqPjtR7MGB6dCldZZ46eHleCB8Re5HPrQAok+ijb\n" +
            "XX0gx7ACYTniH+TsFszZyuLGstR8Cs8s7MwnbAX40506lDrj9c+0FE69/rJIMQsc\n" +
            "wauGk1x1UaK2+gzBw28ymilhBbuOFabuStAHfGx/1niJMgBO4BiOPIBTjMOYtARR\n" +
            "OSZ9dNGXkKYnxtN6T/kTO3F5N/fFJ42WjDWbrvfqDSy5Ag0ETj346wEQALEnw5y/\n" +
            "zL3QAug9xuHktdVKCbxwAy8Q1ei5UA/GTGnTLdsHIN5e1B2bJyZaYcPTIT+xNgzP\n" +
            "hwDQTosFFpg/JLP1xI28mShk8ai3ls73EhJLUGazOZ0ujxyMkWD0rIBMee6YkQMG\n" +
            "zUkJKaEtqeVLci67Q8QLHLfE331JyTtd0gwlps6FAd7PuCl/50cayr0yXMx67iwK\n" +
            "kyvXaLHYUjdK13MC2xoc4VrirzfNtX0JtCmAYoJ2i2Yq7vgLQasUjbzUsLUuwhol\n" +
            "yoxwE6lB6paBdTh1dTa4mCN3Y8gM+CMveqQUcZuOyFZDWNtMPPCNeWWRkKgfc+fw\n" +
            "HSiCHhDWu/7S6/xSqDb3qegXm6cAA2WFxJ+oEwTSRvK/89y6T3oiFbjmZs+sSRjr\n" +
            "ZAsE3rDC2WFRUFBq6/V7+eO2F1fqNLPzXOaVQX9i3BHv4XjxC0PQoVFnvpSJlHSW\n" +
            "Vuw5xA3Qqa8GuB80zWEqVBJ30gfqj1BAErpKwaVKJOuvRuQa2wkq7iXO/Io4S7UQ\n" +
            "HFO+U9W87PaPNdfjxxEsVmexeXhF8l5zwHYyqKK0Pch/YDoUk/+w7Jn3cpmpceim\n" +
            "YVEDr/YqrbvLpakHuEQiDgWZmcHHEVA7DbfsOULqq1vnpVq0TictdZ20Z8MJ2gAM\n" +
            "P9HCZHPxLafI3YqQrXR3UIHb48Zwy9tdMv7NABEBAAGJAiUEGAECAA8FAk49+OsC\n" +
            "GwwFCRLMAwAACgkQ7i/vOn2o4okF+BAAkN0Kd404HPy/35mCCdWm5DHpcxEURoY1\n" +
            "X6mv6D+pvPQHUN9GKeYYT6wjcpsDsCn2UX9mp0e24SXOxZoVlJ7T6L/QN+MUwnt2\n" +
            "LAO9XCZLMijhe7KX51FJjld1W9XfauqhPlR1Lzr9cJI3UdiYcsZH3X6SfW/hLLRE\n" +
            "MWm/3YfACVVWNkG9PanhroNcVr925k/y58WRKdJOOgMGGBYyIAvtWb6m0Qn978AE\n" +
            "53r7msHwZq06sPXIZJpCl6CTeyMrqU90G+JJY3BfP9rFsU9OLkDRrsAELleI9iXP\n" +
            "QGw6Ixezdi93CqY+Y4weCjtYxm/5vKxwssg/ALVkM/VftWgWRSnZmnZwubgBzgwy\n" +
            "wBwGHxPHz7CV3lBKZfw8U3L4Md3u1bMUu6Y+jW+322D+7+ZaLdJejmmJcEvLaItd\n" +
            "c60IHTM/GbtV7TDiqQaRmyLY5KxnwGLthcYUsGI7HYDNqEa1+cRctB8lEWpgTjHK\n" +
            "nwemvB5c1fPxao7w15O0tvSCX2kD5UMoAbvWJJvxcUTPTPBEHTYWrAk+Ny7CbdMA\n" +
            "+71r942RXo9Xdm4hqjfMcDXdQmfjftfFB1rsBd5Qui8ideQP7ypllsWC8fJUkWN6\n" +
            "3leW5gysLx9Mj6bu6XB4rYS1zH2keGtZe4Qqlxss7JPVsJzD9xSotg+G/Wb7F3HL\n" +
            "HzpeeqkwzVU=\n" +
            "=3yEX\n" +
            "-----END PGP PUBLIC KEY BLOCK-----\n";


    @Test
    public void testSignedMessage() throws IOException, MessagingException, PGPException {
        String messageSource = "Date: Mon, 08 Dec 2014 17:44:18 +0100\r\n" +
                "From: cketti <cketti@googlemail.com>\r\n" +
                "MIME-Version: 1.0\r\n" +
                "To: test@example.com\r\n" +
                "Subject: OpenPGP signature test\r\n" +
                "Content-Type: multipart/signed; micalg=pgp-sha1;\r\n" +
                " protocol=\"application/pgp-signature\";\r\n" +
                " boundary=\"24Bem7EnUI1Ipn9jNXuLgsetqa6wOkIxM\"\r\n" +
                "\r\n" +
                "This is an OpenPGP/MIME signed message (RFC 4880 and 3156)\r\n" +
                "--24Bem7EnUI1Ipn9jNXuLgsetqa6wOkIxM\r\n" +
                "Content-Type: multipart/mixed;\r\n" +
                " boundary=\"------------030308060900040601010501\"\r\n" +
                "\r\n" +
                "This is a multi-part message in MIME format.\r\n" +
                "--------------030308060900040601010501\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Transfer-Encoding: quoted-printable\r\n" +
                "\r\n" +
                "Message body\r\n" +
                "goes here\r\n" +
                "\r\n" +
                "\r\n" +
                "--------------030308060900040601010501\r\n" +
                "Content-Type: text/plain; charset=UTF-8;\r\n" +
                " name=\"attachment.txt\"\r\n" +
                "Content-Transfer-Encoding: base64\r\n" +
                "Content-Disposition: attachment;\r\n" +
                " filename=\"attachment.txt\"\r\n" +
                "\r\n" +
                "VGV4dCBhdHRhY2htZW50Cg==\r\n" +
                "--------------030308060900040601010501--\r\n" +
                "\r\n" +
                "--24Bem7EnUI1Ipn9jNXuLgsetqa6wOkIxM\r\n" +
                "Content-Type: application/pgp-signature; name=\"signature.asc\"\r\n" +
                "Content-Description: OpenPGP digital signature\r\n" +
                "Content-Disposition: attachment; filename=\"signature.asc\"\r\n" +
                "\r\n" +
                "-----BEGIN PGP SIGNATURE-----\r\n" +
                "Version: GnuPG v1\r\n" +
                "\r\n" +
                "iQIcBAEBAgAGBQJUhdVqAAoJEO4v7zp9qOKJ8DQP/1+JE8UF7UmirnN1ZO+25hFC\r\n" +
                "jAfFMxRWMWXN0gGB+6ySy6ah0bCwmRwHpRBsW/tNcsmOPKb2XBf9zwF06uk/lLp4\r\n" +
                "ZmGXxSdQ9XJrlaHk8Sitn9Gi/1L+MNWgrsrLROAZv2jfc9wqN3FOrhN9NC1QXQvO\r\n" +
                "+D7sMorSr3l94majoIDrzvxEnfJVfrZWNTUaulJofOJ55GBZ3UJNob1WKjrnculL\r\n" +
                "IwmSERmVUoFBUfe/MBqqZH0WDJq9nt//NZFHLunj6nGsrpush1dQRcbR3zzQfXkk\r\n" +
                "s7zDLDa8VUv6OxcefjsVN/O7EenoWWgNg6GfW6tY2+oUsLSP2OS3JXvYsylQP4hR\r\n" +
                "iU1V9vvsu2Ax6bVb0+uTqw3jNiqVFy3o4mBigVUqp1EFIwBYmyNbe5wj4ACs9Avj\r\n" +
                "9t2reFSfXobWQFUS4s71JeMefNAHHJWZI63wNTxE6LOw01YxdJiDaPWGTOyM75MK\r\n" +
                "yqn7r5uIfeSv8NypGJaUv4firxKbrcZKk7Wpeh/rZuUSgoPcf3I1IzXfGKKIBHjU\r\n" +
                "WUMhTF5SoC5kIZyeXvHrhTM8HszcS8EoG2XcmcYArwgCUlOunFwZNqLPsfdMTRL6\r\n" +
                "9rcioaohEtroqoJiGAToJtIz8kqCaamnP/ASBkp9qqJizRd6fqt+tE8BsmJbuPLS\r\n" +
                "6lBpS8j0TqmaZMYfB9u4\r\n" +
                "=QvET\r\n" +
                "-----END PGP SIGNATURE-----\r\n" +
                "\r\n" +
                "--24Bem7EnUI1Ipn9jNXuLgsetqa6wOkIxM--\r\n";

        BinaryTempFileBody.setTempDirectory(InstrumentationRegistry.getTargetContext().getCacheDir());

        InputStream messageInputStream = new ByteArrayInputStream(messageSource.getBytes());
        MimeMessage message;
        try {
            message = new MimeMessage(messageInputStream, true);
        } finally {
            messageInputStream.close();
        }

        Multipart multipartSigned = (Multipart) message.getBody();

        BodyPart signedPart = multipartSigned.getBodyPart(0);
        ByteArrayOutputStream signedPartOutputStream = new ByteArrayOutputStream();
        signedPart.writeTo(signedPartOutputStream);
        byte[] signedData = signedPartOutputStream.toByteArray();

        Body signatureBody = multipartSigned.getBodyPart(1).getBody();
        ByteArrayOutputStream signatureBodyOutputStream = new ByteArrayOutputStream();
        signatureBody.writeTo(signatureBodyOutputStream);
        byte[] signatureData = signatureBodyOutputStream.toByteArray();

        assertTrue(verifySignature(signedData, signatureData));
    }

    private boolean verifySignature(byte[] signedData, byte[] signatureData) throws IOException, PGPException {
        InputStream signatureInputStream = PGPUtil.getDecoderStream(new ByteArrayInputStream(signatureData));
        PGPObjectFactory pgpObjectFactory = new BcPGPObjectFactory(signatureInputStream);
        Object pgpObject = pgpObjectFactory.nextObject();

        PGPSignatureList pgpSignatureList;
        if (pgpObject instanceof PGPCompressedData) {
            PGPCompressedData compressedData = (PGPCompressedData) pgpObject;
            pgpObjectFactory = new BcPGPObjectFactory(compressedData.getDataStream());
            pgpSignatureList = (PGPSignatureList) pgpObjectFactory.nextObject();
        } else {
            pgpSignatureList = (PGPSignatureList) pgpObject;
        }
        PGPSignature signature = pgpSignatureList.get(0);

        InputStream keyInputStream = PGPUtil.getDecoderStream(new ByteArrayInputStream(PUBLIC_KEY.getBytes()));
        PGPPublicKeyRingCollection pgpPublicKeyRingCollection = new BcPGPPublicKeyRingCollection(keyInputStream);
        PGPPublicKey publicKey = pgpPublicKeyRingCollection.getPublicKey(signature.getKeyID());

        signature.init(new BcPGPContentVerifierBuilderProvider(), publicKey);
        InputStream signedDataInputStream = new ByteArrayInputStream(signedData);
        int ch;
        while ((ch = signedDataInputStream.read()) >= 0) {
            signature.update((byte) ch);
        }

        signedDataInputStream.close();
        keyInputStream.close();
        signatureInputStream.close();

        return signature.verify();
    }
}
