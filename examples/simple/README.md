Hello world project using [gwt-jackson-rest](https://github.com/nmorel/gwt-jackson-rest) to communicate with the server.

The `GenRestBuilder` annotation is directly added to the [GreetingResource](https://github.com/nmorel/gwt-jackson-rest/blob/master/examples/simple/src/main/java/com/github/nmorel/gwtjackson/hello/server/GreetingResource.java) on the server side.

The package is defined through maven :
```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <compilerArgs>
      <arg>-Apackage=com.github.nmorel.gwtjackson.hello.client</arg>
    </compilerArgs>
  </configuration>
</plugin>
```
