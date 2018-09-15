<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/changelog">
    <html>
      <body>
        <h2><img src="https://raw.githubusercontent.com/k9mail/k-9/master/app/ui/src/main/res/drawable-hdpi/icon.png"/><a href="https://k9mail.github.io/">K-9 Mail</a> Change Log</h2>
        <xsl:for-each select="release">
          <h3>
            <xsl:value-of select="./@version" />
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
