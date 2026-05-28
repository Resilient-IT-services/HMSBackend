package com.res.hms;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.res.hms",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureTest {

  @ArchTest
  static final ArchRule CONTROLLERS_SHOULD_ONLY_DEPEND_ON_SERVICES =
      classes()
          .that()
          .haveSimpleNameEndingWith("Controller")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "com.res.hms..api..",
              "com.res.hms..application..",
              "com.res.hms..application.service..",
              "com.res.hms..application.dto..",
              "com.res.hms..domain..",
              "com.res.hms.core..",
              "java..",
              "jakarta..",
              "org.springframework..",
              "io.swagger..",
              "lombok..");

  @ArchTest
  static final ArchRule CONTROLLERS_SHOULD_NOT_ACCESS_REPOSITORIES =
      noClasses()
          .that()
          .haveSimpleNameEndingWith("Controller")
          .should()
          .accessClassesThat()
          .haveSimpleNameEndingWith("Repository");

  @ArchTest
  static final ArchRule SERVICES_SHOULD_BE_IN_SERVICE_PACKAGE =
      classes().that().haveSimpleNameEndingWith("Service").should().resideInAPackage("..service..");

  @ArchTest
  static final ArchRule REPOSITORIES_SHOULD_BE_IN_REPOSITORY_PACKAGE =
      classes()
          .that()
          .haveSimpleNameEndingWith("Repository")
          .should()
          .resideInAPackage("..repository..");

  @ArchTest
  static final ArchRule ENTITIES_SHOULD_HAVE_UUID_ID =
      classes()
          .that()
          .areAnnotatedWith("jakarta.persistence.Entity")
          .should(
              new com.tngtech.archunit.lang.ArchCondition<>("have a UUID ID") {
                @Override
                public void check(
                    com.tngtech.archunit.core.domain.JavaClass item,
                    com.tngtech.archunit.lang.ConditionEvents events) {
                  boolean hasUuidId =
                      item.getAllFields().stream()
                          .filter(field -> field.isAnnotatedWith("jakarta.persistence.Id"))
                          .anyMatch(
                              field -> field.getRawType().isAssignableTo(java.util.UUID.class));

                  if (!hasUuidId) {
                    String message =
                        String.format("Entity %s does not have a UUID ID", item.getName());
                    events.add(
                        com.tngtech.archunit.lang.SimpleConditionEvent.violated(item, message));
                  }
                }
              });
}
