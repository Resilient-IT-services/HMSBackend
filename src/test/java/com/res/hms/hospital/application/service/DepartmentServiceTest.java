package com.res.hms.hospital.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.core.exception.ApiException;
import com.res.hms.hospital.domain.Department;
import com.res.hms.hospital.infrastructure.repository.DepartmentRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

  @Mock private DepartmentRepository departmentRepository;
  @InjectMocks private DepartmentService departmentService;

  private Department dept1;
  private Department dept2;

  @BeforeEach
  void setUp() {
    dept1 = new Department();
    dept1.setId(UUID.randomUUID());
    dept1.setName("Cardiology");
    dept1.setActive(true);

    dept2 = new Department();
    dept2.setId(UUID.randomUUID());
    dept2.setName("Radiology");
    dept2.setActive(false);
  }

  @Test
  void listActiveNames() {
    when(departmentRepository.findAll()).thenReturn(Arrays.asList(dept1, dept2));
    List<String> names = departmentService.listActiveNames();
    assertEquals(1, names.size());
    assertTrue(names.contains("Cardiology"));
  }

  @Test
  void listAll() {
    when(departmentRepository.findAll()).thenReturn(Arrays.asList(dept1, dept2));
    List<Department> departments = departmentService.listAll();
    assertEquals(2, departments.size());
  }

  @Test
  void create_Success() {
    when(departmentRepository.findByName("Neurology")).thenReturn(Optional.empty());
    Department newDept = new Department();
    newDept.setName("Neurology");
    when(departmentRepository.save(any(Department.class))).thenReturn(newDept);

    Department saved = departmentService.create(newDept);
    assertNotNull(saved);
    assertEquals("Neurology", saved.getName());
  }

  @Test
  void create_ThrowsAlreadyExists() {
    when(departmentRepository.findByName("Cardiology")).thenReturn(Optional.of(dept1));
    Department newDept = new Department();
    newDept.setName("Cardiology");

    ApiException ex = assertThrows(ApiException.class, () -> departmentService.create(newDept));
    assertEquals("ALREADY_EXISTS", ex.getCode());
    assertEquals(HttpStatus.CONFLICT, ex.getStatus());
  }

  @Test
  void update_Success() {
    when(departmentRepository.findById(dept1.getId())).thenReturn(Optional.of(dept1));
    when(departmentRepository.save(any(Department.class))).thenReturn(dept1);

    Department updateData = new Department();
    updateData.setName("New Name");
    updateData.setDescription("New Desc");
    updateData.setActive(false);

    Department updated = departmentService.update(dept1.getId(), updateData);
    assertEquals("New Name", updated.getName());
    assertEquals("New Desc", updated.getDescription());
    assertFalse(updated.isActive());
  }

  @Test
  void update_ThrowsNotFound() {
    UUID id = UUID.randomUUID();
    when(departmentRepository.findById(id)).thenReturn(Optional.empty());

    ApiException ex =
        assertThrows(ApiException.class, () -> departmentService.update(id, new Department()));
    assertEquals("NOT_FOUND", ex.getCode());
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
  }

  @Test
  void delete() {
    UUID id = UUID.randomUUID();
    doNothing().when(departmentRepository).deleteById(id);
    departmentService.delete(id);
    verify(departmentRepository, times(1)).deleteById(id);
  }
}
