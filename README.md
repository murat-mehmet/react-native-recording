# react-native-recording [![npm version][version-badge]][npm]
React Native audio recording module used for DSP with Android + iOS

<img src="https://user-images.githubusercontent.com/1709072/34551117-9258a0de-f151-11e7-9795-67dda1cbe6f6.png" width=300 />


## Install
```
$ npm i git://github.com/murat-mehmet/react-native-recording.git
$ react-native link react-native-recording
```

## Usage
```javascript
import Recording from 'react-native-recording'

Recording.init({
  bufferSize: 4096,
  sampleRate: 44100,
  bitsPerChannel: 16,
  channelsPerFrame: 1,
})
Recording.addRecordingEventListener(data => console.log(data))
Recording.start()
```

[npm]: https://www.npmjs.com/package/react-native-recording
[version-badge]: https://badge.fury.io/js/react-native-recording.svg
