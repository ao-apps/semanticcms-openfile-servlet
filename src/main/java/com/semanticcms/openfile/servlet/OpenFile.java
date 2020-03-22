/*
 * semanticcms-openfile-servlet - SemanticCMS desktop integration mode for local content creation in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020  AO Industries, Inc.
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
package com.semanticcms.openfile.servlet;

import com.aoindustries.io.FileUtils;
import com.aoindustries.lang.ProcessResult;
import com.aoindustries.net.DomainName;
import com.aoindustries.net.Path;
import com.aoindustries.servlet.ServletUtil;
import com.semanticcms.core.controller.Book;
import com.semanticcms.core.controller.ResourceRefResolver;
import com.semanticcms.core.controller.SemanticCMS;
import com.semanticcms.core.model.ResourceRef;
import com.semanticcms.core.resources.ResourceStore;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

final public class OpenFile {

	private static final Logger logger = Logger.getLogger(OpenFile.class.getName());

	private static final String ENABLE_INIT_PARAM = OpenFile.class.getName() + ".enabled";

	private static final String FILE_OPENERS_APPLICATION_ATTRIBUTE = OpenFile.class.getName() + ".fileOpeners";

	@WebListener
	public static class Initializer implements ServletContextListener {
		@Override
		public void contextInitialized(ServletContextEvent event) {
			getFileOpeners(event.getServletContext());
		}
		@Override
		public void contextDestroyed(ServletContextEvent event) {
			// Do nothing
		}
	}

	private static ConcurrentMap<String,FileOpener> getFileOpeners(ServletContext servletContext) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<String,FileOpener> fileOpeners = (ConcurrentMap<String,FileOpener>)servletContext.getAttribute(FILE_OPENERS_APPLICATION_ATTRIBUTE);
		if(fileOpeners == null) {
			fileOpeners = new ConcurrentHashMap<>();
			servletContext.setAttribute(FILE_OPENERS_APPLICATION_ATTRIBUTE, fileOpeners);
		}
		return fileOpeners;
	}

	/**
	 * Checks if the given host address is allowed to open files on the server.
	 */
	private static boolean isAllowedAddr(String addr) {
		return "127.0.0.1".equals(addr);
	}

	/**
	 * Checks if the given request is allowed to open files on the server.
	 * The servlet init param must have it enabled, as well as be from an allowed IP.
	 */
	public static boolean isAllowed(ServletContext servletContext, ServletRequest request) {
		return
			Boolean.parseBoolean(servletContext.getInitParameter(ENABLE_INIT_PARAM))
			&& isAllowedAddr(request.getRemoteAddr())
		;
	}

	private static String getJdkPath() {
		try {
			String hostname = InetAddress.getLocalHost().getCanonicalHostName();
			if(
				//"francis.aoindustries.com".equals(hostname)
				"freedom.aoindustries.com".equals(hostname)
			) return "/opt/jdk1.8.0-i686";
		} catch(UnknownHostException e) {
			// Fall-through to default 64-bit
		}
		return "/opt/jdk1.8.0";
	}

	public static boolean isWindows() {
		String osName = System.getProperty("os.name");
		return osName!=null && osName.toLowerCase(Locale.ROOT).contains("windows");
	}

	/**
	 * Additional file openers may be registered to the application context.
	 */
	@FunctionalInterface
	public static interface FileOpener {
		/**
		 * Gets the command that will open the given file.
		 *
		 * @return  The command or null to fall-through to default behavior.
		 */
		String[] getCommand(java.io.File resourceFile) throws IOException;
	}

	/**
	 * Registers a file opener.
	 * 
	 * @param  extensions  The simple extensions, in lowercase, not including the dot, such as "dia"
	 */
	public static void addFileOpener(ServletContext servletContext, FileOpener fileOpener, String ... extensions) {
		ConcurrentMap<String,FileOpener> fileOpeners = getFileOpeners(servletContext);
		for(String extension : extensions) {
			if(fileOpeners.putIfAbsent(extension, fileOpener) != null) {
				throw new IllegalStateException("File opener already registered: " + extension);
			}
		}
	}

	/**
	 * Removes file openers.
	 * 
	 * @param  extensions  The simple extensions, in lowercase, not including the dot, such as "dia"
	 */
	public static void removeFileOpener(ServletContext servletContext, String ... extensions) {
		ConcurrentMap<String,FileOpener> fileOpeners = getFileOpeners(servletContext);
		for(String extension : extensions) {
			fileOpeners.remove(extension);
		}
	}

	// TODO: Should this only allow open of published books?
	public static void openFile(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		DomainName domain,
		Path book,
		final Path path
	) throws ServletException, IOException, SkipPageException {
		// Only allow from localhost and when open enabled
		if(!isAllowed(servletContext, request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			throw ServletUtil.SKIP_PAGE_EXCEPTION;
		} else {
			final String[] command;
			ResourceRef resourceRef = ResourceRefResolver.getResourceRef(servletContext, request, domain, book, path.toString());
			Book bookObj = SemanticCMS.getInstance(servletContext).getBook(resourceRef.getBookRef());
			if(bookObj.isAccessible()) throw new FileNotFoundException("Book is inaccessible: " + resourceRef);
			ResourceStore resourceStore = bookObj.getResources();
			if(!resourceStore.isAvailable()) throw new FileNotFoundException("Resource store is unavailable: " + resourceRef);
			java.io.File resourceFile = resourceStore.getResource(resourceRef.getPath()).getFile();
			if(resourceFile == null) throw new FileNotFoundException("Resource is not a local file: " + resourceRef);
			if(resourceFile.isDirectory()) {
				command = new String[] {
					// TODO: What is good windows path?
					//isWindows()
					//	? "C:\\Program Files (x86)\\OpenOffice 4\\program\\swriter.exe"
					"/usr/bin/dolphin",
					resourceFile.getCanonicalPath()
				};
			} else {
				// Open the file with the appropriate application based on extension
				String extension = FileUtils.getExtension(resourceFile.getName()).toLowerCase(Locale.ROOT);
				// Check registered file openers first
				FileOpener fileOpener = getFileOpeners(servletContext).get(extension);
				if(fileOpener != null) {
					command = fileOpener.getCommand(resourceFile);
				} else {
					// Use default behavior
					switch(extension) {
						case "gif" :
						case "jpg" :
						case "jpeg" :
						case "png" :
							command = new String[] {
								isWindows()
									? "C:\\Program Files (x86)\\OpenOffice 4\\program\\swriter.exe"
									: "/usr/bin/gwenview",
								resourceFile.getCanonicalPath()
							};
							break;
						case "doc" :
						case "docx" :
						case "odt" :
							command = new String[] {
								isWindows()
									? "C:\\Program Files (x86)\\OpenOffice 4\\program\\swriter.exe"
									: "/usr/bin/libreoffice",
								"--writer",
								resourceFile.getCanonicalPath()
							};
							break;
						case "csv" :
						case "ods" :
						case "sxc" :
						case "xls" :
							command = new String[] {
								isWindows()
									? "C:\\Program Files (x86)\\OpenOffice 4\\program\\scalc.exe"
									: "/usr/bin/libreoffice",
								"--calc",
								resourceFile.getCanonicalPath()
							};
							break;
						case "pdf" :
							command = new String[] {
								isWindows()
									? "C:\\Program Files (x86)\\Adobe\\Reader 11.0\\Reader\\AcroRd32.exe"
									: "/usr/bin/okular",
								resourceFile.getCanonicalPath()
							};
							break;
						case "c" :
						case "csh" :
						case "h" :
						case "java" :
						case "jsp" :
						case "jspx" :
						case "sh" :
						case "txt" :
						case "xml" :
							if(isWindows()) {
								command = new String[] {
									"C:\\Program Files\\NetBeans 7.4\\bin\\netbeans64.exe",
									"--open",
									resourceFile.getCanonicalPath()
								};
							} else {
								command = new String[] {
									//"/usr/bin/kwrite",
									"/opt/netbeans/bin/netbeans",
									"--jdkhome",
									getJdkPath(),
									"--open",
									resourceFile.getCanonicalPath()
								};
							}
							break;
						case "dia" :
							command = new String[] {
								isWindows()
									? "C:\\Program Files (x86)\\Dia\\bin\\diaw.exe"
									: "/usr/bin/dia",
								resourceFile.getCanonicalPath()
							};
							break;
						case "zip" :
							if(isWindows()) {
								command = new String[] {
									resourceFile.getCanonicalPath()
								};
							} else {
								command = new String[] {
									"/usr/bin/dolphin",
									resourceFile.getCanonicalPath()
								};
							}
							break;
						case "mp3" :
						case "wma" :
							command = new String[] {
								isWindows()
									? "C:\\Program Files\\VideoLAN\\VLC.exe"
									: "/usr/bin/vlc",
								resourceFile.getCanonicalPath()
							};
							break;
						default :
							throw new IllegalArgumentException("Unsupprted file type by extension: " + extension);
					}
				}
			}
			// Start the process
			final Process process = Runtime.getRuntime().exec(command);
			// Result is watched in the background only
			new Thread(() -> {
				try {
					final ProcessResult result = ProcessResult.getProcessResult(process);
					int exitVal = result.getExitVal();
					if(exitVal != 0) {
						logger.log(Level.SEVERE, "Non-zero exit status from \"{0}\": {1}", new Object[]{path, exitVal});
					}
					String stdErr = result.getStderr();
					if(!stdErr.isEmpty()) {
						logger.log(Level.SEVERE, "Standard error from \"{0}\":\n{1}", new Object[]{path, stdErr});
					}
					if(logger.isLoggable(Level.INFO)) {
						String stdOut = result.getStdout();
						if(!stdOut.isEmpty()) {
							logger.log(Level.INFO, "Standard output from \"{0}\":\n{1}", new Object[]{path, stdOut});
						}
					}
				} catch(IOException e) {
					logger.log(Level.SEVERE, null, e);
				}
			}).start();
		}
	}

	/**
	 * Make no instances.
	 */
	private OpenFile() {
	}
}
