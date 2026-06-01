package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.inventory.domain.Item;
import com.res.hms.inventory.infrastructure.repository.ItemRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GlobalAdminFilteringIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private HospitalRepository hospitalRepository;
  @Autowired private ItemRepository itemRepository;
  @Autowired private com.res.hms.audit.infrastructure.repository.AuditLogRepository auditLogRepository;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
  @Autowired private com.res.hms.iam.infrastructure.repository.UserAccountRepository userAccountRepository;
  @Autowired private com.res.hms.iam.infrastructure.repository.RoleRepository roleRepository;

  private MockHttpSession adminSession;
  private UUID hospitalId;
  private UUID adminUserId;

  @BeforeEach
  void setUp() throws Exception {
    hospitalId = testDataHelper.createHospitalIfMissing("ADMIN_TEST", "Admin Test Hospital");
    
    // Login as super admin
    var loginResponse = mockMvc
        .perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\": \"admin\", \"password\": \"admin123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    
    adminSession = (MockHttpSession) loginResponse.getRequest().getSession();
    
    var meResponse = mockMvc
        .perform(get("/api/v1/auth/me").session(adminSession))
        .andExpect(status().isOk())
        .andReturn();
    
    adminUserId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(meResponse.getResponse().getContentAsString(), "$.data.id"));
  }

  @Test
  void testHospitalListingWithFilteringAndSorting() throws Exception {
    createHospital("Alpha Clinic", "ALPHA");
    createHospital("Beta Hospital", "BETA");
    createHospital("General Hospital", "GENERAL");

    // Filter by search
    mockMvc.perform(get("/api/v1/hospitals")
            .session(adminSession)
            .param("search", "General"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].name").value("General Hospital"));

    // Sort by name desc
    mockMvc.perform(get("/api/v1/hospitals")
            .session(adminSession)
            .param("sort", "name,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[*].name", org.hamcrest.Matchers.hasItems("General Hospital", "Beta Hospital", "Alpha Clinic")));
  }

  @Test
  void testInventoryItemListingWithFilteringAndSorting() throws Exception {
    createItem("Syringe", "Medical");
    createItem("Gauze", "Surgical");

    // Filter by category
    mockMvc.perform(get("/api/v1/inventory/items")
            .session(adminSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("category", "Medical"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].name").value("Syringe"));

    // Sort by name asc
    mockMvc.perform(get("/api/v1/inventory/items")
            .session(adminSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].name").value("Gauze"))
        .andExpect(jsonPath("$.data.content[1].name").value("Syringe"));
  }

  @Test
  void testAuditLogListingWithFilteringAndSorting() throws Exception {
    createAuditLog("USER_ACCOUNT", "CREATE");
    createAuditLog("PATIENT", "UPDATE");

    // Filter by entityName
    mockMvc.perform(get("/api/v1/audit-logs")
            .session(adminSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("entityName", "USER_ACCOUNT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].entityName").value("USER_ACCOUNT"));

    // Sort by entityName desc
    mockMvc.perform(get("/api/v1/audit-logs")
            .session(adminSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("sort", "entityName,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].entityName").value("USER_ACCOUNT"))
        .andExpect(jsonPath("$.data.content[1].entityName").value("PATIENT"));
  }

  private void createAuditLog(String entityName, String action) {
    com.res.hms.audit.domain.AuditLog log = new com.res.hms.audit.domain.AuditLog();
    log.setHospitalId(hospitalId);
    log.setEntityName(com.res.hms.audit.domain.DomainEntity.valueOf(entityName));
    log.setEntityId(UUID.randomUUID());
    log.setAction(com.res.hms.audit.domain.AuditAction.valueOf(action));
    log.setPerformedBy(adminUserId);
    log.setCreatedAt(java.time.Instant.now());
    auditLogRepository.save(log);
  }

  private void createHospital(String name, String code) {
    Hospital h = new Hospital();
    h.setName(name);
    h.setCode(code);
    h.setAddress("Test Address");
    h.setStatus(com.res.hms.hospital.domain.HospitalStatus.ACTIVE);
    hospitalRepository.save(h);
  }

  private void createItem(String name, String category) {
    Item item = new Item();
    item.setHospitalId(hospitalId);
    item.setName(name);
    item.setCategory(category);
    item.setUnit(com.res.hms.inventory.domain.UnitOfMeasure.PIECE);
    itemRepository.save(item);
  }
}
