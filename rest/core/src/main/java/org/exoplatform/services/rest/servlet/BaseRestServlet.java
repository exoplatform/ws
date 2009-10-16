/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.rest.servlet;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.RequestHandler;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class BaseRestServlet extends HttpServlet
{

   private static final long serialVersionUID = -8234561611241680339L;

   private static final Log LOG = ExoLogger.getLogger(BaseRestServlet.class.getName());

   private RequestHandler requestHandler;

   private ServletConfig servletConfig;

   protected RequestHandler getRequestHandler()
   {
      return requestHandler;
   }

   public void init(ServletConfig servletConfig)
   {
      this.servletConfig = servletConfig;
      requestHandler = (RequestHandler)servletConfig.getServletContext().getAttribute(RequestHandler.class.getName());
   }

   public void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException,
      ServletException
   {

      EnvironmentContext env = new EnvironmentContext();
      env.put(HttpServletRequest.class, httpRequest);
      env.put(HttpServletResponse.class, httpResponse);
      env.put(ServletConfig.class, servletConfig);
      env.put(ServletContext.class, servletConfig.getServletContext());

      try
      {
         EnvironmentContext.setCurrent(env);
         ServletContainerRequest request = new ServletContainerRequest(httpRequest);
         ContainerResponse response = new ContainerResponse(new ServletContainerResponseWriter(httpResponse));
         getRequestHandler().handleRequest(request, response);
      }
      catch (Exception e)
      {
         LOG.error(e);
         throw new ServletException(e);
      }
      finally
      {
         EnvironmentContext.setCurrent(null);
      }
   }

}
