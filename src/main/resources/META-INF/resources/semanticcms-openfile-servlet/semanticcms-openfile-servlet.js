/*
 * semanticcms-openfile-servlet - SemanticCMS desktop integration mode for local content creation in a Servlet environment.
 * Copyright (C) 2013, 2014, 2016, 2017, 2019, 2022, 2023  AO Industries, Inc.
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

/*
 * JavaScript available to any page in the project.
 * After including this script, be sure to set the openFileUrl to a correct value.
 */
semanticcms_openfile_servlet = {

  /*
   * The openFileUrl of the application.  This is set by JSP just after this
   * script is included.
   */
  openFileUrl : "",

  /*
   * Handles error messages from Ajax calls.
   */
  handleAjaxError : function(message, errorThrown) {
    window.alert(
      message + "\n"
      + "Error Thrown = " + errorThrown
    );
  },

  /*
   * Opens a file from the server, passing-in the full path to the file.
   * This is to be used in a trusted environment only.
   */
  openFile : async function(domain, book, path) {
    // See https://saturncloud.io/blog/how-to-make-an-ajax-call-without-jquery/
    // See https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch
    // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Using_promises
    // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/async_function
    // See https://developer.mozilla.org/en-US/docs/Web/API/fetch
    // See https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams
    // See https://developer.mozilla.org/en-US/docs/Web/API/Response
    try {
      // window.alert("path="+path);
      const response = await fetch(semanticcms_openfile_servlet.openFileUrl, {
        method : "POST", // POST because has side-effects
        body : new URLSearchParams({
          domain : domain,
          book : book,
          path : path
        }),
        mode : "same-origin",
        cache : "no-store",
        redirect : "error"
      });
      if (response.ok) {
        const result = await response.text();
        console.debug("result = " + result);
        // Do nothing, file is opened by the servlet container
      } else {
        throw new Error("Request failed: " + response.status + " " + response.statusText);
      }
    } catch (errorThrown) {
      semanticcms_openfile_servlet.handleAjaxError(
        "Unable to open file:\n"
        + "domain = " + domain + "\n"
        + "book = " + book + "\n"
        + "path = " + path,
        errorThrown
      );
    }
  }
};
