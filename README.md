Kinect-Project
==============

A project to allow a Kinect sensor to be used to control a (Java) application.

The main body of this project is Kinect driver agnostic, however the existing code (OpenNIHandler) uses the OpenNI and NITE (v1) framework, and requires the following from OpenNI/Primesense:
com.primesense.NITE.jar
org.OpenNI.jar
OpenNI.jni.dll
OpenNI.dll
XnVNITE.jni.dll
SamplesConfig.xml
along with OpenNI, Primesense SensorKinect driver, NITE being installed.
Note that some resources may be named slightly differently depending on OS, and whether 32 or 64 bit.
When I wrote this originally, binaries could be downloaded from:
http://www.openni.org/openni-sdk/openni-sdk-history-2/
but as this page no longer exists, your best bet would probably be to use the still around (at time of writing) GitHub repositories:
https://github.com/OpenNI/OpenNI
https://github.com/avin2/SensorKinect

If you use the existing DemoSubject, you will also need to have JOGL set up, the code uses:
gluegen-rt-natives-_______.jar
gluegen-rt.jar
jogl-all-natives-______.jar
jogl-all.jar
Where the "______" depends on the user's OS - e.g. windows-amd64.

As to the code itself, OpenNIHandler may or may not have a memory leak [lines 265-270]. Last time I used it the code currently there seemed to stop it, but using the same set-up in a different project still resulted in a leak.

To be controllable, an application must meet the requirements of the KinectSubject interface. Having said that, the code to control the mouse is not limited to the application itself, so if it runs in the background, you have control of the mouse no matter where it is on screen.
