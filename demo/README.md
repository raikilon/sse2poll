Demo tests for the sse2poll core module

Build steps

1) Install core to your local Maven repo:
   mvn -q -f core/pom.xml -DskipTests install

2) Run demo tests:
   mvn -q -f demo/pom.xml test

What it covers

- Kickoff timing out returns an accepted-like map with jobId
- Subsequent poll with jobId returns the ready payload
- Fast-path where computation finishes within wait window returns payload directly

