# Plan de Pruebas - Sistema de Roles y Permisos

## Estrategia General de Testing

### Objetivos de Testing

1. **Cobertura >= 80%** en backend, >= 75% en frontend
2. **Cero defectos críticos** en producción
3. **Performance validada** en cada sprint
4. **Regression testing** automatizado
5. **E2E coverage** de flujos principales

### Tipos de Pruebas

```
Pirámide de Testing:
         /\
        /E2E\        10% - Tests End-to-End
       /------\
      /Integr.\     30% - Tests de Integración
     /----------\
    /  Unitarios \  60% - Tests Unitarios
   /--------------\
```

## Tests Unitarios

### Backend - Tests Unitarios Java

#### 1. Tests de Entidades (Data Model)

**`RoleTest.java`**
```java
package org.thingsboard.server.common.data.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class RoleTest {
    
    private Role role;
    
    @BeforeEach
    void setUp() {
        role = new Role();
    }
    
    @Test
    void testRoleCreation() {
        role.setName("Device Manager");
        role.setDescription("Manage devices");
        role.setSystem(false);
        
        assertEquals("Device Manager", role.getName());
        assertEquals("Manage devices", role.getDescription());
        assertFalse(role.isSystem());
    }
    
    @Test
    void testRoleWithNullTenantId() {
        role.setTenantId(null);
        assertTrue(role.isSystemRole());
    }
    
    @Test
    void testRoleEquality() {
        Role role1 = new Role();
        role1.setId(new RoleId(UUID.randomUUID()));
        role1.setName("Role1");
        
        Role role2 = new Role();
        role2.setId(role1.getId());
        role2.setName("Role1");
        
        assertEquals(role1, role2);
    }
}
```

**`RolePermissionTest.java`**
```java
public class RolePermissionTest {
    
    @Test
    void testPermissionCreation() {
        RolePermission perm = new RolePermission();
        perm.setResource(Resource.DEVICE);
        perm.setOperation(Operation.READ);
        
        assertEquals(Resource.DEVICE, perm.getResource());
        assertEquals(Operation.READ, perm.getOperation());
    }
    
    @Test
    void testPermissionWithAllOperation() {
        RolePermission perm = new RolePermission();
        perm.setOperation(Operation.ALL);
        
        assertTrue(perm.allowsOperation(Operation.READ));
        assertTrue(perm.allowsOperation(Operation.WRITE));
        assertTrue(perm.allowsOperation(Operation.DELETE));
    }
}
```

**Coverage Target:** >= 90%  
**Assertions mínimas por test:** 3  
**Total Tests:** ~15 tests

#### 2. Tests de DAO

**`RoleDaoTest.java`**
```java
@SpringBootTest
@ActiveProfiles("test")
public class RoleDaoTest {
    
    @Autowired
    private RoleDao roleDao;
    
    @Autowired
    private DataSource dataSource;
    
    private TenantId tenantId;
    
    @BeforeEach
    void setUp() {
        tenantId = new TenantId(UUID.randomUUID());
        cleanDatabase();
    }
    
    @AfterEach
    void tearDown() {
        cleanDatabase();
    }
    
    @Test
    void testSaveRole() {
        Role role = createTestRole("Test Role");
        Role saved = roleDao.save(tenantId, role);
        
        assertNotNull(saved.getId());
        assertEquals("Test Role", saved.getName());
        assertEquals(tenantId, saved.getTenantId());
    }
    
    @Test
    void testFindByTenantId() {
        createTestRole("Role 1");
        createTestRole("Role 2");
        createTestRole("Role 3");
        
        PageLink pageLink = new PageLink(10, 0);
        PageData<Role> roles = roleDao.findByTenantId(tenantId.getId(), pageLink);
        
        assertEquals(3, roles.getData().size());
        assertTrue(roles.hasNext() == false);
    }
    
    @Test
    void testFindByTenantIdAndName() {
        createTestRole("Device Manager");
        
        Role found = roleDao.findByTenantIdAndName(tenantId, "Device Manager");
        
        assertNotNull(found);
        assertEquals("Device Manager", found.getName());
    }
    
    @Test
    void testFindByTenantIdAndNameNotFound() {
        Role found = roleDao.findByTenantIdAndName(tenantId, "Non Existent");
        assertNull(found);
    }
    
    @Test
    void testDeleteRole() {
        Role role = createTestRole("To Delete");
        Role saved = roleDao.save(tenantId, role);
        
        roleDao.removeById(tenantId, saved.getId().getId());
        
        Role found = roleDao.findById(tenantId, saved.getId().getId());
        assertNull(found);
    }
    
    @Test
    void testFindSystemRoles() {
        Role systemRole = new Role();
        systemRole.setName("System Admin");
        systemRole.setSystem(true);
        systemRole.setTenantId(null);
        roleDao.save(TenantId.SYS_TENANT_ID, systemRole);
        
        List<Role> systemRoles = roleDao.findSystemRoles();
        
        assertTrue(systemRoles.size() >= 1);
        assertTrue(systemRoles.stream().allMatch(Role::isSystem));
    }
    
    @Test
    void testUniqueConstraintViolation() {
        createTestRole("Duplicate");
        
        assertThrows(DataIntegrityViolationException.class, () -> {
            createTestRole("Duplicate");
        });
    }
    
    private Role createTestRole(String name) {
        Role role = new Role();
        role.setName(name);
        role.setTenantId(tenantId);
        role.setDescription("Test description");
        role.setSystem(false);
        return roleDao.save(tenantId, role);
    }
    
    private void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM role_permission");
        jdbcTemplate.execute("DELETE FROM role WHERE tenant_id = '" + tenantId.getId() + "'");
    }
}
```

**`RolePermissionDaoTest.java`**
```java
@SpringBootTest
@ActiveProfiles("test")
public class RolePermissionDaoTest {
    
    @Autowired
    private RolePermissionDao permissionDao;
    
    @Autowired
    private RoleDao roleDao;
    
    private RoleId roleId;
    
    @BeforeEach
    void setUp() {
        Role role = createTestRole();
        roleId = role.getId();
    }
    
    @Test
    void testSavePermission() {
        RolePermission perm = createPermission(Resource.DEVICE, Operation.READ);
        RolePermission saved = permissionDao.save(perm);
        
        assertNotNull(saved.getId());
        assertEquals(Resource.DEVICE, saved.getResource());
    }
    
    @Test
    void testFindByRoleId() {
        createPermission(Resource.DEVICE, Operation.READ);
        createPermission(Resource.DEVICE, Operation.WRITE);
        createPermission(Resource.ASSET, Operation.READ);
        
        List<RolePermission> perms = permissionDao.findByRoleId(roleId);
        
        assertEquals(3, perms.size());
    }
    
    @Test
    void testDeleteByRoleId() {
        createPermission(Resource.DEVICE, Operation.READ);
        createPermission(Resource.ASSET, Operation.WRITE);
        
        permissionDao.deleteByRoleId(roleId);
        
        List<RolePermission> perms = permissionDao.findByRoleId(roleId);
        assertEquals(0, perms.size());
    }
    
    @Test
    void testSaveAllPermissions() {
        List<RolePermission> permissions = Arrays.asList(
            createPermission(Resource.DEVICE, Operation.ALL),
            createPermission(Resource.ASSET, Operation.ALL),
            createPermission(Resource.DASHBOARD, Operation.READ)
        );
        
        permissionDao.saveAll(permissions);
        
        List<RolePermission> saved = permissionDao.findByRoleId(roleId);
        assertEquals(3, saved.size());
    }
    
    private RolePermission createPermission(Resource resource, Operation operation) {
        RolePermission perm = new RolePermission();
        perm.setRoleId(roleId);
        perm.setResource(resource);
        perm.setOperation(operation);
        return permissionDao.save(perm);
    }
}
```

**Coverage Target:** >= 85%  
**Total Tests:** ~25 tests

#### 3. Tests de Service Layer

**`RoleServiceTest.java`**
```java
@SpringBootTest
@ActiveProfiles("test")
public class RoleServiceTest {
    
    @Autowired
    private RoleService roleService;
    
    @MockBean
    private RoleDao roleDao;
    
    @MockBean
    private RolePermissionDao permissionDao;
    
    private TenantId tenantId;
    private Role testRole;
    
    @BeforeEach
    void setUp() {
        tenantId = new TenantId(UUID.randomUUID());
        testRole = createMockRole();
    }
    
    @Test
    void testSaveRole() {
        when(roleDao.save(eq(tenantId), any(Role.class))).thenReturn(testRole);
        
        Role saved = roleService.saveRole(testRole);
        
        assertNotNull(saved);
        verify(roleDao, times(1)).save(eq(tenantId), any(Role.class));
    }
    
    @Test
    void testSaveRoleValidation() {
        Role invalidRole = new Role();
        invalidRole.setName(""); // nombre vacío
        
        assertThrows(DataValidationException.class, () -> {
            roleService.saveRole(invalidRole);
        });
    }
    
    @Test
    void testDeleteRoleWithUsers() {
        // Simular que hay usuarios con este rol
        when(userService.countUsersByRoleId(any(RoleId.class))).thenReturn(5L);
        
        assertThrows(DataValidationException.class, () -> {
            roleService.deleteRole(tenantId, testRole.getId());
        });
    }
    
    @Test
    void testDeleteSystemRole() {
        testRole.setSystem(true);
        when(roleDao.findById(any(), any())).thenReturn(testRole);
        
        assertThrows(DataValidationException.class, () -> {
            roleService.deleteRole(tenantId, testRole.getId());
        });
    }
    
    @Test
    void testUpdateRolePermissions() {
        Set<RolePermission> permissions = createTestPermissions();
        
        roleService.updateRolePermissions(testRole.getId(), permissions);
        
        verify(permissionDao, times(1)).deleteByRoleId(testRole.getId());
        verify(permissionDao, times(1)).saveAll(anyList());
    }
    
    @Test
    void testGetRolePermissions() {
        List<RolePermission> mockPerms = createTestPermissionsList();
        when(permissionDao.findByRoleId(any())).thenReturn(mockPerms);
        
        Set<RolePermission> perms = roleService.getRolePermissions(testRole.getId());
        
        assertEquals(mockPerms.size(), perms.size());
    }
    
    @Test
    void testCreateDefaultTenantRoles() {
        roleService.createDefaultTenantRoles(tenantId);
        
        verify(roleDao, atLeast(2)).save(eq(tenantId), any(Role.class));
    }
    
    @Test
    void testFindRolesByTenantIdPagination() {
        PageLink pageLink = new PageLink(10, 0);
        PageData<Role> mockPage = new PageData<>();
        when(roleDao.findByTenantId(any(), any())).thenReturn(mockPage);
        
        PageData<Role> result = roleService.findRolesByTenantId(tenantId, pageLink);
        
        assertNotNull(result);
        verify(roleDao, times(1)).findByTenantId(tenantId.getId(), pageLink);
    }
}
```

**`RoleBasedPermissionCheckerTest.java`**
```java
public class RoleBasedPermissionCheckerTest {
    
    @InjectMocks
    private RoleBasedPermissionChecker permissionChecker;
    
    @Mock
    private RolePermissionDao permissionDao;
    
    @Mock
    private Cache<RoleId, Set<RolePermission>> permissionCache;
    
    private SecurityUser user;
    private RoleId roleId;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        roleId = new RoleId(UUID.randomUUID());
        user = createMockUser(roleId);
    }
    
    @Test
    void testSysAdminHasAllPermissions() {
        SecurityUser sysAdmin = createSysAdminUser();
        
        boolean hasPermission = permissionChecker.hasPermission(
            sysAdmin, Resource.DEVICE, Operation.DELETE
        );
        
        assertTrue(hasPermission);
    }
    
    @Test
    void testUserWithPermission() {
        Set<RolePermission> permissions = Set.of(
            createPermission(Resource.DEVICE, Operation.READ)
        );
        when(permissionCache.get(any(), any())).thenReturn(permissions);
        
        boolean hasPermission = permissionChecker.hasPermission(
            user, Resource.DEVICE, Operation.READ
        );
        
        assertTrue(hasPermission);
    }
    
    @Test
    void testUserWithoutPermission() {
        Set<RolePermission> permissions = Set.of(
            createPermission(Resource.DEVICE, Operation.READ)
        );
        when(permissionCache.get(any(), any())).thenReturn(permissions);
        
        boolean hasPermission = permissionChecker.hasPermission(
            user, Resource.DEVICE, Operation.WRITE
        );
        
        assertFalse(hasPermission);
    }
    
    @Test
    void testPermissionWithAllOperation() {
        Set<RolePermission> permissions = Set.of(
            createPermission(Resource.DEVICE, Operation.ALL)
        );
        when(permissionCache.get(any(), any())).thenReturn(permissions);
        
        assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
        assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.DELETE));
    }
    
    @Test
    void testCacheUsage() {
        Set<RolePermission> permissions = Set.of(createPermission(Resource.DEVICE, Operation.READ));
        when(permissionCache.get(any(), any())).thenReturn(permissions);
        
        permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ);
        permissionChecker.hasPermission(user, Resource.ASSET, Operation.READ);
        
        verify(permissionCache, times(2)).get(eq(roleId), any());
    }
    
    @Test
    void testLegacyAuthorityFallback() {
        SecurityUser legacyUser = createLegacyUser();
        legacyUser.setRoleId(null);
        
        boolean hasPermission = permissionChecker.hasPermission(
            legacyUser, Resource.DEVICE, Operation.READ
        );
        
        assertTrue(hasPermission); // Asume que legacy authority permite
    }
}
```

**Coverage Target:** >= 80%  
**Total Tests:** ~40 tests

#### 4. Tests de Controllers

**`RoleControllerTest.java`**
```java
@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "TENANT_ADMIN")
public class RoleControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RoleService roleService;
    
    @MockBean
    private AccessValidator accessValidator;
    
    @Test
    void testGetRole() throws Exception {
        Role role = createTestRole();
        when(roleService.findRoleById(any(), any())).thenReturn(role);
        
        mockMvc.perform(get("/api/role/{roleId}", role.getId().getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Role"));
    }
    
    @Test
    void testGetRoleNotFound() throws Exception {
        when(roleService.findRoleById(any(), any())).thenReturn(null);
        
        mockMvc.perform(get("/api/role/{roleId}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testSaveRole() throws Exception {
        Role role = createTestRole();
        when(roleService.saveRole(any())).thenReturn(role);
        
        mockMvc.perform(post("/api/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }
    
    @Test
    void testSaveRoleInvalidData() throws Exception {
        Role invalid = new Role();
        invalid.setName(""); // Invalid
        
        mockMvc.perform(post("/api/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(invalid)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser(authorities = "CUSTOMER_USER")
    void testSaveRoleUnauthorized() throws Exception {
        Role role = createTestRole();
        
        mockMvc.perform(post("/api/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(role)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void testGetRoles() throws Exception {
        PageData<Role> page = createTestPageData();
        when(roleService.findRolesByTenantId(any(), any())).thenReturn(page);
        
        mockMvc.perform(get("/api/role")
                .param("pageSize", "10")
                .param("page", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }
    
    @Test
    void testDeleteRole() throws Exception {
        doNothing().when(roleService).deleteRole(any(), any());
        
        mockMvc.perform(delete("/api/role/{roleId}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
    
    @Test
    void testUpdatePermissions() throws Exception {
        Set<RolePermission> permissions = createTestPermissions();
        doNothing().when(roleService).updateRolePermissions(any(), any());
        
        mockMvc.perform(post("/api/role/{roleId}/permissions", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(permissions)))
                .andExpect(status().isOk());
    }
    
    @Test
    void testGetPermissions() throws Exception {
        Set<RolePermission> permissions = createTestPermissions();
        when(roleService.getRolePermissions(any())).thenReturn(permissions);
        
        mockMvc.perform(get("/api/role/{roleId}/permissions", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
```

**Coverage Target:** >= 80%  
**Total Tests:** ~30 tests

### Frontend - Tests Unitarios Angular

#### 1. Tests de Servicios

**`role.service.spec.ts`**
```typescript
describe('RoleService', () => {
  let service: RoleService;
  let httpMock: HttpTestingController;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RoleService]
    });
    
    service = TestBed.inject(RoleService);
    httpMock = TestBed.inject(HttpTestingController);
  });
  
  afterEach(() => {
    httpMock.verify();
  });
  
  it('should be created', () => {
    expect(service).toBeTruthy();
  });
  
  it('should get roles', () => {
    const mockRoles: PageData<Role> = {
      data: [
        { id: '1', name: 'Role 1' } as Role,
        { id: '2', name: 'Role 2' } as Role
      ],
      totalPages: 1,
      totalElements: 2,
      hasNext: false
    };
    
    service.getRoles(new PageLink(10)).subscribe(roles => {
      expect(roles.data.length).toBe(2);
      expect(roles.data[0].name).toBe('Role 1');
    });
    
    const req = httpMock.expectOne(req => req.url.includes('/api/role'));
    expect(req.request.method).toBe('GET');
    req.flush(mockRoles);
  });
  
  it('should get role by id', () => {
    const mockRole: Role = { id: '1', name: 'Test Role' } as Role;
    
    service.getRole('1').subscribe(role => {
      expect(role.id).toBe('1');
      expect(role.name).toBe('Test Role');
    });
    
    const req = httpMock.expectOne('/api/role/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockRole);
  });
  
  it('should save role', () => {
    const roleToSave: Role = { name: 'New Role' } as Role;
    const savedRole: Role = { id: '1', name: 'New Role' } as Role;
    
    service.saveRole(roleToSave).subscribe(role => {
      expect(role.id).toBeDefined();
      expect(role.name).toBe('New Role');
    });
    
    const req = httpMock.expectOne('/api/role');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(roleToSave);
    req.flush(savedRole);
  });
  
  it('should delete role', () => {
    service.deleteRole('1').subscribe(() => {
      expect(true).toBeTruthy();
    });
    
    const req = httpMock.expectOne('/api/role/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
  
  it('should update permissions', () => {
    const permissions: RolePermission[] = [
      { resource: Resource.DEVICE, operation: Operation.READ }
    ];
    
    service.updatePermissions('1', permissions).subscribe(() => {
      expect(true).toBeTruthy();
    });
    
    const req = httpMock.expectOne('/api/role/1/permissions');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(permissions);
    req.flush(null);
  });
  
  it('should handle errors', () => {
    service.getRole('invalid').subscribe(
      () => fail('should have failed'),
      error => {
        expect(error.status).toBe(404);
      }
    );
    
    const req = httpMock.expectOne('/api/role/invalid');
    req.flush('Not found', { status: 404, statusText: 'Not Found' });
  });
});
```

**Coverage Target:** >= 85%  
**Total Tests:** ~20 tests por servicio

#### 2. Tests de Componentes

**`role.component.spec.ts`**
```typescript
describe('RoleComponent', () => {
  let component: RoleComponent;
  let fixture: ComponentFixture<RoleComponent>;
  let roleService: jasmine.SpyObj<RoleService>;
  
  beforeEach(async () => {
    const roleServiceSpy = jasmine.createSpyObj('RoleService', 
      ['getRole', 'saveRole', 'deleteRole']);
    
    await TestBed.configureTestingModule({
      declarations: [RoleComponent],
      imports: [
        ReactiveFormsModule,
        MaterialModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: RoleService, useValue: roleServiceSpy },
        { provide: MAT_DIALOG_DATA, useValue: {} },
        { provide: MatDialogRef, useValue: {} }
      ]
    }).compileComponents();
    
    roleService = TestBed.inject(RoleService) as jasmine.SpyObj<RoleService>;
  });
  
  beforeEach(() => {
    fixture = TestBed.createComponent(RoleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  
  it('should create', () => {
    expect(component).toBeTruthy();
  });
  
  it('should initialize form with empty values', () => {
    expect(component.entityForm).toBeDefined();
    expect(component.entityForm.get('name').value).toBe('');
  });
  
  it('should validate required fields', () => {
    const nameControl = component.entityForm.get('name');
    nameControl.setValue('');
    
    expect(nameControl.invalid).toBeTruthy();
    expect(nameControl.errors.required).toBeTruthy();
  });
  
  it('should populate form with entity data', () => {
    const testRole: Role = {
      id: { id: '1' } as RoleId,
      name: 'Test Role',
      description: 'Test Description'
    } as Role;
    
    component.entity = testRole;
    component.updateForm(testRole);
    
    expect(component.entityForm.get('name').value).toBe('Test Role');
    expect(component.entityForm.get('description').value).toBe('Test Description');
  });
  
  it('should save role when form is valid', () => {
    const savedRole: Role = { id: { id: '1' } } as Role;
    roleService.saveRole.and.returnValue(of(savedRole));
    
    component.entityForm.patchValue({
      name: 'New Role',
      description: 'Description'
    });
    
    component.onSave();
    
    expect(roleService.saveRole).toHaveBeenCalled();
  });
  
  it('should not save when form is invalid', () => {
    component.entityForm.patchValue({ name: '' });
    component.onSave();
    
    expect(roleService.saveRole).not.toHaveBeenCalled();
  });
  
  it('should disable system role editing', () => {
    const systemRole: Role = {
      id: { id: '1' } as RoleId,
      name: 'System Admin',
      isSystem: true
    } as Role;
    
    component.entity = systemRole;
    
    expect(component.isSystemRole()).toBeTruthy();
  });
});
```

**`role-permissions-dialog.component.spec.ts`**
```typescript
describe('RolePermissionsDialogComponent', () => {
  let component: RolePermissionsDialogComponent;
  let fixture: ComponentFixture<RolePermissionsDialogComponent>;
  let roleService: jasmine.SpyObj<RoleService>;
  
  beforeEach(async () => {
    const roleServiceSpy = jasmine.createSpyObj('RoleService', 
      ['getPermissions', 'updatePermissions', 'getAvailableResources', 'getAvailableOperations']);
    
    await TestBed.configureTestingModule({
      declarations: [RolePermissionsDialogComponent],
      imports: [MaterialModule, NoopAnimationsModule],
      providers: [
        { provide: RoleService, useValue: roleServiceSpy },
        { provide: MAT_DIALOG_DATA, useValue: { roleId: '1' } },
        { provide: MatDialogRef, useValue: {} }
      ]
    }).compileComponents();
    
    roleService = TestBed.inject(RoleService) as jasmine.SpyObj<RoleService>;
  });
  
  beforeEach(() => {
    fixture = TestBed.createComponent(RolePermissionsDialogComponent);
    component = fixture.componentInstance;
  });
  
  it('should load resources and operations on init', () => {
    roleService.getAvailableResources.and.returnValue(of([Resource.DEVICE, Resource.ASSET]));
    roleService.getAvailableOperations.and.returnValue(of([Operation.READ, Operation.WRITE]));
    roleService.getPermissions.and.returnValue(of([]));
    
    fixture.detectChanges();
    
    expect(component.resources.length).toBe(2);
    expect(component.operations.length).toBe(2);
  });
  
  it('should build permissions matrix from loaded permissions', () => {
    const permissions: RolePermission[] = [
      { resource: Resource.DEVICE, operation: Operation.READ },
      { resource: Resource.DEVICE, operation: Operation.WRITE }
    ];
    
    roleService.getPermissions.and.returnValue(of(permissions));
    
    component.buildPermissionsMatrix(permissions);
    
    expect(component.hasPermission(Resource.DEVICE, Operation.READ)).toBeTruthy();
    expect(component.hasPermission(Resource.DEVICE, Operation.WRITE)).toBeTruthy();
    expect(component.hasPermission(Resource.ASSET, Operation.READ)).toBeFalsy();
  });
  
  it('should toggle permission in matrix', () => {
    component.togglePermission(Resource.DEVICE, Operation.READ);
    expect(component.hasPermission(Resource.DEVICE, Operation.READ)).toBeTruthy();
    
    component.togglePermission(Resource.DEVICE, Operation.READ);
    expect(component.hasPermission(Resource.DEVICE, Operation.READ)).toBeFalsy();
  });
  
  it('should save permissions', () => {
    roleService.updatePermissions.and.returnValue(of(undefined));
    
    component.togglePermission(Resource.DEVICE, Operation.READ);
    component.save();
    
    expect(roleService.updatePermissions).toHaveBeenCalled();
  });
});
```

**Coverage Target:** >= 75%  
**Total Tests:** ~50 tests

## Tests de Integración

### Backend Integration Tests

#### 1. API Integration Tests

**`RoleControllerIntegrationTest.java`**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Sql(scripts = "/sql/clear-roles.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RoleControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private RoleDao roleDao;
    
    private String authToken;
    private TenantId tenantId;
    
    @BeforeEach
    void setUp() {
        authToken = loginAsTenantAdmin();
        tenantId = getTenantId();
    }
    
    @Test
    void testFullRoleCRUDFlow() {
        // Create
        Role role = new Role();
        role.setName("Integration Test Role");
        role.setDescription("Created in integration test");
        
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Role> request = new HttpEntity<>(role, headers);
        
        ResponseEntity<Role> createResponse = restTemplate.exchange(
            "/api/role",
            HttpMethod.POST,
            request,
            Role.class
        );
        
        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        Role created = createResponse.getBody();
        assertNotNull(created.getId());
        
        // Read
        ResponseEntity<Role> getResponse = restTemplate.exchange(
            "/api/role/" + created.getId().getId(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Role.class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("Integration Test Role", getResponse.getBody().getName());
        
        // Update
        created.setDescription("Updated description");
        request = new HttpEntity<>(created, headers);
        
        ResponseEntity<Role> updateResponse = restTemplate.exchange(
            "/api/role",
            HttpMethod.POST,
            request,
            Role.class
        );
        
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Updated description", updateResponse.getBody().getDescription());
        
        // Delete
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "/api/role/" + created.getId().getId(),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Void.class
        );
        
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        
        // Verify deleted
        ResponseEntity<Role> verifyResponse = restTemplate.exchange(
            "/api/role/" + created.getId().getId(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Role.class
        );
        
        assertEquals(HttpStatus.NOT_FOUND, verifyResponse.getStatusCode());
    }
    
    @Test
    void testPermissionsManagement() {
        Role role = createTestRole();
        
        // Create permissions
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(createPermission(Resource.DEVICE, Operation.READ));
        permissions.add(createPermission(Resource.DEVICE, Operation.WRITE));
        permissions.add(createPermission(Resource.ASSET, Operation.READ));
        
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Set<RolePermission>> request = new HttpEntity<>(permissions, headers);
        
        ResponseEntity<Void> updateResponse = restTemplate.exchange(
            "/api/role/" + role.getId().getId() + "/permissions",
            HttpMethod.POST,
            request,
            Void.class
        );
        
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        
        // Get permissions
        ResponseEntity<RolePermission[]> getResponse = restTemplate.exchange(
            "/api/role/" + role.getId().getId() + "/permissions",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            RolePermission[].class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(3, getResponse.getBody().length);
    }
    
    @Test
    void testUserWithRoleCreation() {
        Role role = createTestRole();
        
        User user = new User();
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAuthority(Authority.TENANT_ADMIN);
        
        HttpHeaders headers = createAuthHeaders();
        
        ResponseEntity<User> response = restTemplate.exchange(
            "/api/user/tenant?roleId=" + role.getId().getId(),
            HttpMethod.POST,
            new HttpEntity<>(user, headers),
            User.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getId());
    }
    
    @Test
    void testUnauthorizedAccess() {
        ResponseEntity<Role> response = restTemplate.exchange(
            "/api/role",
            HttpMethod.GET,
            null,
            Role.class
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    void testCustomerUserCannotCreateRole() {
        String customerToken = loginAsCustomerUser();
        HttpHeaders headers = createHeaders(customerToken);
        
        Role role = new Role();
        role.setName("Should Fail");
        
        ResponseEntity<Role> response = restTemplate.exchange(
            "/api/role",
            HttpMethod.POST,
            new HttpEntity<>(role, headers),
            Role.class
        );
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
```

**Total Integration Tests:** ~30 tests

### Frontend Integration Tests

**Karma Configuration con Coverage**
```javascript
// karma.conf.js
module.exports = function(config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-headless-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage')
    ],
    client: {
      clearContext: false,
      jasmine: {
        random: false
      }
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcovonly' }
      ],
      check: {
        global: {
          statements: 75,
          branches: 70,
          functions: 75,
          lines: 75
        }
      }
    },
    reporters: ['progress', 'kjhtml', 'coverage'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['ChromeHeadless'],
    singleRun: false,
    restartOnFileChange: true
  });
};
```

## Tests End-to-End (E2E)

### Protractor E2E Tests

**`role-management.e2e-spec.ts`**
```typescript
describe('Role Management', () => {
  let page: RoleManagementPage;
  
  beforeEach(() => {
    page = new RoleManagementPage();
    page.navigateTo();
  });
  
  it('should display roles page', () => {
    expect(page.getPageTitle()).toEqual('Roles');
  });
  
  it('should create new role', () => {
    page.clickAddButton();
    page.fillRoleName('E2E Test Role');
    page.fillRoleDescription('Created in E2E test');
    page.clickSaveButton();
    
    expect(page.getRolesCount()).toBeGreaterThan(0);
    expect(page.getRoleByName('E2E Test Role')).toBeTruthy();
  });
  
  it('should configure permissions', () => {
    page.clickRoleByName('E2E Test Role');
    page.clickManagePermissions();
    
    page.togglePermission('DEVICE', 'READ');
    page.togglePermission('DEVICE', 'WRITE');
    page.togglePermission('ASSET', 'READ');
    
    page.clickSavePermissions();
    
    expect(page.getSuccessMessage()).toContain('Permissions updated');
  });
  
  it('should create user with role', () => {
    page.navigateToUsers();
    page.clickAddUserButton();
    
    page.fillUserEmail('e2e@test.com');
    page.fillUserFirstName('E2E');
    page.fillUserLastName('User');
    page.selectRole('E2E Test Role');
    page.checkCreateAsTenantUser();
    
    page.clickSaveUser();
    
    expect(page.getUserByEmail('e2e@test.com')).toBeTruthy();
  });
  
  it('should validate user permissions', () => {
    // Login as the created user
    page.logout();
    page.loginAs('e2e@test.com', 'password');
    
    // Verify can access allowed resources
    expect(page.canAccessDevices()).toBeTruthy();
    expect(page.canAccessAssets()).toBeTruthy();
    
    // Verify cannot access denied resources
    expect(page.canCreateDashboard()).toBeFalsy();
  });
  
  it('should delete role', () => {
    page.navigateToRoles();
    page.deleteRoleByName('E2E Test Role');
    page.confirmDeletion();
    
    expect(page.getRoleByName('E2E Test Role')).toBeFalsy();
  });
});
```

**Criterios de E2E:**
- Todos los flujos principales cubiertos
- Tests en múltiples navegadores (Chrome, Firefox)
- Tests con diferentes resoluciones
- Performance testing durante E2E

## Performance Testing

### Load Testing con JMeter

**`role-permissions-load-test.jmx`**
- 1000 usuarios concurrentes
- 10,000 requests de validación de permisos
- Target: P95 < 50ms, P99 < 100ms

### Criterios de Performance

| Métrica | Target | Crítico |
|---------|--------|---------|
| Role CRUD API | < 200ms P95 | < 500ms |
| Permission Check | < 10ms P95 | < 50ms |
| Permission Update | < 300ms P95 | < 1s |
| Cache Hit Rate | > 95% | > 90% |
| DB Query Time | < 50ms P95 | < 200ms |

## Automation y CI/CD

### Pipeline de Testing

```yaml
# .github/workflows/test-roles.yml
name: Roles System Tests

on: [push, pull_request]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Run Unit Tests
        run: mvn test -Dtest=*Role*Test
      - name: Run Integration Tests
        run: mvn verify -Dtest=*Role*IntegrationTest
      - name: Upload Coverage
        uses: codecov/codecov-action@v2
        
  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up Node
        uses: actions/setup-node@v2
        with:
          node-version: '14'
      - name: Install Dependencies
        run: cd ui-ngx && yarn install
      - name: Run Unit Tests
        run: cd ui-ngx && yarn test:ci
      - name: Upload Coverage
        uses: codecov/codecov-action@v2
        
  e2e-tests:
    runs-on: ubuntu-latest
    needs: [backend-tests, frontend-tests]
    steps:
      - uses: actions/checkout@v2
      - name: Start Application
        run: docker-compose up -d
      - name: Run E2E Tests
        run: cd ui-ngx && yarn e2e
      - name: Upload Screenshots
        if: failure()
        uses: actions/upload-artifact@v2
```

## Checklist de Testing

### Pre-commit
- [ ] Tests unitarios pasan localmente
- [ ] Linter pasa sin errores
- [ ] Cobertura >= umbral mínimo

### Pre-PR
- [ ] Todos los tests (unit + integration) pasan
- [ ] Cobertura no disminuye
- [ ] Tests E2E de flujos modificados pasan
- [ ] Performance tests si aplica

### Pre-Sprint Review
- [ ] Tests de aceptación pasan
- [ ] Tests de regresión pasan
- [ ] Documentación de tests actualizada

### Pre-Release
- [ ] Suite completa de E2E pasa
- [ ] Load testing completado
- [ ] Security testing completado
- [ ] Todos los bugs críticos resueltos

## Herramientas de Testing

**Backend:**
- JUnit 5
- Mockito
- Spring Boot Test
- TestContainers (para tests con DB)
- JMeter (load testing)

**Frontend:**
- Jasmine
- Karma
- Protractor
- Istanbul (coverage)

**General:**
- SonarQube (quality gates)
- Codecov (coverage tracking)
- GitHub Actions (CI/CD)

---

**Versión:** 1.0  
**Última Actualización:** 23 Enero 2026
