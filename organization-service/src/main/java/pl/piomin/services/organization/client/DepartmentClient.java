package pl.piomin.services.organization.client;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hypertube.sdk.InvocationContext;
import com.hypertube.sdk.RuntimeBridge;

import pl.piomin.services.organization.model.Department;
import pl.piomin.services.organization.model.Employee;

/**
 * Organization-side department client. Internals call the remote department
 * node Graftcode facade over WebSocket instead of OpenFeign
 * {@code department-service}. Department data lives only on that node.
 * {@link #findByOrganizationWithEmployees} also loads employees from the
 * employee node (same composition as {@code DepartmentController}).
 */
@Component
public class DepartmentClient {

	private static final String FACADE = "pl.piomin.services.department.DepartmentFacade";

	private final Logger log = LoggerFactory.getLogger(DepartmentClient.class);

	private final String departmentGraftHost;
	private final EmployeeClient employeeClient;
	private volatile InvocationContext facade;

	public DepartmentClient(
			@Value("${department.graft.host:ws://localhost:19590/ws}") String departmentGraftHost,
			EmployeeClient employeeClient) {
		this.departmentGraftHost = departmentGraftHost;
		this.employeeClient = employeeClient;
		log.info("Department Graftcode host {}", departmentGraftHost);
	}

	private InvocationContext facade() {
		if (facade == null) {
			synchronized (this) {
				if (facade == null) {
					log.info("Opening Graftcode WS to department at {}", departmentGraftHost);
					facade = RuntimeBridge.webSocket(
							new com.hypertube.utils.connectiondata.WsConnectionData(departmentGraftHost))
							.jvm().getType(FACADE).execute();
				}
			}
		}
		return facade;
	}

	/** PID of the remote department process (not this JVM). */
	public long hostPid() {
		Object value = facade().invokeStaticMethod("hostPid").execute().getValue();
		return ((Number) value).longValue();
	}

	public List<Department> findByOrganization(Long organizationId) {
		InvocationContext result = facade()
				.invokeStaticMethod("findByOrganization", Long.valueOf(organizationId.longValue()))
				.execute();
		return toDepartments(result);
	}

	public List<Department> findByOrganizationWithEmployees(Long organizationId) {
		List<Department> departments = findByOrganization(organizationId);
		for (Department department : departments) {
			List<Employee> employees = employeeClient.findByDepartment(department.getId());
			department.setEmployees(employees);
		}
		return departments;
	}

	private List<Department> toDepartments(InvocationContext result) {
		int size = ((Number) result.getSize().execute().getValue()).intValue();
		ArrayList<Department> list = new ArrayList<Department>();
		for (int i = 0; i < size; i++) {
			list.add(toDepartment(result.getIndex(Integer.valueOf(i)).execute()));
		}
		return list;
	}

	private Department toDepartment(InvocationContext department) {
		Department d = new Department();
		d.setId(Long.valueOf(((Number) department.getInstanceField("id").execute().getValue()).longValue()));
		d.setName((String) department.getInstanceField("name").execute().getValue());
		return d;
	}

}
