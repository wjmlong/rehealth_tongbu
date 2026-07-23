package com.rehealth.device;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DeviceServiceArchitectureTest {
    @Test
    void deviceServiceDoesNotDependOnJeecgRepositoriesOrModels() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.rehealth.device");

        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.jeecg..", "..repository..", "..model..")
                .check(classes);
    }
}
