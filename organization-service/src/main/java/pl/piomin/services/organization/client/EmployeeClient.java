package pl.piomin.services.organization.client;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hypertube.sdk.InvocationContext;
import com.hypertube.sdk.RuntimeBridge;

import pl.piomin.services.organization.model.Employee;

/**
 * Organization-side employee client. Internals call the remote employee node
 * Graftcode facade over WebSocket instead of OpenFeign {@code employee-service}.
 * Employee data lives only on that node.
 */
@Component
public class EmployeeClient {

	private static final String FACADE = "pl.piomin.services.employee.EmployeeFacade";

	private final Logger log = LoggerFactory.getLogger(EmployeeClient.class);

	private final String employeeGraftHost;
	private volatile InvocationContext facade;

	public EmployeeClient(
			@Value("${employee.graft.host:ws://localhost:19580/ws}") String employeeGraftHost) {
		this.employeeGraftHost = employeeGraftHost;
		log.info("Employee Graftcode host {}", employeeGraftHost);
	}

	private InvocationContext facade() {
		if (facade == null) {
			synchronized (this) {
				if (facade == null) {
					log.info("Opening Graftcode WS to employee at {}", employeeGraftHost);
					facade = RuntimeBridge.webSocket(
							new com.hypertube.utils.connectiondata.WsConnectionData(employeeGraftHost))
							.jvm().getType(FACADE).execute();
				}
			}
		}
		return facade;
	}

	/** PID of the remote employee process (not this JVM). */
	public long hostPid() {
		Object value = facade().invokeStaticMethod("hostPid").execute().getValue();
		return ((Number) value).longValue();
	}

	public List<Employee> findByOrganization(Long organizationId) {
		InvocationContext result = facade()
				.invokeStaticMethod("findByOrganization", Long.valueOf(organizationId.longValue()))
				.execute();
		int size = ((Number) result.getSize().execute().getValue()).intValue();
		ArrayList<Employee> list = new ArrayList<Employee>();
		for (int i = 0; i < size; i++) {
			list.add(toEmployee(result.getIndex(Integer.valueOf(i)).execute()));
		}
		return list;
	}

	public List<Employee> findByDepartment(Long departmentId) {
		InvocationContext result = facade()
				.invokeStaticMethod("findByDepartment", Long.valueOf(departmentId.longValue()))
				.execute();
		int size = ((Number) result.getSize().execute().getValue()).intValue();
		ArrayList<Employee> list = new ArrayList<Employee>();
		for (int i = 0; i < size; i++) {
			list.add(toEmployee(result.getIndex(Integer.valueOf(i)).execute()));
		}
		return list;
	}

	private Employee toEmployee(InvocationContext employee) {
		Employee e = new Employee();
		e.setId(Long.valueOf(((Number) employee.getInstanceField("id").execute().getValue()).longValue()));
		e.setName((String) employee.getInstanceField("name").execute().getValue());
		e.setAge(((Number) employee.getInstanceField("age").execute().getValue()).intValue());
		e.setPosition((String) employee.getInstanceField("position").execute().getValue());
		return e;
	}

}
