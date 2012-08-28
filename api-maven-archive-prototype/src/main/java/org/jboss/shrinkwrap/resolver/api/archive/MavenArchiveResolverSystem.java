/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.shrinkwrap.resolver.api.archive;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.resolver.api.ResolverSystem;
import org.jboss.shrinkwrap.resolver.api.Resolvers;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableResolveStageBase;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolutionStrategy;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolveStageBase;
import org.jboss.shrinkwrap.resolver.api.maven.dependency.exclusion.DependencyExclusionBuilderToConfiguredDependencyDeclarationBuilderBridge;

/**
 * Entry point of a Maven-based Resolver system capable of formatting results as ShrinkWrap {@link Archive}s. To create
 * a new instance, pass in this class reference to {@link Resolvers#use(Class)} or
 * {@link Resolvers#use(Class, ClassLoader)}, or instead call upon
 * {@link MavenArchiveResolverSystemShortcutImpl#INSTANCE}.
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 */
public interface MavenArchiveResolverSystem
    extends
    ResolverSystem,
    MavenResolveStageBase<ConfiguredArchiveDependencyDeclarationBuilder, DependencyExclusionBuilderToConfiguredDependencyDeclarationBuilderBridge, MavenConfiguredArchiveResolveStage, MavenArchiveStrategyStage, MavenFormatArchiveStage, MavenResolutionStrategy>,
    ConfigurableResolveStageBase<ConfiguredArchiveDependencyDeclarationBuilder, DependencyExclusionBuilderToConfiguredDependencyDeclarationBuilderBridge, MavenConfiguredArchiveResolveStage, MavenArchiveStrategyStage, MavenFormatArchiveStage, MavenResolutionStrategy> {

}
