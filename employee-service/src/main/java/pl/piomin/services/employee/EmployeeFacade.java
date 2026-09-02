package pl.piomin.services.employee;

import java.util.ArrayList;
import java.util.List;

import pl.piomin.services.employee.model.Employee;
import pl.piomin.services.employee.repository.EmployeeRepository;

/**
 * Public Graftcode contract for employee. Runs in the employee process.
 * Wraps the original {@link EmployeeRepository} with the same seed as
 * {@link EmployeeApplication#repository()} so data lives in THIS JVM —
 * not a second copy inside department/organization callers.
 */
public class EmployeeFacade {

	private static final Object LOCK = new Object();
	private static volatile EmployeeRepository repository;

	public static long hostPid() {
		return ProcessHandle.current().pid();
	}

	private static EmployeeRepository repo() {
		if (repository != null) {
			return repository;
		}
		synchronized (LOCK) {
			if (repository == null) {
				System.out.println("[employee pid=" + hostPid()
						+ "] seeding EmployeeRepository (same as EmployeeApplication @Bean)");
				EmployeeRepository r = new EmployeeRepository();
				r.add(new Employee(1L, 1L, "John Smith", 34, "Analyst"));
				r.add(new Employee(1L, 1L, "Darren Hamilton", 37, "Manager"));
				r.add(new Employee(1L, 1L, "Tom Scott", 26, "Developer"));
				r.add(new Employee(1L, 2L, "Anna London", 39, "Analyst"));
				r.add(new Employee(1L, 2L, "Patrick Dempsey", 27, "Developer"));
				r.add(new Employee(2L, 3L, "Kevin Price", 38, "Developer"));
				r.add(new Employee(2L, 3L, "Ian Scott", 34, "Developer"));
				r.add(new Employee(2L, 3L, "Andrew Campton", 30, "Manager"));
				r.add(new Employee(2L, 4L, "Steve Franklin", 25, "Developer"));
				r.add(new Employee(2L, 4L, "Elisabeth Smith", 30, "Developer"));
				repository = r;
				System.out.println("[employee pid=" + hostPid()
						+ "] EmployeeRepository ready, sample=" + repository.findById(1L).getName());
			}
		}
		return repository;
	}

	public static EmployeeDto findById(long id) {
		System.out.println("[employee pid=" + hostPid() + "] findById(" + id + ")");
		return EmployeeDto.from(repo().findById(Long.valueOf(id)));
	}

	public static EmployeeDto[] findAll() {
		System.out.println("[employee pid=" + hostPid() + "] findAll()");
		return toArray(repo().findAll());
	}

	public static EmployeeDto[] findByDepartment(long departmentId) {
		System.out.println("[employee pid=" + hostPid() + "] findByDepartment(" + departmentId + ")");
		return toArray(repo().findByDepartment(Long.valueOf(departmentId)));
	}

	public static EmployeeDto[] findByOrganization(long organizationId) {
		System.out.println("[employee pid=" + hostPid() + "] findByOrganization(" + organizationId + ")");
		return toArray(repo().findByOrganization(Long.valueOf(organizationId)));
	}

	private static EmployeeDto[] toArray(List<Employee> found) {
		ArrayList<EmployeeDto> list = new ArrayList<EmployeeDto>();
		for (Employee employee : found) {
			list.add(EmployeeDto.from(employee));
		}
		return list.toArray(new EmployeeDto[0]);
	}

}
