package com.vic.mynetty.common.declarative;

import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.route.Route;
import com.vic.mynetty.common.route.RouteMatcher;
import lombok.Data;

import java.lang.annotation.*;
import java.lang.reflect.Method;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Mapping {
	String path() default "";
	Mode mode() default Mode.NAN;
	Type type() default Type.NAN;
	@Data
	public class Model implements Route {
		private Path path;
		private Mode mode;
		private Type type;
	}
	public class Matcher implements RouteMatcher<Mapping.Model> {
		private Path.Matcher pathMatcher;
		private Mode mode;
		private Type type;
		public Matcher(Path.Matcher pathMatcher, Mode mode, Type type) {
			this.pathMatcher = pathMatcher;
			this.mode = mode;
		}

		@Override
		public boolean matches(Model route) {
			return pathMatcher.matches(route.getPath()) 
					&& this.mode == route.getMode()
					&& this.type == route.getType();
		}
		
	}
	public class Interpreter implements AnnotationInterpreter<Model> {
		public Model interpret(Class<?> clz, Method method) {
			String path = null;
			Mode mode = null;
			Type type = null;
			
			boolean isAnnotated = false;
			
			Mapping clzMappingAnno = clz.getAnnotation(Mapping.class);
			if (clzMappingAnno != null) {
				isAnnotated = true;
				path = clzMappingAnno.path();
				mode = clzMappingAnno.mode();
				type = clzMappingAnno.type();
			}
			
			Mapping methodMappingAnno = method.getAnnotation(Mapping.class);
			if (methodMappingAnno != null) {
				isAnnotated = true;
				path += methodMappingAnno.path();
				mode = methodMappingAnno.mode();
				if (methodMappingAnno.type() != Type.NAN) {
					type = methodMappingAnno.type();
				}
			}
			
			Model mapping = null;
			if (isAnnotated) {
				mapping = new Model();
				mapping.setPath(new Path(path));
				mapping.setMode(mode);
				mapping.setType(type);
			}
			return mapping;
		}
	}
}
