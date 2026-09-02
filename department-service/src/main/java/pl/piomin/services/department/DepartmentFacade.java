package pl.piomin.services.department;

import java.util.ArrayList;
import java.util.List;

import pl.piomin.services.department.model.Department;
import pl.piomin.services.department.repository.DepartmentRepository;

/**
 * Public Graftcode contract for department. Runs in the department process.
 * Wraps the original {@link DepartmentRepository} with the same seed as
 * {@link DepartmentApplication#repository()} so data lives in THIS JVM —
 * not a second copy inside organization callers.
 */
public class DepartmentFacade {

	private static final Object LOCK = new Object();
	private static volatile DepartmentRepository repository;

	public static long hostPid() {
		return ProcessHandle.current().pid();
	}

	private static DepartmentRepository repo() {
		if (repository != null) {
			return repository;
		}
		synchronized (LOCK) {
			if (repository == null) {
				System.out.println("[department pid=" + hostPid()
						+ "] seeding DepartmentRepository (same as DepartmentApplication @Bean)");
				DepartmentRepository r = new DepartmentRepository();
				r.add(new Department(1L, "Development"));
				r.add(new Department(1L, "Operations"));
				r.add(new Department(2L, "Development"));
				r.add(new Department(2L, "Operations"));
				repository = r;
				System.out.println("[department pid=" + hostPid()
						+ "] DepartmentRepository ready, sample=" + repository.findById(1L).getName());
			}
		}
		return repository;
	}

	public static DepartmentDto findById(long id) {
		System.out.println("[department pid=" + hostPid() + "] findById(" + id + ")");
		return DepartmentDto.from(repo().findById(Long.valueOf(id)));
	}

	public static DepartmentDto[] findAll() {
		System.out.println("[department pid=" + hostPid() + "] findAll()");
		return toArray(repo().findAll());
	}

	public static DepartmentDto[] findByOrganization(long organizationId) {
		System.out.println("[department pid=" + hostPid() + "] findByOrganization(" + organizationId + ")");
		return toArray(repo().findByOrganization(Long.valueOf(organizationId)));
	}

	private static DepartmentDto[] toArray(List<Department> found) {
		ArrayList<DepartmentDto> list = new ArrayList<DepartmentDto>();
		for (Department department : found) {
			list.add(DepartmentDto.from(department));
		}
		return list.toArray(new DepartmentDto[0]);
	}

}
