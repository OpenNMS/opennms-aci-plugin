# OpenNMS ACI Plugin [![CircleCI](https://circleci.com/gh/OpenNMS/opennms-aci-plugin.svg?style=svg)](https://circleci.com/gh/OpenNMS/opennms-aci-plugin)

## Overview

This plugin adds the ability for OpenNMS to monitor devices and networks managed by Cisco APIC controllers to OpenNMS.

We currently support:
 * Importing inventory from the operational topology
 * Receiving Faults via websockets and converting to Events

## Requirements

* OpenNMS Horizon 24.0.0 or greater


## Getting Started

TODO

## Build & install

Build and install the plugin into your local Maven repository using:
```
mvn clean install
```
Before installing plugin:
- add southboud-configuration.xml to <opennms.home>/etc
- Add SSL cert to java keystore:
```
keytool -import -alias apic -keystore /usr/lib/jvm/java-8-oracle/jre/lib/security/cacerts -file mycertfile.der
```

From the OpenNMS Karaf shell:
```
feature:repo-add mvn:org.opennms.plugins.aci/aci-karaf-features/1.0.0-SNAPSHOT/xml
feature:install opennms-plugins-aci
```

Update automatically:
```
bundle:watch *
```
Run your provision import
```
provision:show-import -x aci cluster-name=<cluster-name>

./bin/send-event.pl uei.opennms.org/internal/importer/reloadImport --parm 'url requisition://aci?cluster-name=<cluster-name>'
```

