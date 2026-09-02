package pl.piomin.services.department;

/** Portable Graftcode DTO for nested employee on department responses. */
public class EmployeeDto {

	public long id;
	public String name;
	public int age;
	public String position;

	public EmployeeDto() {
	}

	public EmployeeDto(long id, String name, int age, String position) {
		this.id = id;
		this.name = name;
		this.age = age;
		this.position = position;
	}

}
