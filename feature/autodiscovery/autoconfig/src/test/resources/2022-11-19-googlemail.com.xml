<?xml version="1.0" encoding="UTF-8"?>
<clientConfig version="1.1">
  <emailProvider id="googlemail.com">
    <domain>gmail.com</domain>
    <domain>googlemail.com</domain>
    <!-- MX, for Google Apps -->
    <domain>google.com</domain>
    <!-- HACK. Only add ISPs with 100000+ users here -->
    <domain>jazztel.es</domain>

    <displayName>Google Mail</displayName>
    <displayShortName>GMail</displayShortName>

    <incomingServer type="imap">
      <hostname>imap.gmail.com</hostname>
      <port>993</port>
      <socketType>SSL</socketType>
      <username>%EMAILADDRESS%</username>
      <authentication>OAuth2</authentication>
      <authentication>password-cleartext</authentication>
    </incomingServer>
    <incomingServer type="pop3">
      <hostname>pop.gmail.com</hostname>
      <port>995</port>
      <socketType>SSL</socketType>
      <username>%EMAILADDRESS%</username>
      <authentication>OAuth2</authentication>
      <authentication>password-cleartext</authentication>
      <pop3>
        <leaveMessagesOnServer>true</leaveMessagesOnServer>
      </pop3>
    </incomingServer>
    <outgoingServer type="smtp">
      <hostname>smtp.gmail.com</hostname>
      <port>465</port>
      <socketType>SSL</socketType>
      <username>%EMAILADDRESS%</username>
      <authentication>OAuth2</authentication>
      <authentication>password-cleartext</authentication>
    </outgoingServer>

    <documentation url="http://mail.google.com/support/bin/answer.py?answer=13273">
      <descr>How to enable IMAP/POP3 in GMail</descr>
    </documentation>
    <documentation url="http://mail.google.com/support/bin/topic.py?topic=12806">
      <descr>How to configure email clients for IMAP</descr>
    </documentation>
    <documentation url="http://mail.google.com/support/bin/topic.py?topic=12805">
      <descr>How to configure email clients for POP3</descr>
    </documentation>
    <documentation url="http://mail.google.com/support/bin/answer.py?answer=86399">
      <descr>How to configure TB 2.0 for POP3</descr>
    </documentation>
  </emailProvider>

  <oAuth2>
    <issuer>accounts.google.com</issuer>
    <!-- https://developers.google.com/identity/protocols/oauth2/scopes -->
    <scope>https://mail.google.com/ https://www.googleapis.com/auth/contacts https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/carddav</scope>
    <authURL>https://accounts.google.com/o/oauth2/auth</authURL>
    <tokenURL>https://www.googleapis.com/oauth2/v3/token</tokenURL>
  </oAuth2>

  <enable visiturl="https://mail.google.com/mail/?ui=2&amp;shva=1#settings/fwdandpop">
    <instruction>You need to enable IMAP access</instruction>
  </enable>

  <webMail>
    <loginPage url="https://accounts.google.com/ServiceLogin?service=mail&amp;continue=http://mail.google.com/mail/" />
    <loginPageInfo
      url="https://accounts.google.com/ServiceLogin?service=mail&amp;continue=http://mail.google.com/mail/">
      <username>%EMAILADDRESS%</username>
      <usernameField id="Email" />
      <passwordField id="Passwd" />
      <loginButton id="signIn" />
    </loginPageInfo>
  </webMail>

</clientConfig>
