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
package org.exoplatform.services.rest.impl;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.DependencySupplier;
import org.exoplatform.services.rest.Parameter;
import org.exoplatform.services.rest.ResourceBinder;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.picocontainer.Startable;

import java.util.List;

import javax.ws.rs.core.Application;

/**
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class RestInitializer extends ApplicationDeployer implements DependencySupplier, Startable
{

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger(RestInitializer.class.getName());

   @SuppressWarnings("unchecked")
   public RestInitializer(ResourceBinder resources, InitParams initParams, ExoContainerContext containerContext)
   {
      super(resources, ProviderBinder.getInstance());
      ExoContainer container = containerContext.getContainer();
      if (initParams != null)
      {
         for (Object cl : initParams.getValuesParam("ws.rest.components").getValues())
         {
            try
            {
               deploy(Class.forName((String)cl));
            }
            catch (ClassNotFoundException e)
            {
               LOG.error("Failed load class " + cl, e);
            }
         }
      }
      List<Application> applications = container.getComponentInstancesOfType(Application.class);
      for (Application a : applications)
      {
         deploy(a);
      }
      for (Object resource : container.getComponentInstancesOfType(ResourceContainer.class))
      {
         deploy(resource);
      }
   }

   public Object getInstanceOfType(Parameter parameter)
   {
      return null;
   }

   public void start()
   {
   }

   public void stop()
   {
   }
}
