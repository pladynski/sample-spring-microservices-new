#!/usr/bin/env bash
# Host employee (19580) and department (19590) as separate Graftcode nodes.
# Each node seeds the original EmployeeRepository / DepartmentRepository in THAT process.
set -euo pipefail
export PATH="${HOME}/.local/bin:${PATH}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export PATH="${JAVA_HOME}/bin:${PATH}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGDIR="${LOGDIR:-/tmp/gg-logs}"
mkdir -p "$LOGDIR"

kill_port() {
  local port="$1"
  local pids
  pids="$(ss -tlnp 2>/dev/null | awk -v p=":$port" '$4 ~ p {print}' | grep -oE 'pid=[0-9]+' | cut -d= -f2 | sort -u || true)"
  if [ -n "$pids" ]; then
    echo "killing leftover gg on port $port: $pids"
    kill -9 $pids 2>/dev/null || true
  fi
}

pkill -f "gg --runtime java" 2>/dev/null || true
kill_port 19580
kill_port 19590
kill_port 19581
kill_port 19591
sleep 1

assemble_graft_jar() {
  local name="$1"
  local dir="$ROOT/${name}-service"
  local out="$dir/target/${name}-service-graft.jar"
  mvn -f "$ROOT/pom.xml" -pl "${name}-service" -am install -DskipTests -q 1>&2
  rm -rf "$dir/target/graft-lib"
  mvn -f "$ROOT/pom.xml" -pl "${name}-service" -q \
    dependency:copy-dependencies -DoutputDirectory="$dir/target/graft-lib" -DincludeScope=runtime 1>&2
  python3 - "$dir/target/classes" "$dir/target/graft-lib" "$out" <<'PY'
import os, sys, zipfile
from pathlib import Path
classes, lib, out = sys.argv[1], sys.argv[2], sys.argv[3]
seen = set()
os.makedirs(os.path.dirname(out), exist_ok=True)
with zipfile.ZipFile(out, "w") as dest:
    def add_file(arc, path):
        if arc in seen:
            return
        if arc.startswith("META-INF/") and arc.upper().endswith((".SF", ".DSA", ".RSA")):
            return
        seen.add(arc)
        dest.write(path, arc)
    for p in Path(classes).rglob("*"):
        if p.is_file():
            add_file(str(p.relative_to(classes)).replace("\\", "/"), p)
    for jar in sorted(Path(lib).glob("*.jar")):
        with zipfile.ZipFile(jar) as src:
            for info in src.infolist():
                if info.is_dir():
                    continue
                name = info.filename
                if name in seen:
                    continue
                if name.startswith("META-INF/") and name.upper().endswith((".SF", ".DSA", ".RSA")):
                    continue
                seen.add(name)
                dest.writestr(name, src.read(info))
print("assembled", out, file=sys.stderr)
PY
  echo "$out"
}

echo "building employee graft jar..."
EMPLOYEE_JAR="$(assemble_graft_jar employee)"
echo "building department graft jar..."
DEPARTMENT_JAR="$(assemble_graft_jar department)"
echo "employee jar: $EMPLOYEE_JAR"
echo "department jar: $DEPARTMENT_JAR"
test -f "$EMPLOYEE_JAR"
test -f "$DEPARTMENT_JAR"

: > "$LOGDIR/employee.gg.log"
: > "$LOGDIR/department.gg.log"

wait_ws() {
  local log="$1"
  local label="$2"
  local i
  for i in $(seq 1 90); do
    if grep -a -q "Websocket server is available" "$log" 2>/dev/null; then
      echo "$label ready"
      grep -a "Websocket server is available\|Type enabled" "$log" | tail -8
      return 0
    fi
    if grep -a -q "Error initializing" "$log" 2>/dev/null; then
      echo "$label failed to initialize" >&2
      tail -50 "$log" >&2
      return 1
    fi
    sleep 1
  done
  echo "$label did not become ready" >&2
  tail -50 "$log" >&2
  return 1
}

# Start sequentially so Native/Jvm extract does not race across two gg processes.
nohup gg --runtime java \
  --modules "$EMPLOYEE_JAR" \
  --types pl.piomin.services.employee.EmployeeFacade \
  --port 19580 --httpPort 19581 \
  --GV=0 --GMA=0 --GSMU=0 \
  > "$LOGDIR/employee.gg.log" 2>&1 &
echo "employee gg pid=$!"
wait_ws "$LOGDIR/employee.gg.log" employee

nohup gg --runtime java \
  --modules "$DEPARTMENT_JAR" \
  --types pl.piomin.services.department.DepartmentFacade \
  --port 19590 --httpPort 19591 \
  --GV=0 --GMA=0 --GSMU=0 \
  > "$LOGDIR/department.gg.log" 2>&1 &
echo "department gg pid=$!"
wait_ws "$LOGDIR/department.gg.log" department
