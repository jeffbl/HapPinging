![HapPing logo](figures/happing_logo.png)

HapPing is a revolutionary sensory substitution application that lets you feel the status and stability of your network connection through vibrations with a Neosensory Buzz wristband.

With a vast number of employers and schools moving to a work from home model causing a corresponding increase in bandwidth hungry videoconferencing solutions, many people are realizing that their home internet connection is not as robust as they imagined. Suddenly dropping out of a call or having lag/latency issues are problems many of us face on a daily basis. In the stress of the moment, it can be difficult to determine exactly where a networking issue is occurring, e.g., device-router, modem-ISP, ISP-server. At the same time, monitoring one’s internet access quality using traditional network monitoring tools requires attention, which takes you away from the conversation or task you are trying to achieve.

To help you monitor your network's status, HapPing first checks whether your smartphone is correctly connecting to your wireless router, your internet service provider and finally, Google. From there, it encodes that information into vibrations using the mode you selected (described bellow) and presents it every few seconds. The different modes were created to offer varying levels of detail and ease of use. The simplest one, Fast and Sweep, presents a sweeping back and forth effect when you can successfully reach the Google server. On the more complex side, Adaptive Bounded Sweep, encodes informatin about your wifi signal strenght, bandwidth and every important node between your smartphone and Google's servers. Aligned with the idea of sensory substitution, you should no longer notice the constant presentation of vibrations after 1-2 weeks, only really picking it up when something unusual happens, e.g., change in the pattern due to a connection loss with your wifi router.

## Getting Started
We propose periodically presenting information on the connectivity and quality of the connection between a users’ device and a remote server.

Current network monitoring solutions require a user or network administrator to orient their attention towards a graphical user interface to understand a networking issue. This is typically a notification of an automatically detected issue, or when an application stops functioning normally. We argue that maintaining an ongoing high-level background awareness of the network’s state would allow a user or network administrator to monitor their infrastructure without explicitly dedicating their visual and cognitive attention. In addition, the proposed approach would allow users to do so while away from their workstation.  

Taking advantage of the four actuators of the Neosensory Buzz, a representation of the transit of a data packet through each network node (e.g., Device, router, ISP, server) will be tangibly represented, providing a sense of where in the network an issue is occurring without even having to open a network monitoring tool.

## Operation Modes
Here is a brief description of currently implemented operation modes. As mentioned in the Getting Started section, we recommend starting with the Bounded Sweep mode. However, we encourage you to experiment with the different options to select the one that suits your needs the best.

### Bounded Sweep - Recommended Starting Point
[INSERT IMAGE OF WRISTBAND AND DIFFERENT ICONS]

#### Everything is good
#### Smartphone/Router Issues
#### Router/Internet Service Provider Issues
#### Internet Service Provider/World Issues

### Adaptive Bounded Sweep - A Step Up in Complexity

#### Wifi Strength
#### Bandwidth

### Fast and Sweep - Keep It Simple, Stupid
[INSERT IMAGE OF WRISTBAND AND DIFFERENT ICONS]

#### Everything Is Good
#### Something's Wrong

### Sweep and Notify - The Comfort of Notifications
[INSERT IMAGE OF WRISTBAND AND DIFFERENT ICONS]

#### Everything is good
#### Smartphone/Router Issues
#### Router/Internet Service Provider Issues
#### Internet Service Provider/World Issues


## How does your solution work? What are the main features? Please specify how you will use the Neosensory Buzz in your solution.
Our solution is composed of a mobile application and the Neosensory Buzz. The application will be responsible for i) sensing the internet access quality through a mix of heuristics, e.g., wifi-signal strength, latency to an external server (ping), bandwidth; ii) translating the calculated quality to vibration patterns delivered by the four actuators; iii) transmitting the patterns to Neosensory Buzz.

The mapping between internet access quality and vibration patterns will be designed to be easily learnt by users. This includes modulating the vibration amplitude and creating an illusory sensation of movement around the user’s wrist, representing data packets moving from the user’s location through the network. For example, in the case of  a local router issue, users will feel the data moving a short distance (only the left-most actuator). When confronting an external server shutdown, users will feel the data moving almost all the way up to its destination (up to the third actuator from the left). Alternatively, if there are no connectivity issues, actuators  would be activated in series from left to right, and then right to left, indicating a full back and forth, providing reassurance that the network is functioning correctly. By allowing the user to specify the remote server, a specific service (e.g., Google Meet, Zoom, etc) can be monitored.


## Updates

SweepDiscrete and SweepBounceDiscrete implemented

Full integration between NetworkMonitoring and Jeff's initial implementation

Network information is updated automatically and available through the main activity attributes.

Haptic designers only need to read those values inside their run() implementation and decide what to with it

Mauricio's mapping, containing a few adjustable parameters

Network Simulator with interface - to test new mappings easily

Forked from the Apache 2.0 licensed Neosensory Android SDK Sample app: https://github.com/neosensory/neosensory-sdk-for-android-java
