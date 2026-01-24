# Sprint 7-8: Frontend Services (2 semanas)

## Objetivos del Sprint

Crear la base frontend con modelos TypeScript, servicios Angular e integración con backend.

## User Stories

### US-16: Como frontend developer, necesito modelos TypeScript
**Prioridad:** Alta | **Puntos:** 5

**Criterios de Aceptación:**
- [ ] `role.models.ts` con interfaces completas
- [ ] Enums Resource y Operation
- [ ] Modelos exportados correctamente
- [ ] User model extendido con roleId

**Tareas:**
1. Crear `role.models.ts`
2. Definir interface `Role`
3. Definir interface `RolePermission`
4. Crear enum `Resource`
5. Crear enum `Operation`
6. Modificar `user.model.ts` para agregar `roleId`
7. Crear `role-id.ts` helper
8. Exportar en barrel file
9. Crear tests de modelos

**Estimación:** 1 día

---

### US-17: Como frontend developer, necesito RoleService
**Prioridad:** Alta | **Puntos:** 13

**Criterios de Aceptación:**
- [ ] `RoleService` implementado con todos los métodos
- [ ] Integración con HttpClient
- [ ] Manejo de errores robusto
- [ ] Tests >= 85%

**Tareas:**
1. Crear `role.service.ts`
2. Implementar `getRoles(pageLink)`
3. Implementar `getRole(id)`
4. Implementar `saveRole(role)`
5. Implementar `deleteRole(id)`
6. Implementar `getPermissions(roleId)`
7. Implementar `updatePermissions(roleId, permissions)`
8. Implementar `getAvailableResources()`
9. Implementar `getAvailableOperations()`
10. Agregar manejo de errores
11. Crear `role.service.spec.ts`
12. Tests con HttpTestingController
13. Verificar coverage >= 85%

**Estimación:** 3 días

---

### US-18: Como frontend developer, necesito UserService modificado
**Prioridad:** Alta | **Puntos:** 8

**Criterios de Aceptación:**
- [ ] Métodos para usuarios con roles
- [ ] `createTenantUser(user, roleId)` implementado
- [ ] `changeUserRole(userId, roleId)` implementado
- [ ] Tests actualizados

**Tareas:**
1. Modificar `user.service.ts`
2. Agregar método `createTenantUser()`
3. Agregar método `getTenantUsers()`
4. Agregar método `changeUserRole()`
5. Agregar método `getUsersByRole()`
6. Actualizar tests existentes
7. Crear tests para nuevos métodos

**Estimación:** 2 días

---

### US-19: Como frontend developer, necesito interceptores HTTP
**Prioridad:** Media | **Puntos:** 3

**Criterios de Aceptación:**
- [ ] Interceptor de errores maneja 403 de permisos
- [ ] Mensajes de error claros
- [ ] Redirección a login si necesario

**Tareas:**
1. Modificar `http-interceptor.ts`
2. Agregar manejo de 403 Forbidden
3. Mostrar mensaje de error específico
4. Agregar tests de interceptor

**Estimación:** 1 día

---

## Archivos a Crear/Modificar

```
ui-ngx/src/app/shared/models/
├── role.models.ts (nuevo)
├── user.model.ts (modificar)
└── index.ts (modificar)

ui-ngx/src/app/core/http/
├── role.service.ts (nuevo)
├── user.service.ts (modificar)
└── http-interceptor.ts (modificar)

ui-ngx/src/app/core/http/
└── role.service.spec.ts (nuevo)
```

---

## Código de Ejemplo

### role.models.ts
```typescript
import { BaseData } from './base-data';
import { TenantId } from './id/tenant-id';
import { RoleId } from './id/role-id';

export interface Role extends BaseData<RoleId> {
  tenantId: TenantId;
  name: string;
  description: string;
  isSystem: boolean;
  version: number;
}

export interface RolePermission {
  id?: RolePermissionId;
  roleId: RoleId;
  resource: Resource;
  operation: Operation;
}

export enum Resource {
  DEVICE = 'DEVICE',
  ASSET = 'ASSET',
  DASHBOARD = 'DASHBOARD',
  USER = 'USER',
  CUSTOMER = 'CUSTOMER',
  ALARM = 'ALARM',
  RULE_CHAIN = 'RULE_CHAIN',
  // ... todos los recursos
}

export enum Operation {
  ALL = 'ALL',
  CREATE = 'CREATE',
  READ = 'READ',
  WRITE = 'WRITE',
  DELETE = 'DELETE',
  // ... todas las operaciones
}
```

### role.service.ts
```typescript
@Injectable({ providedIn: 'root' })
export class RoleService {
  
  constructor(private http: HttpClient) {}
  
  getRoles(pageLink: PageLink): Observable<PageData<Role>> {
    return this.http.get<PageData<Role>>('/api/role', {
      params: {
        pageSize: pageLink.pageSize.toString(),
        page: pageLink.page.toString(),
        ...(pageLink.textSearch && { textSearch: pageLink.textSearch })
      }
    });
  }
  
  getRole(roleId: string): Observable<Role> {
    return this.http.get<Role>(`/api/role/${roleId}`);
  }
  
  saveRole(role: Role): Observable<Role> {
    return this.http.post<Role>('/api/role', role);
  }
  
  deleteRole(roleId: string): Observable<void> {
    return this.http.delete<void>(`/api/role/${roleId}`);
  }
  
  getPermissions(roleId: string): Observable<RolePermission[]> {
    return this.http.get<RolePermission[]>(`/api/role/${roleId}/permissions`);
  }
  
  updatePermissions(roleId: string, permissions: RolePermission[]): Observable<void> {
    return this.http.post<void>(`/api/role/${roleId}/permissions`, permissions);
  }
  
  getAvailableResources(): Observable<Resource[]> {
    return this.http.get<Resource[]>('/api/role/resources');
  }
  
  getAvailableOperations(): Observable<Operation[]> {
    return this.http.get<Operation[]>('/api/role/operations');
  }
}
```

---

## Tests

### role.service.spec.ts
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
  
  it('should get roles', () => {
    const mockRoles: PageData<Role> = {
      data: [{ id: { id: '1' }, name: 'Test' } as Role],
      totalPages: 1,
      totalElements: 1,
      hasNext: false
    };
    
    service.getRoles(new PageLink(10)).subscribe(roles => {
      expect(roles.data.length).toBe(1);
    });
    
    const req = httpMock.expectOne(r => r.url.includes('/api/role'));
    expect(req.request.method).toBe('GET');
    req.flush(mockRoles);
  });
  
  it('should save role', () => {
    const role: Role = { name: 'New Role' } as Role;
    const saved: Role = { id: { id: '1' }, name: 'New Role' } as Role;
    
    service.saveRole(role).subscribe(r => {
      expect(r.id).toBeDefined();
    });
    
    const req = httpMock.expectOne('/api/role');
    expect(req.request.method).toBe('POST');
    req.flush(saved);
  });
  
  it('should handle errors', () => {
    service.getRole('invalid').subscribe(
      () => fail('should have failed'),
      error => expect(error.status).toBe(404)
    );
    
    const req = httpMock.expectOne('/api/role/invalid');
    req.flush('Not found', { status: 404, statusText: 'Not Found' });
  });
});
```

**Coverage Target:** >= 85%

---

## Definición de Hecho

- [ ] Modelos TypeScript completos
- [ ] Servicios implementados
- [ ] Tests >= 85%
- [ ] Integración con backend validada
- [ ] Error handling robusto
- [ ] Documentación TSDoc
- [ ] Code review aprobado

---

**Sprint Goal:** Servicios frontend funcionales e integrados con backend.

**Velocity Estimada:** 29 puntos
