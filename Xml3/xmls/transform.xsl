<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html" />
    <xsl:template match="/*[local-name()='people']">
        <html>
            <body>
                <table border="1">
                    <tr>
                        <th>Id</th>
                        <th>Father</th>
                        <th>Mother</th>
                        <th>Brothers</th>
                        <th>Sisters</th>
                    </tr>
                    <xsl:apply-templates select="person"/>
                </table>
            </body>
        </html>
    </xsl:template>
    <xsl:template match="person">
        <xsl:if test="count(father) > 0 and count(mother) > 0 and
            (
            count(id(father/@person-id)/father) > 0 or
            count(id(father/@person-id)/mother) > 0 or
            count(id(mother/@person-id)/father) > 0 or
            count(id(mother/@person-id)/mother) > 0
            ) and
            (count(sister) > 0 or count(brother) > 0)
            ">
            <tr>
                <td>
                    <xsl:call-template name="info">
                        <xsl:with-param name="id" select="@id"/>
                    </xsl:call-template>
                </td>
                <td>
                    <xsl:call-template name="info">
                        <xsl:with-param name="id" select="father/@person-id"/>
                    </xsl:call-template>
                </td>
                <td>
                    <xsl:call-template name="info">
                        <xsl:with-param name="id" select="mother/@person-id"/>
                    </xsl:call-template>
                </td>
                <td>
                    <xsl:for-each select="brother">
                        <xsl:call-template name="info">
                            <xsl:with-param name="id" select="@person-id"/>
                        </xsl:call-template>
                        <p/>
                    </xsl:for-each>
                </td>
                <td>
                    <xsl:for-each select="sister">
                        <xsl:call-template name="info">
                            <xsl:with-param name="id" select="@person-id"/>
                        </xsl:call-template>
                        <p/>
                    </xsl:for-each>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>
    <xsl:template name="info">
        <xsl:param name="id"/>
        <div> id: <xsl:value-of select="$id"/>
        </div>
        <div> name:
            <xsl:value-of select="id($id)/@person-firstname"/>
            <space/>
            <xsl:value-of select="id($id)/@person-surname"/>
        </div>
        <div> gender: <xsl:value-of select="id($id)/@person-gender"/>
        </div>

        <div> father:
            <xsl:value-of select="id(id($id)/father/@person-id)/@person-firstname"/>
            <space/>
            <xsl:value-of select="id(id($id)/father/@person-id)/@person-surname"/>
        </div>
        <div> mother:
            <xsl:value-of select="id(id($id)/mother/@person-id)/@person-firstname"/>
            <space/>
            <xsl:value-of select="id(id($id)/mother/@person-id)/@person-surname"/>
        </div>

        <xsl:if test="count(id($id)/brother) > 0">
            <div> brothers: </div>
            <xsl:for-each select="id($id)/brother">
                <xsl:value-of select="id(@person-id)/@person-firstname"/>
                <space/>
                <xsl:value-of select="id(@person-id)/@person-surname"/>
                <p/>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="count(id($id)/sister) > 0">
            <div> sisters: </div>
            <xsl:for-each select="id($id)/sister">
                <xsl:value-of select="id(@person-id)/@person-firstname"/>
                <space/>
                <xsl:value-of select="id(@person-id)/@person-surname"/>
                <p/>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="count(id($id)/daughter) > 0">
            <div> daughters: </div>
            <xsl:for-each select="id($id)/daughter">
                <xsl:value-of select="id(@person-id)/@person-firstname"/>
                <space/>
                <xsl:value-of select="id(@person-id)/@person-surname"/>
                <p/>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="count(id($id)/son) > 0">
            <div> sons: </div>
            <xsl:for-each select="id($id)/son">
                <xsl:value-of select="id(@person-id)/@person-firstname"/>
                <space/>
                <xsl:value-of select="id(@person-id)/@person-surname"/>
                <p/>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>