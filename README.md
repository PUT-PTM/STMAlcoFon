# STMAlcoFon
<h2>Overview</h2>
STMAlcoFon is simple breathalyzer project.
<h2>Description</h2>
In our project we use MQ-3 sensor to detect alcohol in exhaled air and measure it. Measurments will be sent via HC-05 bluetooth module to smartphone to present results (graphs, history, etc). Project will be available for smartphones working on Android and Windows Mobile 10 operating systems. Project will also be available on PCs (Windows 10) thanks to Universal Windows Platform. Bluetooth module and alcohol sensor will be atached to STM32F4 device.
<h2>Tools</h2>
Android Studio 1.5</br>
Visual Studio 2015 + Windows 10 SDK</br>
CooCox CoIDE</br>
STM Studio
<h2>How to run</h2>
Download program and .elf file from Release section. Install it. If you install STMAlcoFon on Windows 10 device you must turn on developer mode then install. Before you run, <b>connect peripherals to STM board</b>.</br>
Connect Bluetooth module to STM:</br>
RXD -> PC10 pin</br>
TXD -> PC11 pin</br>
VCC -> 5V pin</br>
GND -> GND pin</br>
Now you can connect MQ-3 sensor:</br>
AOUT (analog output) -> PA3</br>
VCC -> 5V pin</br>
GND -> GND pin</br>
Upload StmAlcoFon.elf to your STM board and thats all. Everything shoul work.</br>
<h2>Future improvements</h2>
Reconnect to device after comunication problem (in both platforms).</br>
Stop conitnous reading on exit (in UWP platform).</br>
Sign WinAlcoFon to avoid turning on developer mode.</br>
<h2>Attributions</h2>
Generate chart in adnroid app: https://github.com/PhilJay/MPAndroidChart </br>
Comunication between android and HC-05: https://developer.android.com/guide/topics/connectivity/bluetooth.html
<h2>License</h2>
MIT - more information on LICENSE file.
<h2>Credits</h2>
Autors:</b>
Leszek Stencel</br>
Tomasz Braczyński </br>
The project was conducted during the Microprocessor Lab course held by the Institute of Control and Information Engineering, Poznan University of Technology.

Supervisor: Michał Fularz
