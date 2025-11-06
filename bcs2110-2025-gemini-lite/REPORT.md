# Project Report

Author: Elizabete Beate Putnina
Email: e.putnina@student.maastrichtuniversity.nl
Student ID number: I6407289

## Gemini Lite Client Program

PowerShell - run all tests:

```powershell
cd "C:\Users\User\Documents\Computer Networks\bcs2110-project\bcs2110-2025-gemini-lite"
mvn -f .\pom.xml test
```

Run a single test class or method:

```powershell
mvn -f .\pom.xml -Dtest=ReplyTests test
mvn -f .\pom.xml -Dtest=ReplyTests#parseFormatRoundTrip test
```

Build package:

```powershell
mvn -f .\pom.xml package
```

Run the provided minimal server (in one shell):

```powershell
java -cp .\target\classes TerribleServer
java -cp .\target\classes gemini_lite.Server
```

Run the client (in another shell):

```powershell
java -cp .\target\bcs2110-2025.jar gemini_lite.Client gemini-lite://localhost/
```

Note:
- Client default port in source is 1958;


### Bonus enhancements

(If you attempt any bonus enhancements, document them in this section.)

## Gemini Lite Server Program

(Insert user documentation for your program here. Include command-line usage instructions.)

### Bonus enhancements

(If you attempt any bonus enhancements, document them in this section.)

## Gemini Lite Proxy Program

(Insert user documentation for your program here. Include command-line usage instructions.)

### Bonus enhancements

(If you attempt any bonus enhancements, document them in this section.)

## Test Cases

(Include at least 3 of each kind of test case here, formatted as instructed in the project manual.)

### Client test cases

# Run only Reply tests
```powershell
mvn -f .\pom.xml -Dtest=ReplyTests test
```

# Run only Request tests
```powershell
mvn -f .\pom.xml -Dtest=RequestTests test
```



### Server test cases

### Proxy test cases

## Alternative DNS, Bakeoff and Wireshark outputs

(Include content as specified in labs)

## Reflection on Gemini Lite

(Paragraph or short essay-style answers to the questions)
