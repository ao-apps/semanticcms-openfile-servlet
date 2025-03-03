/*
 * semanticcms-openfile-servlet - SemanticCMS desktop integration mode for local content creation in a Servlet environment.
 * Copyright (C) 2013, 2014, 2016, 2017, 2020, 2021, 2022, 2023, 2024  AO Industries, Inc.
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
 * along with semanticcms-openfile-servlet.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.semanticcms.openfile.servlet.ajax;

import com.aoapps.lang.io.ContentType;
import com.semanticcms.openfile.servlet.OpenFile;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

/**
 * Opens the file provided in the book and path parameters.  This file
 * must reside within this application and be of a supported type.
 * This is to be called by the JavaScript function openFile.
 *
 * <pre>Request parameters:
 *   book  The name of the book of the file to open
 *   path  The book-relative path of the file to open</pre>
 */
@WebServlet(OpenFileServlet.SERVLET_PATH)
public class OpenFileServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private static final Charset ENCODING = StandardCharsets.UTF_8;

  public static final String SERVLET_PATH = "/semanticcms-openfile-servlet/ajax/open-file";

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    try {
      OpenFile.openFile(
          getServletContext(),
          request,
          response,
          request.getParameter("book"),
          request.getParameter("path")
      );
      // Write output
      response.resetBuffer();
      response.setContentType(ContentType.TEXT);
      response.setCharacterEncoding(ENCODING.name());
      PrintWriter out = response.getWriter();
      out.println("Success");
    } catch (SkipPageException e) {
      // Nothing to do
    }
  }
}
