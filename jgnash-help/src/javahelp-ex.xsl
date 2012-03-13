<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://docbook.org/ns/docbook"
  xmlns:doc="http://nwalsh.com/xsl/documentation/1.0"
  xmlns:ng="http://docbook.org/docbook-ng"
  xmlns:db="http://docbook.org/ns/docbook"
  xmlns:exsl="http://exslt.org/common"
  version="1.0"
  exclude-result-prefixes="doc ng db exsl d">

	<xsl:import href="../generated-resources/docbook/javahelp/javahelp.xsl"/>

	<xsl:param name="generate.toc">
		appendix nop
		article nop
		book nop
		chapter nop
	</xsl:param>

	<xsl:param name="chapter.autolabel" select="0" />
	<xsl:param name="chunk.first.sections" select="1" />
	<xsl:param name="chunker.output.encoding" select="UTF-8" />
	<xsl:param name="chunker.output.indent" select="yes" />
	<xsl:param name="html.stylesheet" select="'../javahelp.css'" />
	<xsl:param name="admon.graphics" select="1" />
	<xsl:param name="admon.graphics.path"> ../images/</xsl:param>
	<xsl:param name="ignore.image.scaling" select="1"></xsl:param>

	<xsl:template name="helpset">
		<xsl:call-template name="write.chunk.with.doctype">
			<xsl:with-param name="filename" select="concat($base.dir,'jhelpset.hs')" />
			<xsl:with-param name="method" select="'xml'" />
			<xsl:with-param name="indent" select="'yes'" />
			<xsl:with-param name="doctype-public" select="'-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN'" />
			<xsl:with-param name="doctype-system" select="'http://java.sun.com/products/javahelp/helpset_2_0.dtd'" />
			<xsl:with-param name="content">
			  <xsl:call-template name="helpset.content" />
			</xsl:with-param>
			<xsl:with-param name="quiet" select="$chunk.quietly"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="helpset.content">
		<xsl:variable name="title">
			<xsl:apply-templates select="." mode="title.markup" />
		</xsl:variable>

		<helpset version="1.0">
			<title>
				<xsl:value-of select="normalize-space($title)" />
			</title>

			<!-- maps -->
			<maps>
				<homeID>top</homeID>
				<mapref location="jhelpmap.jhm" />
			</maps>

			<!-- views -->
			<view>
				<name>TOC</name>
				<label>Table Of Contents</label>
				<type>javax.help.TOCView</type>
				<data>jhelptoc.xml</data>
				<image>homeIcon</image>
			</view>

			<view>
				<name>Search</name>
				<label>Search</label>
				<type>javax.help.SearchView</type>
				<data engine="com.sun.java.help.search.DefaultSearchEngine">JavaHelpSearch</data>
				<image>searchIcon</image>
			</view>

			<view>
				<name>Favorites</name>
				<label>Favorites</label>
				<type>javax.help.FavoritesView</type>
				<image>addBookmarkIcon</image>
			</view>

			<presentation default="true">
				<name>main window</name>
				<size height="500" width="640" />
				<location x="0" y="0" />
				<title>jGnash Help</title>
				<toolbar>
					<helpaction image="backIcon">javax.help.BackAction</helpaction>
					<helpaction image="forwardIcon">javax.help.ForwardAction</helpaction>
					<helpaction>javax.help.SeparatorAction</helpaction>
					<helpaction image="addBookmarkIcon">javax.help.FavoritesAction
					</helpaction>
					<helpaction>javax.help.SeparatorAction</helpaction>
					<helpaction image="printIcon">javax.help.PrintAction</helpaction>
					<helpaction image="printSetupIcon">javax.help.PrintSetupAction
					</helpaction>
				</toolbar>
				<image>appIcon</image>
			</presentation>
		</helpset>
	</xsl:template>

	<xsl:template name="helpmap">
		<xsl:call-template name="write.chunk.with.doctype">
			<xsl:with-param name="filename"
				select="concat($base.dir, 'jhelpmap.jhm')" />
			<xsl:with-param name="method" select="'xml'" />
			<xsl:with-param name="indent" select="'yes'" />
			<xsl:with-param name="doctype-public"
				select="'-//Sun Microsystems Inc.//DTD JavaHelp Map Version 2.0//EN'" />
			<xsl:with-param name="doctype-system"
				select="'http://java.sun.com/products/javahelp/map_2_0.dtd'" />
			<xsl:with-param name="encoding" select="$javahelp.encoding" />
			<xsl:with-param name="content">
				<xsl:call-template name="helpmap.content" />
			</xsl:with-param>
			<xsl:with-param name="quiet" select="$chunk.quietly"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="helpmap.content">
		<map version="2.0">
			<xsl:choose>
				<xsl:when test="$rootid != ''">
					<xsl:apply-templates select="key('id',$rootid)//d:set
                                     | key('id',$rootid)//d:book
                                     | key('id',$rootid)//d:part
                                     | key('id',$rootid)//d:reference
                                     | key('id',$rootid)//d:preface
                                     | key('id',$rootid)//d:chapter
                                     | key('id',$rootid)//d:appendix
                                     | key('id',$rootid)//d:article
                                     | key('id',$rootid)//d:colophon
                                     | key('id',$rootid)//d:refentry
                                     | key('id',$rootid)//d:section
                                     | key('id',$rootid)//d:sect1
                                     | key('id',$rootid)//d:sect2
                                     | key('id',$rootid)//d:sect3
                                     | key('id',$rootid)//d:sect4
                                     | key('id',$rootid)//d:sect5
                                     | key('id',$rootid)//d:indexterm 
                                     | key('id',$rootid)//d:glossary
                                     | key('id',$rootid)//d:bibliography
				     | key('id',$rootid)//*[@id]"
                             mode="map"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:apply-templates select="//d:set
                                     | //d:book
                                     | //d:part
                                     | //d:reference
                                     | //d:preface
                                     | //d:chapter
                                     | //d:appendix
                                     | //d:article
                                     | //d:colophon
                                     | //d:refentry
                                     | //d:section
                                     | //d:sect1
                                     | //d:sect2
                                     | //d:sect3
                                     | //d:sect4
                                     | //d:sect5
                                     | //d:indexterm
                                     | //d:glossary
                                     | //d:bibliography
				     | //*[@id]"
                             mode="map"/>
				</xsl:otherwise>
			</xsl:choose>

			<mapID target="printIcon" url="../icons/document-print.png" />
			<mapID target="printSetupIcon" url="../icons/document-page-setup.png" />
			<mapID target="backIcon" url="../icons/go-previous.png" />
			<mapID target="forwardIcon" url="../icons/go-next.png" />
			<mapID target="addBookmarkIcon" url="../icons/folder-saved-search.png" />
			<mapID target="searchIcon" url="../icons/edit-find.png" />
			<mapID target="homeIcon" url="../icons/go-home.png" />
			<mapID target="appIcon" url="../icons/help-browser.png" />
		</map>
	</xsl:template>

</xsl:stylesheet>
