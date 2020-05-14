# Acoustic Integrity Codes - Android Prototype

*Acoustic Integrity Codes (AICs)* form a modulation scheme that provides message authentication on the acoustic physical layer. It can be used for *Secure Device Pairing (SDP)* between nearby devices. This repository contains the Android prototype implementation from our [WiSec 2020 publication](#read-our-paper), which provides more details such as our adversary model, protocol details, and a security evaluation.

## Disclaimer

This prototype is experimental software and not production ready. Use this code at your own risk.

## Code Structure
Our prototype consists of two components, written in Kotlin:

* **aic_communication**: Library for modulating, transmitting, demodulating, and receiving AIC signals.
* **aic_ui**: Android module for the user interface, which imports the library.

## Configuration

The file `Parameters.kt` in both modules contains parameters such as the buffer size, sample rate, frequency band, slot duration and transmit power.
Since audio hardware varies per device model, speaker and microphone characteristics are not consistent for all smartphones (e.g., the measured noise floor varies per device). For our experiments, we mostly used a OnePlus 3T and a Nexus 5. Some parameters might have to be adjusted for other devices. 

## Dependencies

This prototype requires a minimum Android API level of 21 (Android 5.0 or higher) for easier access to more API features. We use the audio processing pipeline from [TarsosDSP](https://github.com/JorenSix/TarsosDSP). For visualization of the received signal, we use the `PitchView` from [android-audio-library](https://gitlab.com/axet/android-audio-library).

## Read Our Paper

* Florentin Putz, Flor Álvarez, and Jiska Classen. 2020. **Acoustic Integrity
Codes: Secure Device Pairing Using Short-Range Acoustic Communication.**
In *WiSec ’20: 13th ACM Conference on Security and Privacy in Wireless and
Mobile Networks*, July 08–10, 2020, Linz, Austria. ACM.

If you use our project in academic research, please cite this paper:
```
@InProceedings{AcousticIntegrityCodes2020,
  author    = {Putz, Florentin and Álvarez, Flor and Classen, Jiska},
  booktitle = {Proceedings of the 13th Conference on Security and Privacy in Wireless and Mobile Networks},
  date      = {2020},
  title     = {{Acoustic Integrity Codes}: Secure Device Pairing Using Short-Range Acoustic Communication},
  doi       = {10.1145/3395351.3399420},
  publisher = {ACM},
  series    = {WiSec '20},
  url       = {https://doi.org/10.1145/3395351.3399420},
}
```

