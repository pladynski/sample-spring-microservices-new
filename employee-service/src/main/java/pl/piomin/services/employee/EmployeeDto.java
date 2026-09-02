package pl.piomin.services.employee;

import pl.piomin.services.employee.model.Employee;

/** Portable Graftcode DTO for employee (primitives + String only). */
public class EmployeeDto {

	public long id;
	public long organizationId;
	public long departmentId;
	public String name;
	public int age;
	public String position;

	public EmployeeDto() {
	}

	public EmployeeDto(long id, long organizationId, long departmentId, String name, int age, String position) {
		this.id = id;
		this.organizationId = organizationId;
		this.departmentId = departmentId;
		this.name = name;
		this.age = age;
		this.position = position;
	}

	public static EmployeeDto from(Employee employee) {
		return new EmployeeDto(
				employee.getId().longValue(),
				employee.getOrganizationId().longValue(),
				employee.getDepartmentId().longValue(),
				employee.getName(),
				employee.getAge(),
				employee.getPosition());
	}

}
