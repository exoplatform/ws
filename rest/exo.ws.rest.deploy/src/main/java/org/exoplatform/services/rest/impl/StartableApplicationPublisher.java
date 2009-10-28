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

import java.lang.annotation.Annotation;
import java.util.List;

import javax.ws.rs.core.Application;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.rest.DependencySupplier;
import org.exoplatform.services.rest.FieldInjector;
import org.exoplatform.services.rest.Inject;
import org.exoplatform.services.rest.Parameter;
import org.exoplatform.services.rest.ResourceBinder;
import org.picocontainer.Startable;

/**
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class StartableApplicationPublisher extends ApplicationPublisher implements DependencySupplier, Startable
{

   private ExoContainer container;

   public StartableApplicationPublisher(ResourceBinder resources, ExoContainerContext containerContext)
   {
      super(resources, ProviderBinder.getInstance());
      container = containerContext.getContainer();
   }

   /**
    * {@inheritDoc}
    */
   public Object getComponent(Parameter parameter)
   {
      // Container can different.
      ExoContainer container = ExoContainerContext.getCurrentContainer();

      if (parameter instanceof FieldInjector)
      {
         for (Annotation a : parameter.getAnnotations())
         {
            // Do not process fields without annotation Inject
            if (a.annotationType() == Inject.class)
            {
               return container.getComponentInstanceOfType(parameter.getParameterClass());
            }
         }
      }
      return container.getComponentInstanceOfType(parameter.getParameterClass());
   }

   /**
    * {@inheritDoc}
    */
   @SuppressWarnings("unchecked")
   public void start()
   {
      List<Application> applications = container.getComponentInstancesOfType(Application.class);
      for (Application application : applications)
         publish(application);
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
   }

}
