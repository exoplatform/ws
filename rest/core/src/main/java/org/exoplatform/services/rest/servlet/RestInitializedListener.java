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
import org.exoplatform.services.rest.DependencySupplier;
import org.exoplatform.services.rest.Filter;
import org.exoplatform.services.rest.RequestHandler;
import org.exoplatform.services.rest.ResourceBinder;
import org.exoplatform.services.rest.impl.ApplicationDeployer;
import org.exoplatform.services.rest.impl.ProviderBinder;
import org.exoplatform.services.rest.impl.ResourceBinderImpl;
import org.exoplatform.services.rest.impl.RequestHandlerImpl;
import org.scannotation.AnnotationDB;
import org.scannotation.WarUrlFinder;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

/**
 * Initialize required components of JAX-RS framework and deploy single JAX-RS application.
 * 
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class RestInitializedListener implements ServletContextListener
{

   private static final Log LOG = ExoLogger.getLogger(RestInitializedListener.class);

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
      boolean scan = Boolean.parseBoolean(event.getServletContext().getInitParameter("ws.rs.scan.components"));
      String dependencyInjectorFQN = event.getServletContext().getInitParameter(DependencySupplier.class.getName());

      ResourceBinder resources = new ResourceBinderImpl();
      ApplicationDeployer deployer = new ApplicationDeployer(resources, ProviderBinder.getInstance());

      String applicationFQN = event.getServletContext().getInitParameter("javax.ws.rs.Application");
      if (applicationFQN != null)
      {
         if (scan)
         {
            String msg = "Scan of rest components is disabled cause to specified 'javax.ws.rs.Application'.";
            LOG.warn(msg);
         }
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
      else if (scan)
      {
         URL classes = WarUrlFinder.findWebInfClassesPath(event);
         URL[] libs = WarUrlFinder.findWebInfLibClasspaths(event);
         AnnotationDB annotationDB = new AnnotationDB();
         // Disable processing of API, implementation and JAX-RS packages
         annotationDB.setIgnoredPackages(new String[]{"org.exoplatform.services.rest", "javax.ws.rs"});
         try
         {
            if (classes != null)
               annotationDB.scanArchives(classes);
            annotationDB.scanArchives(libs);

            Map<String, Set<String>> results = annotationDB.getAnnotationIndex();
            for (String annotation : new String[]{Path.class.getName(), Provider.class.getName(),
               Filter.class.getName()})
            {
               if (results.get(annotation) != null)
               {
                  for (String fqn : results.get(annotation))
                  {
                     try
                     {
                        Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass(fqn);
                        if (cl.isInterface() || Modifier.isAbstract(cl.getModifiers()))
                        {
                           LOG.info("Skip abstract class or interface " + fqn);
                           continue;
                        }
                        deployer.deploy(cl);
                        LOG.info("Deployed component: " + fqn);
                     }
                     catch (ClassNotFoundException e)
                     {
                        throw new RuntimeException(e);
                     }
                  }
               }
            }
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }

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

      RequestHandler handler = new RequestHandlerImpl(resources, dependencySupplier);
      event.getServletContext().setAttribute(RequestHandler.class.getName(), handler);
   }

}
