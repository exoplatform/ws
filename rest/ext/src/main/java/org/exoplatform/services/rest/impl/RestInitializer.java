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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBException;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.RequestFilter;
import org.exoplatform.services.rest.RequestHandler;
import org.exoplatform.services.rest.ResponseFilter;
import org.exoplatform.services.rest.impl.provider.JAXBContextResolver;
import org.exoplatform.services.rest.method.MethodInvokerFilter;
import org.exoplatform.services.rest.provider.EntityProvider;
import org.picocontainer.Startable;

/**
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class RestInitializer implements Startable
{

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger(RestInitializer.class.getName());

   @SuppressWarnings("unchecked")
   public RestInitializer(RequestHandler requestHandler, InitParams initParams)
   {

      ProviderBinder providers = ProviderBinder.getInstance();

      if (initParams != null)
      {
         for (Object cl : initParams.getValuesParam("ws.rs.request.filter").getValues())
         {
            try
            {
               providers.addRequestFilter((Class<? extends RequestFilter>)Class.forName((String)cl));
            }
            catch (ClassNotFoundException e)
            {
               LOG.error("Failed load class " + cl, e);
            }
         }
      }
      for (Object cl : initParams.getValuesParam("ws.rs.response.filter").getValues())
      {
         try
         {
            providers.addResponseFilter((Class<? extends ResponseFilter>)Class.forName((String)cl));
         }
         catch (ClassNotFoundException e)
         {
            LOG.error("Failed load class " + cl, e);
         }
      }
      for (Object cl : initParams.getValuesParam("ws.rs.method.filter").getValues())
      {
         try
         {
            providers.addMethodInvokerFilter((Class<? extends MethodInvokerFilter>)Class.forName((String)cl));
         }
         catch (ClassNotFoundException e)
         {
            LOG.error("Failed load class " + cl, e);
         }
      }
      for (Object cl : initParams.getValuesParam("ws.rs.entity.provider").getValues())
      {
         try
         {
            Class<? extends EntityProvider> prov = (Class<? extends EntityProvider>)Class.forName((String)cl);
            providers.addMessageBodyReader(prov);
            providers.addMessageBodyWriter(prov);
         }
         catch (ClassNotFoundException e)
         {
            LOG.error("Failed load class " + cl, e);
         }
      }
      for (Object cl : initParams.getValuesParam("ws.rs.jaxb.context").getValues())
      {
         try
         {
            ContextResolver<JAXBContextResolver> resolver =
               providers.getContextResolver(JAXBContextResolver.class, MediaType.WILDCARD_TYPE);
            if (resolver == null)
            {
               LOG.error("Not found JAXBContextResolver.");
            }
            else
            {
               JAXBContextResolver contextResolver = resolver.getContext(null);
               contextResolver.createJAXBContext((Class<?>)Class.forName((String)cl));
            }
         }
         catch (ClassNotFoundException e)
         {
            LOG.error("Failed load class " + cl, e);
         }
         catch (JAXBException jaxbe)
         {
            LOG.error("Failed add JAXBContext for class " + cl, jaxbe);
         }
      }
      
   }

   public void start()
   {
   }

   public void stop()
   {
   }
}
