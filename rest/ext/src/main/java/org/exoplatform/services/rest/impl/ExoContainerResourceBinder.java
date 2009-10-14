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

import java.util.List;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.resource.ResourceDescriptorValidator;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.rest.resource.ResourceDescriptorVisitor;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class ExoContainerResourceBinder extends BaseResourceBinder
{

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger(ExoContainerResourceBinder.class.getName());

   /**
    * Validator.
    */
   private final ResourceDescriptorVisitor rdv = ResourceDescriptorValidator.getInstance();

   /**
    * @see RuntimeDelegate
    */
   private final RuntimeDelegate rd;

   public ExoContainerResourceBinder()
   {
      rd = new RuntimeDelegateImpl();
      RuntimeDelegate.setInstance(rd);
   }
   
   /**
    * @param containerContext eXo container context
    * @throws Exception if can't set instance of {@link RuntimeDelegate}
    */
   @SuppressWarnings("unchecked")
   public ExoContainerResourceBinder(ExoContainerContext containerContext) throws Exception
   {
      // Initialize RuntimeDelegate instance
      // This is first component in life cycle what needs.
      // TODO better solution to initialize RuntimeDelegate
      rd = new RuntimeDelegateImpl();
      RuntimeDelegate.setInstance(rd);

      ExoContainer container = containerContext.getContainer();

      // Lookup Applications
      List<Application> al = container.getComponentInstancesOfType(Application.class);
      for (Application a : al)
      {
         try
         {
            addApplication(a);
         }
         catch (Exception e)
         {
            LOG.error("Failed add JAX-RS application " + a.getClass().getName(), e);
         }
      }

      // Lookup all object which implements ResourceContainer interface and
      // process them to be add as root resources.
      for (Object resource : container.getComponentInstancesOfType(ResourceContainer.class))
      {
         bind(resource);
      }

   }

}
