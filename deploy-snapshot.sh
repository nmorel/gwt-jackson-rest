#!/usr/bin/env bash

## Put the sonatype login/mdp in settings.xml
#<servers>
#  <server>
#    <id>sonatype-nexus-snapshots</id>
#    <username>XXXXX</username>
#    <password>XXXXX</password>
#  </server>
#</servers>

mvn clean deploy -pl processor -am -DskipTests
