#!/usr/bin/env bash
# From a SEPARATE JVM, call EmployeeClient / DepartmentClient over WS to localhost employee/department nodes.
set -euo pipefail
export PATH="${HOME}/.local/bin:${PATH}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export PATH="${JAVA_HOME}/bin:${PATH}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ORG="$ROOT/organization-service"
LOG="${1:-/workspace/conversions/sample-spring-microservices-new-test.log}"
export EMPLOYEE_GRAFT_HOST="${EMPLOYEE_GRAFT_HOST:-ws://localhost:19580/ws}"
export DEPARTMENT_GRAFT_HOST="${DEPARTMENT_GRAFT_HOST:-ws://localhost:19590/ws}"

if [ ! -d "$ORG/target/classes" ]; then
  mvn -f "$ROOT/pom.xml" -pl organization-service -am package -DskipTests -q
fi

CP_FILE="$(mktemp)"
mvn -f "$ORG/pom.xml" -q -DincludeScope=compile dependency:build-classpath -Dmdep.outputFile="$CP_FILE"
CP="$ORG/target/classes:$(cat "$CP_FILE")"
rm -f "$CP_FILE"

mkdir -p /tmp/smoke-ssmn-classes
javac -cp "$CP" -d /tmp/smoke-ssmn-classes "$ROOT/scripts/SmokeRemote.java"

{
  echo "==== graftcode-min smoke $(date -u -Is) UTC / $(TZ=Europe/Warsaw date -Is) PT ===="
  echo "caller will be a separate JVM; employee gg and department gg must already be up"
  ss -tlnp 2>/dev/null | awk '/19580|19590/' || true
  echo "--- gg processes ---"
  ps -eo pid,cmd | awk '/gg --runtime/ && !/awk/'
  echo "--- invoke ---"
  java -cp "/tmp/smoke-ssmn-classes:$CP" SmokeRemote
  echo "--- employee node log (pid lines) ---"
  grep -a "employee pid=\|seeding EmployeeRepository\|findByOrganization\|findByDepartment" /tmp/gg-logs/employee.gg.log | tail -20 || true
  echo "--- department node log (pid lines) ---"
  grep -a "department pid=\|seeding DepartmentRepository\|findByOrganization" /tmp/gg-logs/department.gg.log | tail -20 || true
} 2>&1 | tee "$LOG"
