<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/changelog">
    <html>
      <head>
        <link rel="stylesheet" href="changelog.css" />
      </head>
      <body>
        <h1><a href="https://k9mail.app/"><img class="logo" src="https://k9mail.app/assets/img/k9-logo.svg"/></a> K-9 Mail Change Log</h1>
        <xsl:for-each select="release">
          <h3>
            Version <xsl:value-of select="./@version" /><xsl:if test="./@date"> (<xsl:value-of select="./@date" />)</xsl:if>
          </h3>
          <ul>
            <xsl:for-each select="change">
              <li>
                <xsl:value-of select="." />
              </li>
            </xsl:for-each>
          </ul>
        </xsl:for-each>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
