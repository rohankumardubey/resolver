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
package org.jboss.shrinkwrap.resolver.impl.maven;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.resolver.api.NoResolutionException;
import org.jboss.shrinkwrap.resolver.api.maven.MavenFormatStage;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolutionFilter;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolutionStrategy;
import org.jboss.shrinkwrap.resolver.api.maven.MavenStrategyStage;
import org.jboss.shrinkwrap.resolver.api.maven.PackagingType;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.dependency.DependencyDeclaration;
import org.jboss.shrinkwrap.resolver.api.maven.dependency.exclusion.DependencyExclusion;
import org.jboss.shrinkwrap.resolver.impl.maven.convert.MavenConverter;
import org.jboss.shrinkwrap.resolver.impl.maven.dependency.DependencyDeclarationImpl;
import org.jboss.shrinkwrap.resolver.impl.maven.filter.AcceptAllFilter;
import org.jboss.shrinkwrap.resolver.impl.maven.filter.MavenResolutionFilterInternalView;
import org.jboss.shrinkwrap.resolver.impl.maven.strategy.NonTransitiveStrategy;
import org.jboss.shrinkwrap.resolver.impl.maven.strategy.TransitiveStrategy;
import org.jboss.shrinkwrap.resolver.impl.maven.util.Validate;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyResolutionException;

/**
 * Implementation of {@link MavenStrategyStage}
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 */
public class MavenStrategyStageImpl implements MavenStrategyStage, MavenWorkingSessionContainer {

    private static final Logger log = Logger.getLogger(MavenStrategyStageImpl.class.getName());

    private final MavenWorkingSession session;

    public MavenStrategyStageImpl(MavenWorkingSession session) {
        this.session = session;
    }

    @Override
    public MavenFormatStage withTransitivity() {
        return using(new TransitiveStrategy());
    }

    @Override
    public MavenFormatStage withoutTransitivity() {
        return using(new NonTransitiveStrategy());
    }

    @Override
    public MavenWorkingSession getMavenWorkingSession() {
        return session;
    }

    private List<DependencyDeclaration> preFilter(MavenResolutionFilter filter, List<DependencyDeclaration> heap) {

        if (filter == null) {
            return heap;
        }

        List<DependencyDeclaration> filtered = new ArrayList<DependencyDeclaration>();
        for (DependencyDeclaration candidate : heap) {
            if (filter.accepts(candidate)) {
                filtered.add(candidate);
            }
        }

        return filtered;
    }

    @Override
    public MavenFormatStage using(MavenResolutionStrategy strategy) throws IllegalArgumentException {
        // first, get dependencies specified for resolution in the session
        Validate.notEmpty(session.getDependencies(), "No dependencies were set for resolution");

        // create a copy
        final List<DependencyDeclaration> prefilteredDependencies = preFilter(
            configureFilterFromSession(session, strategy.getPreResolutionFilter()), session.getDependencies());
        final List<DependencyDeclaration> depManagement = new ArrayList<DependencyDeclaration>(
            session.getVersionManagement());


        final CollectRequest request = new CollectRequest(MavenConverter.asDependencies(prefilteredDependencies),
            MavenConverter.asDependencies(depManagement), session.getRemoteRepositories());

        // wrap artifact files to archives
        Collection<ArtifactResult> artifactResults = null;
        try {
            // resolution filtering
            artifactResults = session.execute(request, configureFilterFromSession(session, strategy.getResolutionFilter()));
        } catch (DependencyResolutionException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof ArtifactResolutionException) {
                    throw new NoResolutionException("Unable to get artifact from the repository");
                } else if (cause instanceof DependencyCollectionException) {
                    throw new NoResolutionException("Unable to collect dependency tree for given dependencies");
                }
                throw new NoResolutionException("Unable to collect/resolve dependency tree for a resulution");
            }
        }

        // TODO Do we need to do real post-resolution filtering? Seems excessive, plus it breaks tests
        // https://community.jboss.org/message/756355#756355
        // final MavenResolutionFilter postResolutionFilter = new CombinedFilter(RestrictPomArtifactFilter.INSTANCE,
        // (MavenResolutionFilterInternalView) strategy.getPostResolutionFilter());
        final MavenResolutionFilter postResolutionFilter = RestrictPomArtifactFilter.INSTANCE;

        // Run post-resolution filtering to weed out POMs
        final Collection<ArtifactResult> filteredArtifacts = new ArrayList<ArtifactResult>();
        for (final ArtifactResult artifactResult : artifactResults) {
            final Artifact artifact = artifactResult.getArtifact();
            final DependencyDeclaration dependency = new DependencyDeclarationImpl(artifact.getGroupId(),
                artifact.getArtifactId(), PackagingType.fromPackagingType(artifact.getExtension()),
                artifact.getClassifier(), artifact.getBaseVersion(), ScopeType.COMPILE, false,
                new HashSet<DependencyExclusion>(0));
            if (postResolutionFilter.accepts(dependency)) {
                filteredArtifacts.add(artifactResult);
            }
        }

        // Proceed to format stage
        return new MavenFormatStageImpl(filteredArtifacts);
    }

    /**
     * {@link MavenResolutionFilter} implementation which does not allow POMs to pass through
     *
     * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
     */
    private enum RestrictPomArtifactFilter implements MavenResolutionFilterInternalView {

        INSTANCE;

        @Override
        public boolean accepts(final DependencyDeclaration coordinate) throws IllegalArgumentException {
            if (PackagingType.POM.equals(coordinate.getPackaging())) {
                if (log.isLoggable(Level.FINER)) {
                    log.finer("Filtering out POM dependency resolution: " + coordinate
                        + "; its transitive dependencies will be included");
                }

                return false;
            }
            return true;
        }

        @Override
        public MavenResolutionFilterInternalView setDefinedDependencies(final List<DependencyDeclaration> dependencies) {
            return this;
        }

        @Override
        public MavenResolutionFilterInternalView setDefinedDependencyManagement(
            final List<DependencyDeclaration> dependencyManagement) {
            return this;
        }

    }

    private MavenResolutionFilter configureFilterFromSession(final MavenWorkingSession session,
        final MavenResolutionFilter filter) {

        Validate.notNull(session, "MavenWorkingSession must not be null");
        // filter can be null
        if (filter == null) {
            return AcceptAllFilter.INSTANCE;
        }

        // Represent as our internal SPI type
        assert filter instanceof MavenResolutionFilterInternalView : "All filters must conform to the internal SPI: "
            + MavenResolutionFilterInternalView.class.getName();
        final MavenResolutionFilterInternalView internalFilterView = MavenResolutionFilterInternalView.class
            .cast(filter);

        // prepare dependencies
        Stack<DependencyDeclaration> dependencies = session.getDependencies();
        List<DependencyDeclaration> dependenciesList;
        if (dependencies == null || dependencies.size() == 0) {
            dependenciesList = Collections.emptyList();
        } else {
            dependenciesList = new ArrayList<DependencyDeclaration>(dependencies);
        }

        // prepare dependency management
        Set<DependencyDeclaration> dependenciesMngmt = session.getVersionManagement();
        List<DependencyDeclaration> dependenciesMngmtList;
        if (dependenciesMngmt == null || dependenciesMngmt.size() == 0) {
            dependenciesMngmtList = Collections.emptyList();
        } else {
            dependenciesMngmtList = new ArrayList<DependencyDeclaration>(dependencies);
        }

        // configure filter
        internalFilterView.setDefinedDependencies(dependenciesList);
        internalFilterView.setDefinedDependencyManagement(dependenciesMngmtList);

        return internalFilterView;
    }

    @Override
    public MavenStrategyStage offline(final boolean offline) {
        // Set session offline flag via the abstraction
        this.session.setOffline(offline);
        return this;
    }

    @Override
    public MavenStrategyStage offline() {
        // Delegate
        return this.offline(true);
    }
}
