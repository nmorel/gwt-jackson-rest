gwt-jackson-rest
=====
gwt-jackson-rest is a simple GWT REST client using [gwt-jackson](https://github.com/nmorel/gwt-jackson). It includes an api and an annotation processor which generates an helper class to easily send REST request to an annotated REST service.

Quick start
-------------
Add `<inherits name="com.github.nmorel.gwtjackson.rest.GwtJacksonRest" />` to your module descriptor XML file.

Then annotate your REST service with the annotation `GenRestBuilder`.
An helper class will be generated in the same package by default. You can specify your package by passing the option `package` to the annotation processor.

Check the [example](https://github.com/nmorel/gwt-jackson-rest/tree/master/examples/simple).


With Maven
-------------

```xml
<dependency>
  <groupId>com.github.nmorel.gwtjackson</groupId>
  <artifactId>gwt-jackson-rest-processor</artifactId>
  <version>0.5.0</version>
  <scope>provided</scope>
</dependency>
```

You can also get maven snapshots using the following repository :

```xml
<repository>
  <id>oss-sonatype-snapshots</id>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  <snapshots>
    <enabled>true</enabled>
  </snapshots>
</repository>
```


Copyright and license
-------------

Copyright 2015 Nicolas Morel under the [Apache 2.0 license](LICENSE).
