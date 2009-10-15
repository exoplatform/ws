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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.Filter;
import org.exoplatform.services.rest.ObjectFactory;
import org.exoplatform.services.rest.PerRequestObjectFactory;
import org.exoplatform.services.rest.RequestFilter;
import org.exoplatform.services.rest.ResourceBinder;
import org.exoplatform.services.rest.ResponseFilter;
import org.exoplatform.services.rest.SingletonObjectFactory;
import org.exoplatform.services.rest.impl.resource.AbstractResourceDescriptorImpl;
import org.exoplatform.services.rest.impl.resource.ResourceDescriptorValidator;
import org.exoplatform.services.rest.method.MethodInvokerFilter;
import org.exoplatform.services.rest.resource.AbstractResourceDescriptor;
import org.exoplatform.services.rest.resource.ResourceDescriptorVisitor;
import org.exoplatform.services.rest.uri.UriPattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.RuntimeDelegate;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class BaseResourceBinder implements ResourceBinder
{

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger(BaseResourceBinder.class.getName());

   private static final Comparator<ObjectFactory<AbstractResourceDescriptor>> RESOURCE_COMPARATOR =
      new ResourceComparator();

   /**
    * Compare two {@link SingletonResourceFactory}.
    */
   private static final class ResourceComparator implements Comparator<ObjectFactory<AbstractResourceDescriptor>>
   {
      /**
       * Compare two ResourceClass for order.
       * 
       * @param o1 first ResourceClass to be compared
       * @param o2 second ResourceClass to be compared
       * @return positive , zero or negative dependent of {@link UriPattern}
       *         comparison
       * @see Comparator#compare(Object, Object)
       * @see UriPattern
       * @see UriPattern#URIPATTERN_COMPARATOR
       */
      public int compare(ObjectFactory<AbstractResourceDescriptor> o1, ObjectFactory<AbstractResourceDescriptor> o2)
      {
         return UriPattern.URIPATTERN_COMPARATOR.compare(o1.getObjectModel().getUriPattern(), o2.getObjectModel()
            .getUriPattern());
      }
   };

   /**
    * Root resource descriptors.
    */
   private final List<ObjectFactory<AbstractResourceDescriptor>> rootResources =
      new ArrayList<ObjectFactory<AbstractResourceDescriptor>>();

   /**
    * Validator.
    */
   private final ResourceDescriptorVisitor rdv = ResourceDescriptorValidator.getInstance();

   private int size = 0;

   public BaseResourceBinder()
   {
      // Initialize RuntimeDelegate instance
      // This is first component in life cycle what needs.
      // TODO better solution to initialize RuntimeDelegate
      RuntimeDelegate rd = new RuntimeDelegateImpl();
      RuntimeDelegate.setInstance(rd);
   }
   
   /**
    * @param application Application
    * @see Application
    */
   @SuppressWarnings("unchecked")
   public void addApplication(Application application)
   {
      ProviderBinder providers = ProviderBinder.getInstance();
      for (Object obj : application.getSingletons())
      {
         if (obj.getClass().getAnnotation(Provider.class) != null)
         {
            // singleton provider
            if (obj instanceof ContextResolver)
            {
               providers.addContextResolver((ContextResolver)obj);
            }
            if (obj instanceof ExceptionMapper)
            {
               providers.addExceptionMapper((ExceptionMapper)obj);
            }
            if (obj instanceof MessageBodyReader)
            {
               providers.addMessageBodyReader((MessageBodyReader)obj);
            }
            if (obj instanceof MessageBodyWriter)
            {
               providers.addMessageBodyWriter((MessageBodyWriter)obj);
            }
         }
         else if (obj.getClass().getAnnotation(Filter.class) != null)
         {
            // singleton filter
            if (obj instanceof MethodInvokerFilter)
            {
               providers.addMethodInvokerFilter((MethodInvokerFilter)obj);
            }
            if (obj instanceof RequestFilter)
            {
               providers.addRequestFilter((RequestFilter)obj);
            }
            if (obj instanceof ResponseFilter)
            {
               providers.addResponseFilter((ResponseFilter)obj);
            }
         }
         else
         {
            bind(obj); // singleton resource
         }
      }
      for (Class clazz : application.getClasses())
      {
         if (clazz.getAnnotation(Provider.class) != null)
         {
            // per-request provider
            if (ContextResolver.class.isAssignableFrom(clazz))
            {
               providers.addContextResolver(clazz);
            }
            if (ExceptionMapper.class.isAssignableFrom(clazz))
            {
               providers.addExceptionMapper(clazz);
            }
            if (MessageBodyReader.class.isAssignableFrom(clazz))
            {
               providers.addMessageBodyReader(clazz);
            }
            if (MessageBodyWriter.class.isAssignableFrom(clazz))
            {
               providers.addMessageBodyWriter(clazz);
            }
         }
         else if (clazz.getAnnotation(Filter.class) != null)
         {
            // per-request filter
            if (MethodInvokerFilter.class.isAssignableFrom(clazz))
            {
               providers.addMethodInvokerFilter(clazz);
            }
            if (RequestFilter.class.isAssignableFrom(clazz))
            {
               providers.addRequestFilter(clazz);
            }
            if (ResponseFilter.class.isAssignableFrom(clazz))
            {
               providers.addResponseFilter(clazz);
            }
         }
         else
         {
            bind(clazz); // per-request resource
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean bind(final Object resource)
   {
      final Path path = resource.getClass().getAnnotation(Path.class);

      AbstractResourceDescriptor descriptor = null;
      if (path != null)
      {
         try
         {
            descriptor = new AbstractResourceDescriptorImpl(resource);
         }
         catch (Exception e)
         {
            String msg = "Unexpected error occurs when process resource class " + resource.getClass().getName();
            LOG.error(msg, e);
            return false;
         }
      }
      else
      {
         String msg =
            "Resource class " + resource.getClass().getName() + " it is not root resource. "
               + "Path annotation javax.ws.rs.Path is not specified for this class.";
         LOG.warn(msg);
         return false;
      }

      // validate AbstractResourceDescriptor
      try
      {
         descriptor.accept(rdv);
      }
      catch (Exception e)
      {
         LOG.error("Validation of root resource failed. ", e);
         return false;
      }

      synchronized (rootResources)
      {
         // check does exist other resource with the same URI pattern
         for (ObjectFactory<AbstractResourceDescriptor> exist : rootResources)
         {
            if (exist.getObjectModel().getUriPattern().equals(descriptor.getUriPattern()))
            {
               String msg =
                  "Resource class " + descriptor.getObjectClass().getName() + " can't be registered. Resource class "
                     + exist.getClass().getName() + " with the same pattern "
                     + exist.getObjectModel().getUriPattern().getTemplate() + " already registered.";
               LOG.warn(msg);
               return false;
            }
         }

         // Singleton resource
         ObjectFactory<AbstractResourceDescriptor> res =
            new SingletonObjectFactory<AbstractResourceDescriptor>(descriptor, resource);
         rootResources.add(res);
         Collections.sort(rootResources, RESOURCE_COMPARATOR);
         LOG.info("Bind new resource " + res.getObjectModel().getUriPattern().getTemplate() + " : "
            + descriptor.getObjectClass());
      }
      size++;
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean bind(final Class<?> resourceClass)
   {
      final Path path = resourceClass.getAnnotation(Path.class);

      AbstractResourceDescriptor descriptor = null;
      if (path != null)
      {
         try
         {
            descriptor = new AbstractResourceDescriptorImpl(resourceClass);
         }
         catch (Exception e)
         {
            String msg = "Unexpected error occurs when process resource class " + resourceClass.getName();
            LOG.error(msg, e);
            return false;
         }
      }
      else
      {
         String msg =
            "Resource class " + resourceClass.getName() + " it is not root resource. "
               + "Path annotation javax.ws.rs.Path is not specified for this class.";
         LOG.warn(msg);
         return false;
      }

      // validate AbstractResourceDescriptor
      try
      {
         descriptor.accept(rdv);
      }
      catch (Exception e)
      {
         LOG.error("Validation of root resource failed. ", e);
         return false;
      }

      synchronized (rootResources)
      {
         // check does exist other resource with the same URI pattern
         for (ObjectFactory<AbstractResourceDescriptor> exist : rootResources)
         {
            AbstractResourceDescriptor existDescriptor = exist.getObjectModel();
            if (exist.getObjectModel().getUriPattern().equals(descriptor.getUriPattern()))
            {

               String msg =
                  "Resource class " + descriptor.getObjectClass().getName() + " can't be registered. Resource class "
                     + existDescriptor.getObjectClass().getName() + " with the same pattern "
                     + exist.getObjectModel().getUriPattern().getTemplate() + " already registered.";
               LOG.warn(msg);
               return false;
            }
         }
         // per-request resource
         ObjectFactory<AbstractResourceDescriptor> res =
            new PerRequestObjectFactory<AbstractResourceDescriptor>(descriptor);
         rootResources.add(res);
         Collections.sort(rootResources, RESOURCE_COMPARATOR);
         LOG.info("Bind new resource " + res.getObjectModel().getUriPattern().getRegex() + " : " + resourceClass);
      }
      size++;
      return true;
   }

   /**
    * {@inheritDoc}
    */
   @SuppressWarnings("unchecked")
   public boolean unbind(Class clazz)
   {
      synchronized (rootResources)
      {
         Iterator<ObjectFactory<AbstractResourceDescriptor>> i = rootResources.iterator();
         while (i.hasNext())
         {
            ObjectFactory<AbstractResourceDescriptor> res = i.next();
            Class c = res.getObjectModel().getObjectClass();
            if (clazz.equals(c))
            {
               i.remove();
               LOG.info("Remove ResourceContainer " + res.getObjectModel().getUriPattern().getTemplate() + " : " + c);
               size--;
               return true;
            }
         }
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean unbind(String uriTemplate)
   {
      synchronized (rootResources)
      {
         Iterator<ObjectFactory<AbstractResourceDescriptor>> i = rootResources.iterator();
         while (i.hasNext())
         {
            ObjectFactory<AbstractResourceDescriptor> res = i.next();
            String t = res.getObjectModel().getUriPattern().getTemplate();
            if (t.equals(uriTemplate))
            {
               i.remove();
               LOG.info("Remove ResourceContainer " + res.getObjectModel().getUriPattern().getTemplate());
               size--;
               return true;
            }
         }
         return false;
      }
   }

   /**
    * Clear the list of ResourceContainer description.
    */
   public void clear()
   {
      rootResources.clear();
      size = 0;
   }

   /**
    * {@inheritDoc}
    */
   public List<ObjectFactory<AbstractResourceDescriptor>> getResources()
   {
      return rootResources;
   }

   /**
    * {@inheritDoc}
    */
   public int getSize()
   {
      return size;
   }

   /**
    * @return all registered root resources
    */
   @Deprecated
   public List<AbstractResourceDescriptor> getRootResources()
   {
      List<AbstractResourceDescriptor> l = new ArrayList<AbstractResourceDescriptor>(rootResources.size());
      synchronized (rootResources)
      {
         for (ObjectFactory<AbstractResourceDescriptor> f : rootResources)
            l.add(f.getObjectModel());
      }
      return l;
   }

}
