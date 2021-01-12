![HapPing logo](figures/happing_logo.png)

HapPing is a “life-changing” sensory substitution application for feeling the status and stability of your network connection through vibrations with a Neosensory Buzz wristband.

You’ve moved online for your work meetings and social events, but sometimes, your internet connection just drops out or gets slow and fuzzy. Figuring out what went wrong, and how to fix it, is both annoying and stressful. Is it your wifi? Is your ISP having problems? Did your dog bite your ethernet cable? With HapPing, you can feel issues before they even show up in the actual video and audio of your video conference, letting you keep all your attention on the call itself. HapPing provides reassurance that everything is fine, but starts feeling different when things start going wrong with the connection. For example, it can tell you when things are broken so that you don’t continue talking during a presentation even though you aren’t getting through. With ongoing use, we expect that HapPing can even give you hints of where problems are developing, and what part of the connection is causing issues, letting you investigate before you fully drop off.

To help you monitor your network's status, HapPing first checks whether your smartphone is correctly connecting to your wireless router, your internet service provider and finally, Google. From there, it translates that information into vibrations using the mode you select (described below) and presents it every few seconds. The different modes were created to offer varying levels of details and match user preferences:
 The simplest one, Fast and Sweep, presents a sweeping back and forth effect when you can successfully reach the Google server.
On the more complex side, Adaptive Bounded Sweep encodes information about your wifi signal strength, bandwidth and connectivity of every important node between your smartphone and Google's servers. 

Aligned with the idea of sensory substitution, all but one operation mode (Sweep and Notify) were designed such that you will no longer notice the constant presentation of vibrations after 1-2 weeks, only picking up on it when something unusual happens, e.g., a change in the pattern due to a wifi router connection issue.

## Quick Start
Here is a step by step guide that will help you get started feeling your network! To experience HapPing, you will need a Neosensory Buzz wristband, and an Android smartphone.

1. Pair your Neosensory Buzz
   1. Install the Neosensory Android application and follow Neosensory’s instructions to pair the device.

2. Install & Launch the HapPing Application
   1. Download the HapPing APK from the [repository’s release section](https://github.com/jeffbl/HapPinging/releases/). 
   2. Open or execute the APK file on your Android device. You should be prompted for your permission to install the application. You may have to change some settings to allow the installation of an application coming from an untrusted source.  
   3. Launch HapPing by tapping its icon
   4. Click the “SCAN AND CONNECT TO NEOSENSORY BUZZ” button. Successful connection should be acknowledged with a brief popup message, the “SCAN AND CONNECT TO NEOSENSORY BUZZ” button changing to a “DISCONNECT” button, and the appearance of debug information in the Neosensory Command Line Interface at the bottom of the application’s graphical user interface.

3. Select an Operation Mode
Using the dropdown menu, select an operation mode. If it is your first time using HapPing, we recommend that you begin with the Bounded Sweep mode. You can find information about the different operation modes below. TIP: Stop the presentation of vibrations before changing operation modes!

4. Start the Vibrations
Click the “START” button to start feeling vibrations every few seconds corresponding to the operation mode you selected. To help you get a sense of what you are currently experiencing, you can take a look at the Network Status information section and the detailed operation mode information.

5. Quick Training Session

Is your network working too well at the moment? To experience the vibration effects associated with different network failures, use Training Mode. To turn on training mode, activate the Training Mode switch in the middle of the screen. In training mode, your real network status will be overridden by the manual controls, letting you explore how the different network characteristics influence the vibration effects.

   1. Everything is good: Activate both the “Local Router OK” and “ISP OK” switches. This is what you should feel when there are no issues with your connectivity.

   2. Poor WIFI: Put the Wifi Strength slider all the way to the left. Not all mappings use the Wifi Strength. Those that do are designed such that a weak WIFI signal increases the vibration intensity, whereas a strong WIFI signal reduces it.

   3. Poor bandwidth: Ensuring the Wifi Strength is approximately in the middle of its slider, position the bandwidth slider all the way to the left. Not all mappings use the bandwidth information. Those that do are designed such that a low bandwidth lengthen the time it takes for the haptic ping to travel across the actuators. A very high bandwidth, would instead produce a haptic ping that travels very fast between the actuators.

   4. Connection to router lost: Disable the “Local Router OK” switch. Refer to the description of the different operation modes for a detailed description of what you should feel.

   5. Issue with internet service provider: Ensure that the “Local Router OK” switch is enabled, disable the “ISP OK” switch. Refer to the description of the different operation modes for a detailed description of what you should feel.


## Operation Modes
Here is a brief description of currently implemented operation modes. As mentioned in the Getting Started section, we recommend starting with the Bounded Sweep mode. However, we encourage you to experiment with the different options to select the one that best suits your needs. 

All operation modes rely on the concept of a haptic ping. A haptic ping is analogous to a traditional networking ping and travels along the Neosensory Buzz’s actuators, simulating the ping packets traveling across a computer network. This intuitive direct mapping facilitates the troubleshooting process of network issues and turns a networking fault into a highly perceptually distinctive effect to make the problem easy to address.

### Bounded Sweep - Recommended Starting Point
[INSERT IMAGE OF WRISTBAND AND DIFFERENT ICONS]

#### Everything is good
As everything is fine, your ping traveled all the way from your smartphone, to the Google server and back. This is rendered as a vibration that sweeps from the actuator on one end of the wristband to the other end and back.

#### Smartphone/Router Issues
Something is wrong and you are no longer connected to your router. The haptic ping starts from the smartphone, travels to the router, but fails to make it further or return. This is rendered as a vibration that starts on one end, goes one actuator away then stops in its tracks.

#### Router/Internet Service Provider Issues
You are connected to your router, but you are unable to connect to your internet service provider. The haptic ping starts from the smartphone, travels to the router, tries to make it to your ISP but fails to make it further or return. This is rendered as a vibration that starts on one end, sweeps two actuators away then stops in its tracks.

#### Internet Service Provider/World Issues
You are connected to your router, you can connect to your internet service provider but you are unable to reach Google’s servers. The haptic ping starts from the smartphone, travels to the router, makes it through your ISP but fails to make it to Google. This is rendered as a vibration that starts on one end, sweeps three actuators away then stops in its tracks.

### Adaptive Bounded Sweep - A Step Up in Complexity
The Adaptive Bounded Sweep operation mode is an extension of the Bounded Sweep. The haptic ping behaves in the same way as in the Bounded Sweep mode. However, the intensity of the vibration is modulated by the WIFI signal strength (Weaker wifi results in stronger vibrations, while stronger wifi results in weaker vibrations) and your bandwidth modulates the speed at which the haptic ping travels on the wristband (high bandwidth will travel faster than low bandwidth).

### Fast and Sweep - Keep It Simple, Stupid
[INSERT IMAGE OF WRISTBAND AND DIFFERENT ICONS]

#### Everything Is Good
You are connected to your router, it is connecting properly to your ISP and your signal makes it to Google’s servers. This is rendered as a haptic ping that travels from the actuator on one end of the wristband, to the other end before making it back.

#### Something's Wrong
There is an issue somewhere between your smartphone and Google’s server. In Fast and Sweep mode, any issue will result in NO effect being rendered, quickly bringing it up to your attention, since you lose the comfort of feeling your data traveling every few seconds. Unlike in the (Adaptive) Bounded Sweep mode(s), the Fast and Sweep mode does not represent the haptic ping partially making it to its target server.

### Sweep and Notify - The Comfort of Notifications
[INSERT IMAGE OF WRISTBAND AND DIFFERENT ICONS]

#### Everything is good
(simple) You are connected to your router and ISP, and your signal makes it to Google’s servers. The main idea is: “no news is good news;” so it just presents a short and weak pulse periodically when everything is okay.
(dynamic) It will present an unidirectional (#1 to #4) flow of pulse, the speed of pulse indicates the bandwidth; faster connection, faster speed of flow.

#### Smartphone/Router Issues
(Simple)
It will present three pulses on actuator #1 and #2 to indicate there are problem(s) in the connection between your smartphone and (Wifi) router. (The position indicates where the problem is.)
(Dynamic)
The notification signal has features, 4 times of 500 ms, presented on the actuators #1 and #2.

#### Router/Internet Service Provider Issues
(Simple)
Three pulses on actuator #2 and #3 indicate there are problem(s) in the connection between your router and ISP.
(Dynamic)
The notification signal has features, 2 times of 1 s, presented on the actuators #2 and #3.

#### Internet Service Provider/World Issues
(Simple)
Three pulses on actuator #3 and #4indicate there are problem(s) in the connection between your ISP and world. 
(Dynamic)
One long (2 s) pulse on actuators #3 and #4.

## Build HapPing from source
1 Build and Install the Application
1.1 Clone this repository to your system.
1.2 In Android Studio, “run” the application, selecting your device as the target system.

2 Make your own new mode, following the existing examples in MainActivity.java. Note that you can use the NeoVibe.java class to render different effects, including sweeps.


# TO DO
Implementation of network monitoring and vibration rendering as foreground service.
