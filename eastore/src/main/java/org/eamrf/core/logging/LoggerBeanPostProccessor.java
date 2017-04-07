/**
 * 
 */
package org.eamrf.core.logging;

import java.lang.reflect.Field;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * Auto injects the underlying implementation of logger into the bean with field
 * having annotation <code>InjectLogger</code>.
 * 
 * @see org.lenzi.fstore.core.stereotype.InjectLogger
 * 
 * @author slenzi
 */
@Component
public class LoggerBeanPostProccessor implements BeanPostProcessor {

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(), new FieldCallback() {
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
		
				// make the field accessible if defined private
				ReflectionUtils.makeAccessible(field);
				
				if(field.getAnnotation(InjectLogger.class) != null && field.get(bean) == null){
					Logger log = LoggerFactory.getLogger(bean.getClass());
					field.set(bean, log);			
				}
		
			}
		});		
		return bean;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(), new FieldCallback() {
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
			
				// make the field accessible if defined private
				ReflectionUtils.makeAccessible(field);
				
				if (field.getAnnotation(InjectLogger.class) != null) {
					Logger log = LoggerFactory.getLogger(bean.getClass());
					field.set(bean, log);
				}
				
			}
		});
		return bean;
	}

}
