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

import org.exoplatform.services.rest.DependencySupplier;
import org.exoplatform.services.rest.RequestHandler;
import org.exoplatform.services.rest.ResourceBinder;
import org.exoplatform.services.rest.impl.ApplicationDeployer;
import org.exoplatform.services.rest.impl.ProviderBinder;
import org.exoplatform.services.rest.impl.ResourceBinderImpl;
import org.exoplatform.services.rest.impl.RequestHandlerImpl;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.core.Application;

/**
 * Initialize required components of JAX-RS framework and deploy single JAX-RS application.
 * 
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class RestInitializedListener implements ServletContextListener
{

   /**
    * {@inheritDoc}
    */
   public void contextDestroyed(ServletContextEvent event)
   {
   }

   /**
    * {@inheritDoc}
    */
   public void contextInitialized(ServletContextEvent event)
   {
      String dependencyInjectorFQN = event.getServletContext().getInitParameter(DependencySupplier.class.getName());
      DependencySupplier dependencySupplier = null;
      if (dependencyInjectorFQN != null)
      {
         try
         {
            Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass(dependencyInjectorFQN.trim());
            dependencySupplier = (DependencySupplier)cl.newInstance();
         }
         catch (ClassNotFoundException cnfe)
         {
            throw new RuntimeException(cnfe);
         }
         catch (InstantiationException ie)
         {
            throw new RuntimeException(ie);
         }
         catch (IllegalAccessException iae)
         {
            throw new RuntimeException(iae);
         }
      }

      ResourceBinder resources = new ResourceBinderImpl();
      ApplicationDeployer deployer = new ApplicationDeployer(resources, ProviderBinder.getInstance());

      String applicationFQN = event.getServletContext().getInitParameter("javax.ws.rs.Application");
      if (applicationFQN != null)
      {
         try
         {
            Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass(applicationFQN.trim());
            Application application = (Application)cl.newInstance();
            deployer.deploy(application);
         }
         catch (ClassNotFoundException cnfe)
         {
            throw new RuntimeException(cnfe);
         }
         catch (InstantiationException ie)
         {
            throw new RuntimeException(ie);
         }
         catch (IllegalAccessException iae)
         {
            throw new RuntimeException(iae);
         }
      }

      RequestHandler handler = new RequestHandlerImpl(resources, dependencySupplier);
      event.getServletContext().setAttribute(RequestHandler.class.getName(), handler);
   }

}
