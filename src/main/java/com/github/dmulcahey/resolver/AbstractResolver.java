package com.github.dmulcahey.resolver;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/*
 * Flow: 
 * 
 * Execute preresolution tests on input
 * Perform preresolution activities using valid input
 * Invoke doResolution if all preresolution tests pass
 * Execute postresolution tests passing output from doResolution
 * Perform postresolution activities using valid output
 * 
 */
@Slf4j
public abstract class AbstractResolver<I,O> implements Resolver<I,O> {
	private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder().addUrls(ClasspathHelper.forManifest(ClasspathHelper.forClassLoader())));
	private Set<ResolutionActivity<I>> preresolutionActivities;
	private Set<ResolutionActivity<O>> postresolutionActivities;
	private Set<ResolutionTest<I>> preresolutionTests;
	private Set<ResolutionTest<O>> postresolutionTests;
	
	public AbstractResolver(){
		initialize();
	}

	protected abstract O doResolution(final I input);
	
	@Override
	public final O resolve(final I input) {
		String thisClassName = this.getClass().getSimpleName();
		log.trace("{} resolve start", thisClassName);
		CombinedResolutionTestResult preresolutionTestResults = new CombinedResolutionTestResult();
		int numPreresolutionTests = getPreresolutionTests().size();
		if(numPreresolutionTests > 0){
			log.debug("Executing {} preresolution tests in {}", numPreresolutionTests, thisClassName);
			List<ResolutionTest<I>> sortedPreresolutionTests = Lists.newArrayList(getPreresolutionTests());
			Collections.sort(sortedPreresolutionTests, Collections.reverseOrder(new ResolutionOrdering<ResolutionTest<I>>()));
			for(ResolutionTest<I> resolutionTest : sortedPreresolutionTests){
				String testClassName = resolutionTest.getClass().getSimpleName();
				log.debug("Executing preresolution test: {}", testClassName);
				preresolutionTestResults.addResolutionTestResult(resolutionTest.execute(input), testClassName);
			}
			handlePreresolutionTestResults(preresolutionTestResults);
		}
		
		int numPreresolutionActivities = getPreresolutionActivities().size();
		if(numPreresolutionActivities > 0){
			log.debug("Executing {} preresolution activities in {}", numPreresolutionActivities , thisClassName);
			List<ResolutionActivity<I>> sortedPreresolutionActivities = Lists.newArrayList(getPreresolutionActivities());
			Collections.sort(sortedPreresolutionActivities, Collections.reverseOrder(new ResolutionOrdering<ResolutionActivity<I>>()));
			for(ResolutionActivity<I> resolutionActivity : sortedPreresolutionActivities){
				log.debug("Executing preresolution activity: {}", resolutionActivity.getClass().getSimpleName());
				resolutionActivity.perform(input);
			}
		}
		
		log.trace("{} resolving...", thisClassName);
		O output = doResolution(input);
		
		CombinedResolutionTestResult postresolutionTestResults = new CombinedResolutionTestResult();
		int numPostresolutionTests = getPostresolutionTests().size();
		if(numPostresolutionTests > 0){
			log.debug("Executing {} postresolution tests in {}", numPostresolutionTests, thisClassName);
			List<ResolutionTest<O>> sortedPostresolutionTests = Lists.newArrayList(getPostresolutionTests());
			Collections.sort(sortedPostresolutionTests, Collections.reverseOrder(new ResolutionOrdering<ResolutionTest<O>>()));
			for(ResolutionTest<O> resolutionTest : sortedPostresolutionTests){
				String testClassName = resolutionTest.getClass().getSimpleName();
				log.debug("Executing postresolution test: {}", testClassName);
				postresolutionTestResults.addResolutionTestResult(resolutionTest.execute(output), testClassName);
			}
			handlePostresolutionTestResults(postresolutionTestResults);
		}
		
		int numPostresolutionActivities = getPostresolutionActivities().size();
		if(numPostresolutionActivities > 0){
			log.debug("Executing {} postresolution activities in {}", numPostresolutionActivities, thisClassName);
			List<ResolutionActivity<O>> sortedPostresolutionActivities = Lists.newArrayList(getPostresolutionActivities());
			Collections.sort(sortedPostresolutionActivities, Collections.reverseOrder(new ResolutionOrdering<ResolutionActivity<O>>()));
			for(ResolutionActivity<O> postresolutionActivity : sortedPostresolutionActivities){
				log.debug("Executing postresolution activity: {}", postresolutionActivity.getClass().getSimpleName());
				postresolutionActivity.perform(output);
			}
		}
		log.trace("{} resolve complete", thisClassName);
		return output;
	}
	
	public final Set<ResolutionActivity<I>> getPreresolutionActivities(){
		if(preresolutionActivities == null){
			preresolutionActivities = Sets.newHashSet();
		}
		return preresolutionActivities;
	}
	
	public final Set<ResolutionActivity<O>> getPostresolutionActivities(){
		if(postresolutionActivities == null){
			postresolutionActivities = Sets.newHashSet();
		}
		return postresolutionActivities;
	}
	
	public final Set<ResolutionTest<I>> getPreresolutionTests(){
		if(preresolutionTests == null){
			preresolutionTests = Sets.newHashSet();
		}
		return preresolutionTests;
	}
	
	public final Set<ResolutionTest<O>> getPostresolutionTests(){
		if(postresolutionTests == null){
			postresolutionTests = Sets.newHashSet();
		}
		return postresolutionTests;
	}

	public final AbstractResolver<I,O> addPreresolutionActivity(final ResolutionActivity<I> preresolutionActivity){
		this.getPreresolutionActivities().add(preresolutionActivity);
		return this;
	}
	
	public final AbstractResolver<I,O> addPostresolutionActivity(final ResolutionActivity<O> postresolutionActivity){
		this.getPostresolutionActivities().add(postresolutionActivity);
		return this;
	}
	
	public final AbstractResolver<I,O> addPreresolutionTest(final ResolutionTest<I> preresolutionTest){
		this.getPreresolutionTests().add(preresolutionTest);
		return this;
	}
	
	public final AbstractResolver<I,O> addPostresolutionTest(final ResolutionTest<O> postresolutionTest){
		this.getPostresolutionTests().add(postresolutionTest);
		return this;
	}
	
	public void handlePreresolutionTestResults(CombinedResolutionTestResult preresolutionTestResult){
		handleResolutionTestResults(preresolutionTestResult);
	}
	
	public void handlePostresolutionTestResults(CombinedResolutionTestResult postresolutionTestResult){
		handleResolutionTestResults(postresolutionTestResult);
	}
	
	public Optional<Class<? extends Annotation>> getPreresolutionTestAnnotationClass(){
		return Optional.absent();
	}
	
	public Optional<Class<? extends Annotation>> getPostresolutionTestAnnotationClass(){
		return Optional.absent();
	}
	
	public Optional<Class<? extends Annotation>> getPreresolutionActivityAnnotationClass(){
		return Optional.absent();
	}
	
	public Optional<Class<? extends Annotation>> getPostresolutionActivityAnnotationClass(){
		return Optional.absent();
	}
	
	protected <T> Set<Class<? extends T>> getSubTypesOf(Class<T> clazz){
		return REFLECTIONS.getSubTypesOf(clazz);
	}
	
	protected <T> Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> clazz, boolean honorInherited){
		return REFLECTIONS.getTypesAnnotatedWith(clazz, honorInherited);
	}
	
	protected <T> Set<Class<?>> getTypesAnnotatedWith(Annotation annotation, boolean honorInherited){
		return REFLECTIONS.getTypesAnnotatedWith(annotation, honorInherited);
	}
	
	private void handleResolutionTestResults(CombinedResolutionTestResult resolutionTestResult){
		if(!resolutionTestResult.isSuccessful()){
			for(ResolutionTestResult result : resolutionTestResult.getFailedTestResults()){
				if(result.getErrorMessage().isPresent()){
					log.error("Error in {}: {}", result.getTestClassName(), result.getErrorMessage().get());
				}
				if(result.getPossibleException().isPresent()){
					log.error("Exception thrown in {}: {}", result.getTestClassName(), result.getPossibleException().get());
				}
			}
			throw new RuntimeException("Resolution Failed...");
		}else{
			for(ResolutionTestResult result : resolutionTestResult.getResolutionTestResults()){
				if(result.getInformationMessage().isPresent()){
					log.info("{}: {}", result.getTestClassName(), result.getInformationMessage().get());
				}
				if(result.getWarningMessage().isPresent()){
					log.warn("{}: {}", result.getTestClassName(), result.getWarningMessage().get());
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@SneakyThrows
	private void initialize(){
		String thisClassName = this.getClass().getSimpleName();
		log.debug("initializing {} started", thisClassName);
		log.trace("loading preresolution tests in {}", thisClassName);
		Optional<Class<? extends Annotation>> preresolutionTestAnnotationClass = getPreresolutionTestAnnotationClass();
		if(preresolutionTestAnnotationClass.isPresent()){
			Set<Class<?>> preresolutionTests = REFLECTIONS.getTypesAnnotatedWith(preresolutionTestAnnotationClass.get());
			for(Class<?> preresolutionTestClass : preresolutionTests){
				this.addPreresolutionTest((ResolutionTest<I>) preresolutionTestClass.getConstructor().newInstance());
			}
		}
		log.trace("loading postresolution tests in {}", thisClassName);
		Optional<Class<? extends Annotation>> postresolutionTestAnnotationClass = getPostresolutionTestAnnotationClass();
		if(postresolutionTestAnnotationClass.isPresent()){
			Set<Class<?>> postresolutionTests = REFLECTIONS.getTypesAnnotatedWith(postresolutionTestAnnotationClass.get());
			for(Class<?> postresolutionTestClass : postresolutionTests){
				this.addPostresolutionTest((ResolutionTest<O>) postresolutionTestClass.getConstructor().newInstance());
			}
		}
		log.trace("loading preresolution activities in {}", thisClassName);
		Optional<Class<? extends Annotation>> preresolutionActivityAnnotationClass = getPreresolutionActivityAnnotationClass();
		if(preresolutionActivityAnnotationClass.isPresent()){
			Set<Class<?>> preresolutionActivities = REFLECTIONS.getTypesAnnotatedWith(preresolutionActivityAnnotationClass.get());
			for(Class<?> preresolutionActivityClass : preresolutionActivities){
				this.addPreresolutionActivity((ResolutionActivity<I>) preresolutionActivityClass.getConstructor().newInstance());
			}
		}
		log.trace("loading postresolution activities in {}", thisClassName);
		Optional<Class<? extends Annotation>> postresolutionActivityAnnotationClass = getPostresolutionActivityAnnotationClass();
		if(postresolutionActivityAnnotationClass.isPresent()){
			Set<Class<?>> postresolutionActivities = REFLECTIONS.getTypesAnnotatedWith(postresolutionActivityAnnotationClass.get());
			for(Class<?> postresolutionActivityClass : postresolutionActivities){
				this.addPostresolutionActivity((ResolutionActivity<O>) postresolutionActivityClass.getConstructor().newInstance());
			}
		}
		log.debug("initializing {} complete", thisClassName);
	}
	
}
