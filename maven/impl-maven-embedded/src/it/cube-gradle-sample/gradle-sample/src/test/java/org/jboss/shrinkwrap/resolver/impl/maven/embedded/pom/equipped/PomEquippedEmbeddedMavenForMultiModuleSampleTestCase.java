package org.jboss.shrinkwrap.resolver.impl.maven.embedded.pom.equipped;

import org.jboss.shrinkwrap.resolver.api.maven.embedded.BuiltProject;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;
import org.junit.Test;

import static org.jboss.shrinkwrap.resolver.impl.maven.embedded.Utils.archiveNameModuleTwoParamKey;
import static org.jboss.shrinkwrap.resolver.impl.maven.embedded.Utils.archiveNameModuleTwoParamValue;
import static org.jboss.shrinkwrap.resolver.impl.maven.embedded.Utils.multiModuleactivateModulesParamKey;
import static org.jboss.shrinkwrap.resolver.impl.maven.embedded.Utils.multiModuleactivateModulesParamValue;
import static org.jboss.shrinkwrap.resolver.impl.maven.embedded.Utils.pathToMultiModulePom;
import static org.jboss.shrinkwrap.resolver.impl.maven.embedded.Utils.verifyMavenVersion;
import static org.jboss.shrinkwrap.resolver.impl.maven.embedded.Utils.verifyMultiModuleSample;
import static org.jboss.shrinkwrap.resolver.impl.maven.embedded.Utils.verifyMultiModuleSampleWasCleaned;

/**
 * @author <a href="mailto:mjobanek@redhat.com">Matous Jobanek</a>
 */
public class PomEquippedEmbeddedMavenForMultiModuleSampleTestCase {

    @Test
    public void testMultiModuleSampleBuildWithMaven305() {
        BuiltProject builtProject = EmbeddedMaven
            .forProject(pathToMultiModulePom)
            .useMaven3Version("3.0.5")
            .setGoals("install")
            .addProperty(multiModuleactivateModulesParamKey, multiModuleactivateModulesParamValue)
            .addProperty(archiveNameModuleTwoParamKey, archiveNameModuleTwoParamValue)
            .setShowVersion(true)
            .build();

        verifyMavenVersion(builtProject, "3.0.5");
        verifyMultiModuleSample(builtProject, true);
    }

    @Test
    public void testMultiModuleSampleCleanBuild() {
        BuiltProject builtProject = EmbeddedMaven
            .forProject(pathToMultiModulePom)
            .useMaven3Version("3.1.0")
            .setGoals("clean")
            .addProperty(multiModuleactivateModulesParamKey, multiModuleactivateModulesParamValue)
            .addProperty(archiveNameModuleTwoParamKey, archiveNameModuleTwoParamValue)
            .build();

        verifyMultiModuleSampleWasCleaned(builtProject);
    }

    @Test
    public void testMultiModuleSampleBuildWithoutModulesActivated() {
        BuiltProject builtProject = EmbeddedMaven
            .forProject(pathToMultiModulePom)
            .useMaven3Version("3.1.0")
            .setGoals("clean", "package")
            .addProperty(archiveNameModuleTwoParamKey, archiveNameModuleTwoParamValue)
            .build();

        verifyMultiModuleSample(builtProject, false);
    }
}
