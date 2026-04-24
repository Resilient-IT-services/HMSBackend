package com.res.hms.user.dto;

import com.res.hms.domain.RoleCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UserRoleValidator implements ConstraintValidator<ValidUserRole, UserRoleValidatable> {

  @Override
  public boolean isValid(UserRoleValidatable req, ConstraintValidatorContext context) {
    boolean isDoctor = req.roles().contains(RoleCode.DOCTOR);
    boolean isNurse = req.roles().contains(RoleCode.NURSE);
    boolean valid = true;
    context.disableDefaultConstraintViolation();
    if (isDoctor) {
      if (req.consultationFee() == null || req.consultationFee().floatValue() <= 0.1) {
        context
            .buildConstraintViolationWithTemplate("consultationFee required for doctor ")
            .addPropertyNode("consultationFee")
            .addConstraintViolation();
        valid = false;
      }
      if (req.specialization() == null || req.specialization().isBlank()) {
        context
            .buildConstraintViolationWithTemplate("specialization required for doctor")
            .addPropertyNode("specialization")
            .addConstraintViolation();
        valid = false;
      }
      if (req.followUpDays() == null || req.followUpDays() <= 0) {
        context
            .buildConstraintViolationWithTemplate("followUpDays required for doctor")
            .addPropertyNode("followUpDays")
            .addConstraintViolation();
        valid = false;
      }
    }

    if (isNurse) {
      if (req.department() == null || req.department().isBlank()) {
        context
            .buildConstraintViolationWithTemplate("department required for nurse")
            .addPropertyNode("department")
            .addConstraintViolation();
        valid = false;
      }
    }

    return valid;
  }
}
