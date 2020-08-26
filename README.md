# HelloMap
HelloMap is a SDL VPM app running on Android for testing purpose.

## About HelloMap
This app demonstrates how stable frame rate works, which is proposed at SDL 0292: VirtualDisplayDriver improvement for stable frame rate.

## how to run
1) clone and build, and run.
2) open menu -> Configure Transport
- configure either multiplex or TCP (WiFi) transport.
- if TCP transport is chosen, you must specify HU's IP address and port.
- Press "Apply" button when you have done.
3) open menu -> Configure VDE frame rate
- check useStableFrameRate check box if you want to use stable frame rate.
- When useStableFrameRate is checked, specify frame rate.
- Press "Apply" button when you have done.
4) open menu -> Restart
5) on IVI system, activate "HelloMap".
6) if you don't see VPM started, you should check if HU is capable for VPM.
7) Pay attention to log area, and see how often onOutputBufferAvailable.
