import java.util.List;

import pl.piomin.services.organization.client.DepartmentClient;
import pl.piomin.services.organization.client.EmployeeClient;
import pl.piomin.services.organization.model.Department;
import pl.piomin.services.organization.model.Employee;

/**
 * Separate-JVM caller. Talks to employee/department Graftcode nodes over WS.
 * Must not load EmployeeApplication / DepartmentApplication / their repositories.
 */
public class SmokeRemote {

	public static void main(String[] args) {
		long callerPid = ProcessHandle.current().pid();
		String employeeHost = env("EMPLOYEE_GRAFT_HOST", "ws://localhost:19580/ws");
		String departmentHost = env("DEPARTMENT_GRAFT_HOST", "ws://localhost:19590/ws");
		System.out.println("== Graftcode min smoke (remote nodes, original repos) ==");
		System.out.println("caller pid            = " + callerPid);
		System.out.println("employee host         = " + employeeHost);
		System.out.println("department host       = " + departmentHost);

		EmployeeClient employee = new EmployeeClient(employeeHost);
		DepartmentClient department = new DepartmentClient(departmentHost, employee);

		long employeePid = employee.hostPid();
		long departmentPid = department.hostPid();
		System.out.println("employee.hostPid()    = " + employeePid);
		System.out.println("department.hostPid()  = " + departmentPid);

		if (employeePid == callerPid) {
			fail("employee.hostPid() equals caller pid — call stayed in-process");
		}
		if (departmentPid == callerPid) {
			fail("department.hostPid() equals caller pid — call stayed in-process");
		}
		if (employeePid == departmentPid) {
			fail("employee and department share a pid — they must be two nodes");
		}

		List<Employee> employees = employee.findByOrganization(Long.valueOf(1L));
		System.out.println("EmployeeClient.findByOrganization(1) size = " + employees.size());
		if (employees.size() < 1) {
			fail("expected EmployeeApplication seed employees for organization 1");
		}
		Employee first = employees.get(0);
		System.out.println("EmployeeClient.findByOrganization(1)[0] name=" + first.getName()
				+ " position=" + first.getPosition());
		if (!"John Smith".equals(first.getName()) || !"Analyst".equals(first.getPosition())) {
			fail("expected EmployeeApplication seed John Smith / Analyst, got "
					+ first.getName() + " / " + first.getPosition());
		}

		List<Employee> byDept = employee.findByDepartment(Long.valueOf(1L));
		System.out.println("EmployeeClient.findByDepartment(1) size = " + byDept.size());
		if (byDept.size() != 3) {
			fail("expected 3 seed employees in department 1, got " + byDept.size());
		}

		List<Department> departments = department.findByOrganization(Long.valueOf(1L));
		System.out.println("DepartmentClient.findByOrganization(1) size = " + departments.size());
		if (departments.size() != 2) {
			fail("expected 2 seed departments for organization 1, got " + departments.size());
		}
		System.out.println("DepartmentClient.findByOrganization(1)[0] name=" + departments.get(0).getName());
		if (!"Development".equals(departments.get(0).getName())) {
			fail("expected DepartmentApplication seed Development, got " + departments.get(0).getName());
		}

		List<Department> withEmp = department.findByOrganizationWithEmployees(Long.valueOf(1L));
		System.out.println("DepartmentClient.findByOrganizationWithEmployees(1)[0] employees="
				+ withEmp.get(0).getEmployees().size());
		if (withEmp.get(0).getEmployees().size() != 3) {
			fail("expected 3 employees attached to Development, got "
					+ withEmp.get(0).getEmployees().size());
		}

		System.out.println("REMOTE PROOF: caller pid=" + callerPid + " employee pid=" + employeePid
				+ " department pid=" + departmentPid
				+ " — EmployeeClient/DepartmentClient crossed WS to other processes.");
		System.out.println("SMOKE TEST PASSED");
		System.exit(0);
	}

	private static String env(String key, String fallback) {
		String value = System.getenv(key);
		if (value == null || value.trim().isEmpty()) {
			return fallback;
		}
		return value.trim();
	}

	private static void fail(String message) {
		System.out.println("SMOKE TEST FAILED: " + message);
		throw new IllegalStateException(message);
	}

}
