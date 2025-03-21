# Thunderbird for Android Security

## Security Audit

The code in this repository underwent an extensive security audit in collaboration with the Open Source Technology
Improvement Fund ([OSTIF](https://ostif.org/)) and [7ASecurity](https://7asecurity.com/) in the first half of 2023. For
more details, see
our [blog post](https://blog.thunderbird.net/2023/07/k-9-mail-collaborates-with-ostif-and-7asecurity-security-audit/).

## Verifying Fingerprints

These are the SHA-256 fingerprints for our signing certificates:

- Thunderbird: `B6:52:47:79:B3:DB:BC:5A:C1:7A:5A:C2:71:DD:B2:9D:CF:BF:72:35:78:C2:38:E0:3C:3C:21:78:11:35:6D:D1`
- Thunderbird Beta: `05:6B:FA:FB:45:02:49:50:2F:D9:22:62:28:70:4C:25:29:E1:B8:22:DA:06:76:0D:47:A8:5C:95:57:74:1F:BD`
- K-9 Mail: `55:C8:A5:23:B9:73:35:F5:BF:60:DF:E8:A9:F3:E1:DD:E7:44:51:6D:93:57:E8:0A:92:5B:7B:22:E4:F5:55:24`

You can use the following command to retrieve and [verify](https://developer.android.com/tools/apksigner#usage-verify)
the certificate before installation:

```bash
apksigner verify -v --print-certs <path-to-apk>
```

## Reporting Vulnerabilities

You can report a security vulnerability through the [vulnerability reporting form](https://github.com/thunderbird/thunderbird-android/security/advisories/new).

We appreciate your support in making Thunderbird for Android as safe as possible!
