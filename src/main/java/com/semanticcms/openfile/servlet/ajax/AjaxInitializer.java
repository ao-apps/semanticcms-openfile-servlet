/*
 * semanticcms-openfile-servlet - SemanticCMS desktop integration mode for local content creation in a Servlet environment.
 * Copyright (C) 2016, 2017, 2019  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-openfile-servlet.
 *
 * semanticcms-openfile-servlet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-openfile-servlet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-openfile-servlet.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.openfile.servlet.ajax;

import com.semanticcms.core.renderer.html.HtmlRenderer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener("Registers the scripts and \"" + AjaxInitializer.HEAD_INCLUDE + "\" head include in HtmlRenderer.")
public class AjaxInitializer implements ServletContextListener {

	static final String HEAD_INCLUDE = "/semanticcms-openfile-servlet/head.inc.jspx";

	@Override
	public void contextInitialized(ServletContextEvent event) {
		HtmlRenderer htmlRenderer = HtmlRenderer.getInstance(event.getServletContext());
		htmlRenderer.addScript("jquery", "/webjars/jquery/" + Maven.properties.getProperty("jquery.version") + "/jquery.min.js");
		htmlRenderer.addHeadInclude(HEAD_INCLUDE);
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		// Do nothing
	}
}
