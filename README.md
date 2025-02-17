# TwinManager
This repository provides a prototypical Java implementation of the TwinManager framework (evolving from its baseline implementation [DTManager](https://github.com/cdl-mint/DTManagementFramework)) for setting up Digital Twins.

- [TwinManager](#twinmanager)
  - [Features](#features)
  - [Description of packages](#description-of-packages)
    - [config](#config)
    - [endpoints](#endpoints)
    - [management](#management)
    - [model](#model)
    - [services](#services)
  - [Building and deployment](#building-and-deployment)
  - [Examples](#examples)
  - [Citing the tool](#citing-the-tool)
  - [Disclaimer](#disclaimer)

## Features
This prototypical implementation provides unified interfaces to easily set up Digital and Physical Twins for different communication endpoints, including simulations (following the FMI standard) and other protocols, such as MQTT and RabbitMQ.

This architecture also enables the aggregation (and subsequent management) of twins into twin systems which are intrinsically coupled using co-simulation.
The co-simulation engine [Maestro](https://github.com/INTO-CPS-Association/maestro) is used for this at the system level. At the individual level, [JavaFMI](https://bitbucket.org/siani/javafmi/src/master/) is used instead.

Asset Administration Shell models are supported to initialize the structure of twins (`Attributes` and `Operations`), using `.aasx` files. This is supported by integrating some features of [Eclipse Basyx](https://eclipse.dev/basyx/).

The architecture supports synchronous messaging by default, and asynchronous messaging can also be enabled using [RabbitMQ FMU](https://github.com/INTO-CPS-Association/fmu-rabbitmq) (this feature for `AggregateEndpoints`), or `MQTTEndpoints` and `RabbitMQEndpoints` for `IndividualEndpoints`.

## Description of packages
### config
In this package, one can find the classes to initialize the twin endpoints (for individual twins and also aggregate systems).
These classes expect a `.conf` file (see [Configuration library for JVM languages](https://github.com/lightbend/config) for more information).

### endpoints
In this package, one can find the interfaces and some implementations for endpoints (which are the external entities that are to be connected via the TwinManager).
There are interfaces for individual endpoints (`IndividualEndpoint`) and aggregate endpoints (`AggregateEndpoint`). The existing individual implementations are `MQTTEndpoint`, `FMIEndpoint`, and `RabbitMQEndpoint`. For aggregate implementations only the `MaestroEndpoint` is available (this is valid for pure co-simulations or co-simulations with hardware-in-the-loop).

### management
In this package, the `TwinManager` class is found. This is the unique interface the user has to access the twins and their properties and operations.
An instance of the `TwinManager` has methods to get/set attributes of twins and twin systems, and to execute operations on them too. It also has some additional miscellaneous.

### model
In this package, the core components of the architecture are found. Namely, the classes for `Twin`, `TwinSystem`, `TwinSchema`, `Parameter`, and `Clock` are found.
Additionally, the internal package `composition` contains the classes `Attribute`, `Behavior`, `Operation`, and `Relationship`, which belong to a module for enabling composition of Digital Twins (see [Composition of Digital Twins](https://github.com/sagilar/DigitalTwin_Composition) for more information). Additionally, the package `skills` provides an architectural abstraction for the class `Operation` using the skills-based concept. In this module, an operation can be disaggregated into `DevicePrimitive` (most basic operation a device has), `Skill` (a collection of device primitives), and `Task` (a collection of skills).

Currently, the `TwinManager` class only uses of the classes `Attribute` and `Operation` from the composition package. However, this can be changed if other components here are needed to set up a Twin (and its functionalities), e.g., with routines.

### services
This package provides some case-independent services that can be added on top of the `TwinManager`. Currently, two prototypical implementation for a deviation checker (between twins or twin systems) is available (in the `DeviationChecker` class); and for a planner that executes operations on twins or twin systems (in the `Planner` class). The planner requires a `.csv` file with columns indicating the timestep (in milliseconds) and the variables to update with the provided value given a specific timestep.

For the development of new services, these should also be interfaced through the `TwinManager` class to keep the case-agnostic functionality of the overall architecture.


## Building and deployment
Use maven to build the jar file:
```
mvn -f Java/pom.xml package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
```

You can bind the generated jar file to other projects or use it for standalone applications.
An example for running a standolone application is as follows:

```
java -cp your/path/to/TwinManagerFramework-0.0.4.jar YourMainClass.java
```


## Examples
The [examples folder](Java/src/main/java/examples/) provides three examples about how to use the TwinManager. Notice that the examples also require external files, such as models and configurations.
More complete examples can be found in:
- [Flex-cell example with Digital Twin as a Service Platform](https://github.com/INTO-CPS-Association/DTaaS-examples/tree/main/digital_twins/flex-cell).
- [Three-tank system example with Digital Twin as a Service Platform](https://github.com/INTO-CPS-Association/DTaaS-examples/tree/main/digital_twins/three-tank).
- [Flex-cell and Three-tank system integrated with semantic services](https://github.com/Edkamb/ConfLiftingaaS).
- [UR5e in a co-simulation setup using RoboSim technology](https://github.com/INTO-CPS-Association/DigitalTwins_RoboSim).

## Citing the tool

When citing the tool, please cite the following papers:

- D. Lehner, S. Gil, P. H. Mikkelsen, P. G. Larsen and M. Wimmer, "An Architectural Extension for Digital Twin Platforms to Leverage Behavioral ModelsBehaviors," 2023 IEEE 19th International Conference on Automation Science and Engineering (CASE), Auckland, New Zealand, 2023, pp. 1-8, doi: 10.1109/CASE56687.2023.10260417

- Gil, S., Kamburjan, E., Talasila, P. et al. An architecture for coupled digital twins with semantic lifting. Softw Syst Model (2024). https://doi.org/10.1007/s10270-024-01221-w

- Gil, S. Towards realization of Digital Twins for systems with coupled behavior. Thesis (2024). DOI: 10.7146/aul.545. https://doi.org/10.7146/aul.545

Bibtex:

```
@INPROCEEDINGS{10260417,
  author={Lehner, Daniel and Gil, Santiago and Mikkelsen, Peter H. and Larsen, Peter G. and Wimmer, Manuel},
  booktitle={2023 IEEE 19th International Conference on Automation Science and Engineering (CASE)}, 
  title={An Architectural Extension for Digital Twin Platforms to Leverage Behavioral ModelsBehaviors}, 
  year={2023},
  volume={},
  number={},
  pages={1-8},
  keywords={Economics;Adaptation models;Runtime;Computer aided software engineering;Biological system modeling;Digital transformation;Collaboration;Digital Twin;Simulation;Software Frame-work;Digital Twin Platform},
  doi={10.1109/CASE56687.2023.10260417}}

@article{Gil2024,
  author       = {Santiago Gil and Eduard Kamburjan and Prasad Talasila and Peter Gorm Larsen},
  title        = {An Architecture for Coupled Digital Twins with Semantic Lifting},
  year         = {2024},
  journal = {Software and Systems Modeling},
  publisher = {Springer},  
  url = {https://link.springer.com/article/10.1007/s10270-024-01221-w},
  pages = {1--26},
  doi = {10.1007/s10270-024-01221-w},
}
```

## Disclaimer
This is a prototypical implementation and several components have not been tested or fully implemented.