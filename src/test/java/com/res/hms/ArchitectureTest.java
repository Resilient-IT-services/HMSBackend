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
public class ArchitectureTest {

  @ArchTest
  static final ArchRule controllers_should_only_depend_on_services =
      classes()
          .that()
          .haveSimpleNameEndingWith("Controller")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "com.res.hms.api..",
              "com.res.hms..service..",
              "com.res.hms..dto..",
              "com.res.hms.common..",
              "java..",
              "jakarta..",
              "org.springframework..",
              "io.swagger..",
              "lombok..");

  @ArchTest
  static final ArchRule controllers_should_not_access_repositories =
      noClasses()
          .that()
          .haveSimpleNameEndingWith("Controller")
          .should()
          .accessClassesThat()
          .haveSimpleNameEndingWith("Repository");

  @ArchTest
  static final ArchRule services_should_be_in_service_package =
      classes().that().haveSimpleNameEndingWith("Service").should().resideInAPackage("..service..");

  @ArchTest
  static final ArchRule repositories_should_be_in_repository_package =
      classes()
          .that()
          .haveSimpleNameEndingWith("Repository")
          .should()
          .resideInAPackage("..repository..");

  @ArchTest
  static final ArchRule entities_should_have_uuid_id =
      classes()
          .that()
          .areAnnotatedWith("jakarta.persistence.Entity")
          .should(new com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>("have a UUID ID") {
            @Override
            public void check(com.tngtech.archunit.core.domain.JavaClass item, com.tngtech.archunit.lang.ConditionEvents events) {
              boolean hasUuidId = item.getAllFields().stream()
                  .filter(field -> field.isAnnotatedWith("jakarta.persistence.Id"))
                  .anyMatch(field -> field.getRawType().isAssignableTo(java.util.UUID.class));
              
              if (!hasUuidId) {
                String message = String.format("Entity %s does not have a UUID ID", item.getName());
                events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(item, message));
              }
            }
          });
}
