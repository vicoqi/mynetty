package com.vic.mynetty.rpc_common.service;

import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.netty_server.ServerContext;
import com.vic.mynetty.common.service.AbstractService;
import lombok.Setter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractServerService extends AbstractService {
	@Setter
	protected ServerContext serverContext;

	public Object request(Mapping.Model mapping, Object[] parameters) throws Exception{
		return null;
	};


	protected abstract boolean isServiceClazz(Class<?> candidate);
	
	protected abstract void initCandidate(Class<?> candidate);

	//配置需要扫的包名
	protected abstract String[] getScanningPackages();

	public Resource[] scan(ClassLoader loader, String packageName) throws IOException {
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(loader);
		String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
				+ ClassUtils.convertClassNameToResourcePath(packageName) + "/**/*.class";
		Resource[] resources = resolver.getResources(pattern);
		return resources;
	}

	public Class<?> loadClass(ClassLoader loader, MetadataReaderFactory readerFactory, Resource resource) {
		try {
			MetadataReader reader = readerFactory.getMetadataReader(resource);
			return ClassUtils.forName(reader.getClassMetadata().getClassName(), loader);
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	protected void startInner() {
		ClassLoader loader = this.getClass().getClassLoader();
		MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(loader);
		try {
			List<Resource> resources = new ArrayList<Resource>();
			String[] scanPackages = getScanningPackages();
			if (scanPackages != null) {
				for (String packageName : scanPackages) {
					resources.addAll(Arrays.asList(scan(loader, packageName)));
				}
				for (Resource resource : resources) {
					final Class<?> candidate = loadClass(loader, metadataReaderFactory, resource);
					if (candidate != null && isServiceClazz(candidate)) {
						initCandidate(candidate);
					}
				}
			}
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
}
