# BCS2110 Project — Gemini Lite (Java)

A small **Gemini (“gemini://”) client/server/proxy** implementation written in Java for the BCS2110 lab project.

## What’s in this repo

- `src/main/java/gemini_lite/Client.java` — CLI Gemini client (prints *text/gemini* nicely, streams other content as raw bytes)
- `src/main/java/gemini_lite/Server.java` — simple Gemini server
- `src/main/java/gemini_lite/Proxy.java` — proxy component
- `lab5_*_session.txt`, `lab5_dhcp.txt` — captured sessions/logs used for the lab/report
- `REPORT.md` — project report
- `test-cases.json` — test inputs / scenarios

## Requirements

- **Java 23** (see `pom.xml`)
- **Maven**

## Build
From the project root:
```bash
mvn clean package
```


## Run (from the project root)

This project provides three entry points (each is a `main` class). After building, you can run them from compiled classes:

Client
```bash
java -cp target/classes gemini_lite.Client <GEMINI_URL>
```
Server
```bash
java -cp target/classes gemini_lite.Server
```
Proxy
```bash
java -cp target/classes gemini_lite.Proxy
```

Replace `<ARGS>` / `<GEMINI_URL>` with the values required by your lab setup (ports, hostnames, paths, etc.).

## Features
- Gemini protocol request/response handling
- CLI client with formatted `text/gemini` output
- Raw byte streaming for non-text content
- Standalone Gemini server implementation
- Proxy routing & forwarding
- Test cases for protocol validation

## Notes

- For the write-up and screenshots, see `REPORT.md` and `screenshotsForReport/`.