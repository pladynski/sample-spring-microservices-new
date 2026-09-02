package pl.piomin.services.department;

import pl.piomin.services.department.model.Department;

/** Portable Graftcode DTO for department (primitives + String + nested DTOs). */
public class DepartmentDto {

	public long id;
	public long organizationId;
	public String name;
	public EmployeeDto[] employees;

	public DepartmentDto() {
	}

	public DepartmentDto(long id, long organizationId, String name, EmployeeDto[] employees) {
		this.id = id;
		this.organizationId = organizationId;
		this.name = name;
		this.employees = employees;
	}

	public static DepartmentDto from(Department department) {
		return new DepartmentDto(
				department.getId().longValue(),
				department.getOrganizationId().longValue(),
				department.getName(),
				new EmployeeDto[0]);
	}

}
